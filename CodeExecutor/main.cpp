#include "sandbox.h"
#include "LanguageStrategy.hpp"
#include "utils.h"
#include <algorithm>
#include <iostream>
#include <fstream>
#include <cstdlib>
#include <cmath>
#include <filesystem>
#include <SimpleAmqpClient/SimpleAmqpClient.h>
#include <nlohmann/json.hpp>
#include <unistd.h>

using json = nlohmann::json;
using namespace AmqpClient;
namespace fs = std::filesystem;

int main()
{
    Sandbox worker;
    Channel::ptr_t channel;
    std::string consumer_tag;

    const char *rabbit_host = std::getenv("RABBITMQ_HOST");
    if (!rabbit_host) rabbit_host = "localhost";

    const char *rabbit_user = std::getenv("RABBITMQ_USER");
    if (!rabbit_user) rabbit_user = "guest";

    const char *rabbit_pass = std::getenv("RABBITMQ_PASS");
    if (!rabbit_pass) rabbit_pass = "guest";

    const char *rabbit_port_env = std::getenv("RABBITMQ_PORT");
    int rabbit_port = rabbit_port_env ? std::atoi(rabbit_port_env) : 5672;

    const char *data_dir_env = std::getenv("JUDGE_DATA_DIR");
    std::string base_data_path = data_dir_env ? data_dir_env : "./judge_data";

    try
    {
        Channel::OpenOpts opts;
        opts.host = rabbit_host;
        opts.port = rabbit_port;
        opts.auth = Channel::OpenOpts::BasicAuth(rabbit_user, rabbit_pass);
        channel = Channel::Open(opts);
        channel->DeclareQueue("submission_queue", false, true, false, false);
        channel->DeclareQueue("result_queue", false, true, false, false);
        consumer_tag = channel->BasicConsume("submission_queue", "", false, false, false);
        channel->BasicQos(consumer_tag, 1);
        std::cout << "[*] Worker Started." << std::endl;
    }
    catch (...)
    {
        std::cerr << "[!] RabbitMQ Connect Failed" << std::endl;
        return 1;
    }

    while (true)
    {
        auto envelope = channel->BasicConsumeMessage(consumer_tag);
        std::string payload = envelope->Message()->Body();
        std::cout << "[Received] " << payload << std::endl;

        bool should_ack = false;
        bool should_requeue = true;

        try
        {
            auto j = json::parse(payload);
            std::string id = j.value("id", "0");
            std::string code = j.value("code", "");
            std::string lang = j.value("language", "cpp");
            std::string problem_id = std::to_string(j.value("problem_id", 0));
            int tc_count = j.value("test_case_count", 0);

            double time_lim = j.value("time_limit", 1.0);
            int mem_lim = j.value("memory_limit", 256);

            auto strategy = LanguageFactory::create(lang);
            json res_json;
            res_json["id"] = id;
            res_json["error"] = nullptr;
            if (!strategy)
            {
                res_json["verdict"] = "INTERNAL_ERROR";
                res_json["error"] = "Unsupported language: " + lang;
            }
            else
            {
                std::string src_file = strategy->get_src_filename(id);
                std::ofstream(src_file) << code;
                ScopedFile src_guard(src_file);

                bool compile_ok = true;

                if (strategy->needs_compilation())
                {
                    std::string err_path = "compile_err_" + id + ".txt";
                    ScopedFile err_guard(err_path);

                    auto compile_args = strategy->get_compile_args(id);
                    int rc = run_command(compile_args, &err_guard.path());
                    if (rc != 0)
                    {
                        res_json["verdict"] = "COMPILATION_ERROR";
                        res_json["error"] = read_file_limited(err_path);
                        compile_ok = false;
                    }
                }

                if (compile_ok)
                {
                    long max_time = 0, max_mem = 0;
                    std::string final_verdict = "ACCEPTED";
                    std::string runtime_error_msg = "";
                    std::string problem_dir = base_data_path + "/" + problem_id + "/";

                    if (tc_count <= 0) {
                        final_verdict = "INTERNAL_ERROR";
                        runtime_error_msg = "No test cases found in request";
                    }

                    for (int i = 1; i <= tc_count; i++)
                    {
                        std::string in_file = problem_dir + std::to_string(i) + "_in.txt";
                        std::string exp_file = problem_dir + std::to_string(i) + "_out.txt";
                        std::string user_out_file = "out_" + id + "_" + std::to_string(i) + ".txt";

                        if (!fs::exists(in_file)) {
                            final_verdict = "INTERNAL_ERROR";
                            runtime_error_msg = "Test case input file missing: " + in_file;
                            break;
                        }
                        if (!fs::exists(exp_file)) {
                            final_verdict = "INTERNAL_ERROR";
                            runtime_error_msg = "Test case output file missing: " + exp_file;
                            break;
                        }

                        ExecutionResult res = worker.run(*strategy, id, in_file, user_out_file, exp_file, static_cast<long>(std::ceil(time_lim)), mem_lim);

                        max_time = std::max(max_time, res.time_used_ms);
                        max_mem = std::max(max_mem, res.memory_used_kb);

                        remove(user_out_file.c_str());

                        if (res.verdict != ACCEPTED)
                        {
                            const char *v_str[] = {
                                "ACCEPTED", 
                                "WRONG_ANSWER", 
                                "TIME_LIMIT_EXCEEDED",
                                "MEMORY_LIMIT_EXCEEDED",
                                "RUNTIME_ERROR", 
                                "INTERNAL_ERROR", 
                                "COMPILATION_ERROR"
                            };
                            final_verdict = v_str[res.verdict];
                            
                            if (res.verdict == RUNTIME_ERROR || res.verdict == INTERNAL_ERROR) {
                                runtime_error_msg = res.stderr_output;
                            }
                            
                            break; 
                        }
                    }

                    res_json["verdict"] = final_verdict;
                    res_json["time_ms"] = max_time;
                    res_json["memory_kb"] = max_mem;

                    if (!runtime_error_msg.empty()) {
                        res_json["error"] = runtime_error_msg;
                    }
                }

                strategy->cleanup(id);
            }

            channel->BasicPublish("", "result_queue", BasicMessage::Create(res_json.dump()));
            std::cout << "[DONE] " << id << ": " << res_json["verdict"] << std::endl;

            should_ack = true;
            should_requeue = false;
        }
        catch (const json::parse_error &e)
        {
            std::cerr << "[Error] Invalid JSON payload: " << e.what() << std::endl;
            should_ack = false;
            should_requeue = false;
        }
        catch (const std::exception &e)
        {
            std::cerr << "[Error] " << e.what() << std::endl;
            should_ack = false;
            should_requeue = true;
        }

        try
        {
            if (should_ack)
                channel->BasicAck(envelope);
            else
                channel->BasicReject(envelope, should_requeue);
        }
        catch (const std::exception &e)
        {
            std::cerr << "[Error] Failed to ACK/NACK message: " << e.what() << std::endl;
        }
    }
}