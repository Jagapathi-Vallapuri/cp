#include "sandbox.h"
#include <iostream>
#include <fstream>
#include <cstdlib>
#include <SimpleAmqpClient/SimpleAmqpClient.h>
#include <nlohmann/json.hpp>

using json = nlohmann::json;
using namespace AmqpClient;

LanguageConfig get_language_config(std::string lang, std::string id)
{
    LanguageConfig config;
    config.name = lang;
    std::cout << "[Debug] Config setup for Lang: '" << lang << "' ID: " << id << std::endl;
    if (lang == "cpp")
    {
        config.src_filename = "submit_" + id + ".cpp";
        config.run_cmd = "./bin_" + id;
        config.compile_cmd = "g++ " + config.src_filename + " -o " + config.run_cmd;
        config.needs_compilation = true;
    }
    else if (lang == "python")
    {
        config.src_filename = "submit_" + id + ".py";
        config.run_cmd = "python3";
        config.run_args = config.src_filename;
        config.needs_compilation = false;
    }
    else if (lang == "java")
    {
        config.src_filename = "Main.java";
        config.run_cmd = "java";
        config.compile_cmd = "javac " + config.src_filename;
        config.needs_compilation = true;
    }
    return config;
}

int main()
{
    Sandbox worker;
    Channel::ptr_t channel;
    std::string consumer_tag;
    const char *rabbit_host = std::getenv("RABBITMQ_HOST");
    if (rabbit_host == NULL)
    {
        rabbit_host = "localhost";
    }

    std::cout << "[*] Connecting to RabbitMQ at: " << rabbit_host << " ..." << std::endl;

    try
    {
        Channel::OpenOpts opts;
        opts.host = rabbit_host;
        opts.auth = Channel::OpenOpts::BasicAuth("guest", "guest");
        channel = Channel::Open(opts);

        channel->DeclareQueue("submission_queue", false, true, false, false);
        channel->DeclareQueue("result_queue", false, true, false, false);
        consumer_tag = channel->BasicConsume("submission_queue", "", true, false, false);
        channel->BasicQos(consumer_tag, 1);

        std::cout << "[*] Worker Started. Waiting for jobs..." << std::endl;
    }
    catch (const std::exception &e)
    {
        std::cerr << "[!] Failed to connect to RabbitMQ: " << e.what() << std::endl;
        return 1;
    }

    while (true)
    {
        Envelope::ptr_t envelope = channel->BasicConsumeMessage(consumer_tag);
        std::string payload = envelope->Message()->Body();
        std::cout << "[Received] Processing..." << std::endl;

        try
        {
            auto j = json::parse(payload);
            std::string id = j["id"];
            std::string code = j["code"];
            std::string lang = j.value("language", "cpp");

            LanguageConfig config = get_language_config(lang, id);

            std::ofstream(config.src_filename) << code;

            std::string input_str = j.contains("input") ? j["input"].get<std::string>() : "";
            std::string expected_str = j.contains("expected_output") ? j["expected_output"].get<std::string>() : "";

            std::ofstream("in_" + id + ".txt") << input_str;
            std::ofstream("exp_" + id + ".txt") << expected_str;

            bool compile_success = true;
            if (config.needs_compilation)
            {
                if (system((config.compile_cmd + " 2> /dev/null").c_str()) != 0)
                    compile_success = false;
            }

            json res_json;
            res_json["id"] = id;

            if (!compile_success)
            {
                res_json["verdict"] = "COMPILATION_ERROR";
            }
            else
            {
                int time_limit = j.contains("time_limit") ? j["time_limit"].get<int>() : 1;
                ExecutionResult res = worker.run(config, "in_" + id + ".txt", "out_" + id + ".txt", "exp_" + id + ".txt", time_limit, 64);

                const char *v_strs[] = {"ACCEPTED", "WRONG_ANSWER", "TLE", "MLE", "RUNTIME_ERROR", "INTERNAL_ERROR", "COMPILATION_ERROR"};
                res_json["verdict"] = v_strs[res.verdict];
                res_json["time_ms"] = res.time_used_ms;
                res_json["memory_kb"] = res.memory_used_kb;
            }

            channel->BasicPublish("", "result_queue", BasicMessage::Create(res_json.dump()));
            std::cout << "[Done] " << res_json["verdict"] << std::endl;

            unlink(config.src_filename.c_str());
            unlink(("in_" + id + ".txt").c_str());
            unlink(("out_" + id + ".txt").c_str());
            unlink(("exp_" + id + ".txt").c_str());
            if (config.needs_compilation)
            {
                if (lang == "java")
                    unlink("Main.class");
                else
                    unlink(config.run_cmd.c_str());
            }
        }
        catch (const std::exception &e)
        {
            std::cerr << "[Error] " << e.what() << std::endl;
        }
        channel->BasicAck(envelope);
    }
}