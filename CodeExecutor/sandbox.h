#pragma once
#include "defs.h"
#include "LanguageStrategy.hpp"
#include <cstddef>
#include <string>

class Sandbox {
public:
    ExecutionResult run(LanguageStrategy& strategy,
                        const std::string& id,
                        const std::string& input_file,
                        const std::string& output_file,
                        const std::string& expected_file,
                        long time_limit_sec,
                        long memory_limit_mb);

private:
    bool is_correct_answer(const std::string& out_path, const std::string& exp_path);
};

std::string read_file_limited(const std::string& filename, size_t max_bytes = 4096);