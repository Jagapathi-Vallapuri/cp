#pragma once

#include <string>
#include <vector>

// RAII helper that removes the file when it goes out of scope.
class ScopedFile {
public:
    explicit ScopedFile(std::string path) : path_(std::move(path)) {}
    ~ScopedFile() {
        if (!path_.empty()) {
            std::remove(path_.c_str());
        }
    }

    // disable copy
    ScopedFile(const ScopedFile&) = delete;
    ScopedFile& operator=(const ScopedFile&) = delete;

    // allow move
    ScopedFile(ScopedFile&& other) noexcept : path_(std::move(other.path_)) {
        other.path_.clear();
    }
    ScopedFile& operator=(ScopedFile&& other) noexcept {
        if (this != &other) {
            if (!path_.empty()) std::remove(path_.c_str());
            path_ = std::move(other.path_);
            other.path_.clear();
        }
        return *this;
    }

    const std::string& path() const { return path_; }
    void dismiss() { path_.clear(); }

private:
    std::string path_;
};

// Execute the given command (arguments include program name).
// If stderr_path is non-null, the child's stderr is redirected to that
// file. Returns exit status (or -1 on fork/exec failure).
int run_command(const std::vector<std::string>& args, const std::string* stderr_path = nullptr);
