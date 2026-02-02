#include "sandbox.h"
#include <unistd.h>
#include <sys/wait.h>
#include <sys/resource.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <cstring>
#include <fstream>
#include <iostream>
#include <vector>

std::vector<char *> to_c_args(const std::vector<std::string> &args)
{
    std::vector<char *> c_args;
    for (const auto &arg : args)
    {
        c_args.push_back(const_cast<char *>(arg.c_str()));
    }
    c_args.push_back(nullptr);
    return c_args;
}

ExecutionResult Sandbox::run(LanguageStrategy &strategy,
                             const std::string &id,
                             const std::string &input_file,
                             const std::string &output_file,
                             const std::string &expected_file,
                             long time_limit_sec,
                             long memory_limit_mb)
{
    struct rusage usage;
    int status;
    std::string error_file = "err_" + id + ".txt";

    // 1. Get exact command args from strategy (No logic here!)
    std::vector<std::string> cmd_args = strategy.get_run_args(id, memory_limit_mb);
    std::vector<char *> c_argv = to_c_args(cmd_args);

    pid_t pid = fork();
    if (pid < 0)
        return {INTERNAL_ERROR, 0, 0, -1};

    if (pid == 0)
    { // Child
        // Redirect I/O
        int in_fd = open(input_file.c_str(), O_RDONLY);
        int out_fd = open(output_file.c_str(), O_WRONLY | O_CREAT | O_TRUNC, 0644);
        int err_fd = open(error_file.c_str(), O_WRONLY | O_CREAT | O_TRUNC, 0644);
        if (in_fd < 0 || out_fd < 0)
            _exit(INTERNAL_ERROR);

        dup2(in_fd, STDIN_FILENO);
        dup2(out_fd, STDOUT_FILENO);
        dup2(err_fd, STDERR_FILENO);
        close(in_fd);
        close(out_fd);

        // Limits
        rlimit cpu_lim = {(rlim_t)time_limit_sec, (rlim_t)time_limit_sec + 1};
        setrlimit(RLIMIT_CPU, &cpu_lim);

        // Ask Strategy for memory limit logic
        rlimit mem_lim;
        mem_lim.rlim_cur = strategy.get_rlimit_as(memory_limit_mb);
        mem_lim.rlim_max = mem_lim.rlim_cur;
        setrlimit(RLIMIT_AS, &mem_lim);

        // EXECUTE - Generic!
        execvp(c_argv[0], c_argv.data());

        // If we get here, it failed
        perror("execvp failed");
        _exit(INTERNAL_ERROR);
    }
    else
    { // Parent
        // Wait and Monitor
        if (wait4(pid, &status, 0, &usage) == -1)
            return {INTERNAL_ERROR, 0, 0, -1};

        ExecutionResult result;
        result.time_used_ms = (usage.ru_utime.tv_sec * 1000) + (usage.ru_utime.tv_usec / 1000);
        result.memory_used_kb = usage.ru_maxrss;
        result.stderr_output = read_file_limited(error_file);

        if (WIFSIGNALED(status))
        {
            int sig = WTERMSIG(status);
            if (sig == SIGXCPU)
                result.verdict = TIME_LIMIT_EXCEEDED;
            else if (sig == SIGKILL)
                result.verdict = MEMORY_LIMIT_EXCEEDED;
            else if (sig == SIGSEGV)
            {
                result.verdict = RUNTIME_ERROR;
                result.stderr_output += "\n[System] Segmentation Fault (Invalid Memory Access)";
            }
            else if (sig == SIGFPE)
            {
                result.verdict = RUNTIME_ERROR;
                result.stderr_output += "\n[System] Floating Point Exception (Div/0?)";
            }
            else
            {
                result.verdict = RUNTIME_ERROR;
                result.stderr_output += "\n[System] Killed by signal " + std::to_string(sig);
            }
        }
        else if (WIFEXITED(status))
        {
            result.exit_code = WEXITSTATUS(status);
            if (result.exit_code == 0)
            {
                result.verdict = is_correct_answer(output_file, expected_file) ? ACCEPTED : WRONG_ANSWER;
            }
            else
            {
                result.verdict = RUNTIME_ERROR;
            }
        }

        remove(error_file.c_str());

        return result;
    }
}

bool Sandbox::is_correct_answer(const std::string &out_path, const std::string &exp_path)
{
    std::string cmd = "diff -w -B " + out_path + " " + exp_path + " > /dev/null";
    return (system(cmd.c_str()) == 0);
}

std::string read_file_limited(const std::string &filename, size_t max_bytes)
{
    std::ifstream file(filename);
    if (!file.is_open())
        return "";

    std::string content;
    content.resize(max_bytes);
    file.read(&content[0], max_bytes);
    size_t bytes_read = file.gcount();
    content.resize(bytes_read);
    return content;
}