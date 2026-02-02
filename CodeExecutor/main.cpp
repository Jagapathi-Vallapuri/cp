#include "sandbox.h"
#include "LanguageStrategy.hpp"
#include <iostream>
#include <fstream>
#include <cstdlib>
#include <SimpleAmqpClient/SimpleAmqpClient.h>
#include <nlohmann/json.hpp>
#include <unistd.h>

using json = nlohmann::json;
using namespace AmqpClient;

int main() {
    Sandbox worker;
    Channel::ptr_t channel;
    std::string consumer_tag;

    const char *rabbit_host = std::getenv("RABBITMQ_HOST");
    if (!rabbit_host) rabbit_host = "localhost";
    
    try {
        Channel::OpenOpts opts;
        opts.host = rabbit_host;
        opts.auth = Channel::OpenOpts::BasicAuth("guest", "guest");
        channel = Channel::Open(opts);
        channel->DeclareQueue("submission_queue", false, true, false, false);
        channel->DeclareQueue("result_queue", false, true, false, false);
        consumer_tag = channel->BasicConsume("submission_queue", "", true, false, false);
        channel->BasicQos(consumer_tag, 1);
        std::cout << "[*] Worker Started." << std::endl;
    } catch (...) {
        std::cerr << "[!] RabbitMQ Connect Failed" << std::endl;
        return 1;
    }

    while (true) {
        auto envelope = channel->BasicConsumeMessage(consumer_tag);
        std::string payload = envelope->Message()->Body();
        std::cout << "[Received] " << payload << std::endl;

        try {
            auto j = json::parse(payload);
            std::string id = j.value("id", "0");
            std::string code = j.value("code", "");
            std::string lang = j.value("language", "cpp");
            std::string input = j.value("input", "");
            std::string expected = j.value("expected_output", "");
            int time_lim = j.value("time_limit", 1);
            int mem_lim = j.value("memory_limit", 256);

            // 1. Get Strategy
            auto strategy = LanguageFactory::create(lang);
            if (!strategy) {
                // Handle unknown language...
                channel->BasicAck(envelope); 
                continue; 
            }

            // 2. Setup Files
            std::string src_file = strategy->get_src_filename(id);
            std::ofstream(src_file) << code;
            std::ofstream("in_" + id + ".txt") << input;
            std::ofstream("exp_" + id + ".txt") << expected;

            // 3. Compile (Delegated to Strategy)
            json res_json;
            res_json["id"] = id;
            res_json["error"] = nullptr;
            bool compile_ok = true;

            if (strategy->needs_compilation()) {
                std::string cmd = strategy->get_compile_cmd(id) + " 2> /dev/null";
                if (system(cmd.c_str()) != 0) {
                    res_json["verdict"] = "COMPILATION_ERROR";
                    res_json["error"] = read_file_limited("compile_err_" + id + ".txt");
                    compile_ok = false;
                }
                remove(("compile_err_" + id + ".txt").c_str());
            }

            // 4. Run (If compile OK)
            if (compile_ok) {
                ExecutionResult res = worker.run(*strategy, id, "in_" + id + ".txt", "out_" + id + ".txt", "exp_" + id + ".txt", time_lim, mem_lim);
                
                const char *v_strs[] = {"ACCEPTED", "WRONG_ANSWER", "TLE", "MLE", "RUNTIME_ERROR", "INTERNAL_ERROR", "COMPILATION_ERROR"};
                res_json["verdict"] = v_strs[res.verdict];
                res_json["time_ms"] = res.time_used_ms;
                res_json["memory_kb"] = res.memory_used_kb;

                if(res.verdict == RUNTIME_ERROR || res.verdict == INTERNAL_ERROR) {
                    res_json["error"] = res.stderr_output;
                }
            }

            // 5. Cleanup
            channel->BasicPublish("", "result_queue", BasicMessage::Create(res_json.dump()));
            
            remove(src_file.c_str());
            remove(("in_" + id + ".txt").c_str());
            remove(("out_" + id + ".txt").c_str());
            remove(("exp_" + id + ".txt").c_str());
            strategy->cleanup(id);

        } catch (const std::exception &e) {
            std::cerr << "[Error] " << e.what() << std::endl;
        }
        channel->BasicAck(envelope);
    }
}