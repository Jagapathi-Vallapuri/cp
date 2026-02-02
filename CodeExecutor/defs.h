#pragma once
#include <string>

enum Verdict {
    ACCEPTED,
    WRONG_ANSWER,
    TIME_LIMIT_EXCEEDED,
    MEMORY_LIMIT_EXCEEDED,
    RUNTIME_ERROR,
    INTERNAL_ERROR,
    COMPILATION_ERROR
};

struct ExecutionResult {
    Verdict verdict;
    long time_used_ms;
    long memory_used_kb;
    int exit_code;
    std::string stderr_output;
};