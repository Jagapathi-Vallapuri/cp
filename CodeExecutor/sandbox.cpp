#include "sandbox.h"
#include <unistd.h>
#include <sys/wait.h>
#include <sys/resource.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <cstring>
#include <iostream>

ExecutionResult Sandbox::run(const LanguageConfig &config,
                             const std::string &input_file,
                             const std::string &output_file,
                             const std::string &expected_file,
                             long time_limit_sec,
                             long memory_limit_mb)
{
    struct rusage usage;
    int status;

    pid_t pid = fork();
    if (pid < 0)
        return {INTERNAL_ERROR, 0, 0, -1};

    if (pid == 0)
    {

        int in_fd = open(input_file.c_str(), O_RDONLY);
        int out_fd = open(output_file.c_str(), O_WRONLY | O_CREAT | O_TRUNC, 0644);
        dup2(in_fd, STDIN_FILENO);
        dup2(out_fd, STDOUT_FILENO);
        dup2(out_fd, STDERR_FILENO);
        close(in_fd);
        close(out_fd);

        rlimit cpu_lim = {(rlim_t)time_limit_sec, (rlim_t)time_limit_sec + 1};
        setrlimit(RLIMIT_CPU, &cpu_lim);

        rlimit mem_lim;

        if (config.name == "java")
        {
            mem_lim.rlim_cur = RLIM_INFINITY;
            mem_lim.rlim_max = RLIM_INFINITY;
        }
        else
        {
            // For C++ and Python, we apply the strict calculation
            long ram_headroom = (config.name == "cpp") ? 0 : 50; // Base headroom

            if (config.name == "python")
            {
                ram_headroom = 512; // Python runtime overhead
            }

            long total_mem_bytes = (memory_limit_mb + ram_headroom) * 1024 * 1024;
            mem_lim.rlim_cur = (rlim_t)total_mem_bytes;
            mem_lim.rlim_max = (rlim_t)total_mem_bytes;
        }

        setrlimit(RLIMIT_AS, &mem_lim);

        if (config.name == "python")
        {
            execlp("python3", "python3", config.run_args.c_str(), (char *)NULL);
        }
        else if (config.name == "java")
        {
            execlp("java", "java", "-Xmx256m", "-XX:+ExitOnOutOfMemoryError", "-cp", ".", "Main", (char *)NULL);
        }
        else
        {
            execl(config.run_cmd.c_str(), "run_me", (char *)NULL);
        }
        std::cerr << "[Sandbox Child Error] execl failed! Command: " << config.run_cmd << std::endl;
        perror("System Error");
        exit(EXIT_FAILURE);
    }
    else
    {
        if (wait4(pid, &status, 0, &usage) == -1)
            return {INTERNAL_ERROR, 0, 0, -1};

        ExecutionResult result;
        result.time_used_ms = (usage.ru_utime.tv_sec * 1000) + (usage.ru_utime.tv_usec / 1000);
        result.memory_used_kb = usage.ru_maxrss;

        if (WIFEXITED(status))
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
        else if (WIFSIGNALED(status))
        {
            int sig = WTERMSIG(status);
            if (sig == SIGXCPU)
                result.verdict = TIME_LIMIT_EXCEEDED;
            else if (sig == SIGKILL)
                result.verdict = MEMORY_LIMIT_EXCEEDED;
            else
            {
                if (result.memory_used_kb > memory_limit_mb * 1024 * 0.9)
                {
                    result.verdict = MEMORY_LIMIT_EXCEEDED;
                }
                else
                {
                    result.verdict = RUNTIME_ERROR;
                }
            }
        }
        return result;
    }
}

bool Sandbox::is_correct_answer(const std::string &out_path, const std::string &exp_path)
{
    std::string cmd = "diff -w -B " + out_path + " " + exp_path + " > /dev/null";
    return (system(cmd.c_str()) == 0);
}