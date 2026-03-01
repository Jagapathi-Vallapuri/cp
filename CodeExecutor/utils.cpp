#include "utils.h"
#include <unistd.h>
#include <sys/wait.h>
#include <fcntl.h>
#include <cstring>
#include <errno.h>

int run_command(const std::vector<std::string>& args, const std::string* stderr_path) {
    if (args.empty()) return -1;

    pid_t pid = fork();
    if (pid < 0) {
        return -1;
    }

    if (pid == 0) {
        if (stderr_path) {
            int fd = open(stderr_path->c_str(), O_WRONLY | O_CREAT | O_TRUNC, 0644);
            if (fd >= 0) {
                dup2(fd, STDERR_FILENO);
                close(fd);
            }
        }

        std::vector<char*> cargs;
        cargs.reserve(args.size() + 1);
        for (const auto& s : args) {
            cargs.push_back(const_cast<char*>(s.c_str()));
        }
        cargs.push_back(nullptr);

        execvp(cargs[0], cargs.data());
        perror("execvp");
        _exit(127);
    } else {
        int status;
        if (waitpid(pid, &status, 0) < 0) {
            return -1;
        }
        if (WIFEXITED(status)) {
            return WEXITSTATUS(status);
        }
        return -1;
    }
}
