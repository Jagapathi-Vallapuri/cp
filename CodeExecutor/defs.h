#pragma once
#include <string>

enum Verdict
{
    ACCEPTED,
    WRONG_ANSWER,
    TIME_LIMIT_EXCEEDED,
    MEMORY_LIMIT_EXCEEDED,
    RUNTIME_ERROR,
    INTERNAL_ERROR,
    COMPILATION_ERROR
};

struct ExecutionResult
{
    Verdict verdict;
    long time_used_ms;
    long memory_used_kb;
    int exit_code;
};

struct LanguageConfig
{
    std::string name = "unknown";
    std::string src_filename = "";
    std::string compile_cmd = "";
    std::string run_cmd = "";
    std::string run_args = "";
    bool needs_compilation = false;
};