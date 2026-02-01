#pragma once
#include "defs.h"
#include <string>

class Sandbox
{
public:
    ExecutionResult run(const LanguageConfig &config,
                        const std::string &input_file,
                        const std::string &output_file,
                        const std::string &expected_file,
                        long time_limit_sec,
                        long memory_limit_mb);

private:
    bool is_correct_answer(const std::string &out_path, const std::string &exp_path);
};