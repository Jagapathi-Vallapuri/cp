#include "sandbox.h"
#include <unistd.h>
#include <sys/wait.h>
#include <sys/resource.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <cstring>
#include <fstream>
#include <iostream>
#include <sched.h>
#include <vector>
#include <memory>

namespace CgroupUtils {

    const std::string ROOT_CG = "/sys/fs/cgroup";
    const std::string SERVICE_CG = ROOT_CG + "/worker_service";
    const std::string JUDGE_ROOT = ROOT_CG + "/judges";

    void write_file(const std::string &path, const std::string &value) {
        std::ofstream ofs(path);
        if (!ofs.is_open()) return;
        ofs << value;
        ofs.flush();
    }

    bool is_v2() {
        struct stat buffer;
        return (stat((ROOT_CG + "/cgroup.controllers").c_str(), &buffer) == 0);
    }

    void evacuate_root() {
        if (!is_v2()) return;
        
        mkdir(SERVICE_CG.c_str(), 0755);

        std::ifstream root_procs(ROOT_CG + "/cgroup.procs");
        std::vector<int> pids;
        int pid;
        while (root_procs >> pid) pids.push_back(pid);
        
        pids.push_back(0); 

        for (int p : pids) {
            write_file(SERVICE_CG + "/cgroup.procs", std::to_string(p));
        }
    }

    void setup(pid_t pid, int memory_mb) {
        std::string pid_str = std::to_string(pid);
        std::string limit = std::to_string(memory_mb * 1024 * 1024);

        if (is_v2()) {
            std::string job_cg = JUDGE_ROOT + "/job_" + pid_str;
            
            evacuate_root();
            write_file(ROOT_CG + "/cgroup.subtree_control", "+memory +cpu");

            mkdir(JUDGE_ROOT.c_str(), 0755);
            write_file(JUDGE_ROOT + "/cgroup.subtree_control", "+memory +cpu");

            mkdir(job_cg.c_str(), 0755);
            write_file(job_cg + "/memory.max", limit);
            write_file(job_cg + "/memory.swap.max", "0");
            write_file(job_cg + "/cgroup.procs", pid_str);
        } else {
            std::string job_cg = ROOT_CG + "/memory/judge_" + pid_str;
            mkdir(job_cg.c_str(), 0755);
            write_file(job_cg + "/memory.limit_in_bytes", limit);
            write_file(job_cg + "/memory.memsw.limit_in_bytes", limit);
            write_file(job_cg + "/tasks", pid_str);
        }
    }

    void cleanup(pid_t pid) {
        std::string pid_str = std::to_string(pid);
        if (is_v2()) {
            rmdir((JUDGE_ROOT + "/job_" + pid_str).c_str());
        } else {
            rmdir((ROOT_CG + "/memory/judge_" + pid_str).c_str());
        }
    }
}

std::string read_file_limited(const std::string &filename, size_t max_bytes) {
    std::ifstream file(filename);
    if (!file.is_open()) return "";
    std::string content(max_bytes, '\0');
    file.read(&content[0], max_bytes);
    content.resize(file.gcount());
    return content;
}

std::vector<char *> to_c_args(const std::vector<std::string> &args) {
    std::vector<char *> c_args;
    for (const auto &arg : args) c_args.push_back(const_cast<char *>(arg.c_str()));
    c_args.push_back(nullptr);
    return c_args;
}

void redirect_std_streams(const std::string &in, const std::string &out, const std::string &err) {
    int in_fd = open(in.c_str(), O_RDONLY);
    int out_fd = open(out.c_str(), O_WRONLY | O_CREAT | O_TRUNC, 0644);
    int err_fd = open(err.c_str(), O_WRONLY | O_CREAT | O_TRUNC, 0644);

    if (in_fd < 0 || out_fd < 0 || err_fd < 0) _exit(INTERNAL_ERROR);

    dup2(in_fd, STDIN_FILENO);
    dup2(out_fd, STDOUT_FILENO);
    dup2(err_fd, STDERR_FILENO);

    close(in_fd); close(out_fd); close(err_fd);
}


bool Sandbox::is_correct_answer(const std::string &out_path, const std::string &exp_path) {
    std::string cmd = "diff -w -B " + out_path + " " + exp_path + " > /dev/null";
    return (system(cmd.c_str()) == 0);
}

ExecutionResult Sandbox::run(LanguageStrategy &strategy,
                             const std::string &id,
                             const std::string &input_file,
                             const std::string &output_file,
                             const std::string &expected_file,
                             long time_limit_sec,
                             long memory_limit_mb) 
{
    std::string error_file = "err_" + id + ".txt";
    int pipe_fd[2];
    if (pipe(pipe_fd) == -1) return {INTERNAL_ERROR, 0, 0, -1, "Pipe failed"};

    pid_t pid = fork();
    if (pid < 0) return {INTERNAL_ERROR, 0, 0, -1, "Fork failed"};

    if (pid == 0) {
        // --- CHILD PROCESS ---
        close(pipe_fd[1]);
        
        char buffer;
        if (read(pipe_fd[0], &buffer, 1) <= 0) _exit(INTERNAL_ERROR);
        close(pipe_fd[0]);

        unshare(CLONE_NEWNET);
        redirect_std_streams(input_file, output_file, error_file);
        
        rlimit cpu = {(rlim_t)time_limit_sec, (rlim_t)time_limit_sec + 1};
        setrlimit(RLIMIT_CPU, &cpu);
        
        rlimit fsize = {10 * 1024 * 1024, 10 * 1024 * 1024};
        setrlimit(RLIMIT_FSIZE, &fsize);

        auto args = strategy.get_run_args(id, memory_limit_mb);
        auto c_args = to_c_args(args);
        execvp(c_args[0], c_args.data());
        
        perror("Exec failed");
        _exit(INTERNAL_ERROR);
    } 
    else {
        // --- PARENT PROCESS ---
        close(pipe_fd[0]);

        CgroupUtils::setup(pid, memory_limit_mb);

        write(pipe_fd[1], "X", 1);
        close(pipe_fd[1]);
        int status;
        struct rusage usage;
        wait4(pid, &status, 0, &usage);

        CgroupUtils::cleanup(pid);

        ExecutionResult result;
        result.time_used_ms = (usage.ru_utime.tv_sec * 1000) + (usage.ru_utime.tv_usec / 1000);
        result.memory_used_kb = usage.ru_maxrss;
        result.stderr_output = read_file_limited(error_file);

        if (WIFSIGNALED(status)) {
            int sig = WTERMSIG(status);
            if (sig == SIGXCPU) result.verdict = TIME_LIMIT_EXCEEDED;
            else if (sig == SIGKILL) result.verdict = MEMORY_LIMIT_EXCEEDED;
            else result.verdict = RUNTIME_ERROR;
        } else if (WIFEXITED(status)) {
            result.exit_code = WEXITSTATUS(status);
            if (result.exit_code == 0) {
                result.verdict = is_correct_answer(output_file, expected_file) ? ACCEPTED : WRONG_ANSWER;
            } else {
                result.verdict = RUNTIME_ERROR;
            }
        } else {
            result.verdict = INTERNAL_ERROR;
        }

        remove(error_file.c_str());
        return result;
    }
}