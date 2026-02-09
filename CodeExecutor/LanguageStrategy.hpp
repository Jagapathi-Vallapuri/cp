#pragma once
#include <string>
#include <vector>
#include <memory>
#include <sys/resource.h>

// --- Abstract Interface ---
class LanguageStrategy {
public:
    virtual ~LanguageStrategy() = default;

    virtual std::string get_src_filename(const std::string& id) = 0;
    virtual bool needs_compilation() = 0;
    virtual std::string get_compile_cmd(const std::string& id) = 0;
    virtual std::string get_run_cmd(const std::string& id) = 0;
    virtual std::vector<std::string> get_run_args(const std::string& id, int memory_limit_mb) = 0;    
    virtual rlim_t get_rlimit_as(int memory_limit_mb) = 0;    
    virtual void cleanup(const std::string& id) = 0;
};

// --- C++ Strategy ---
class CppStrategy : public LanguageStrategy {
public:
    std::string get_src_filename(const std::string& id) override { return "submit_" + id + ".cpp"; }
    bool needs_compilation() override { return true; }
    
    std::string get_compile_cmd(const std::string& id) override {
        return "g++ -O2 " + get_src_filename(id) + " -o ./bin_" + id + " 2> compile_err_" + id + ".txt";
    }
    
    std::string get_run_cmd(const std::string& id) override { return "./bin_" + id; }
    
    std::vector<std::string> get_run_args(const std::string& id, int memory_limit_mb) override {
        return { get_run_cmd(id) };
    }
    
    rlim_t get_rlimit_as(int memory_limit_mb) override {
        return (rlim_t)memory_limit_mb * 1024 * 1024;
    }

    void cleanup(const std::string& id) override {
        std::string bin = "bin_" + id;
        remove(bin.c_str());
    }
};

// --- Python Strategy ---
class PythonStrategy : public LanguageStrategy {
public:
    std::string get_src_filename(const std::string& id) override { return "submit_" + id + ".py"; }
    bool needs_compilation() override { return false; }
    std::string get_compile_cmd(const std::string& id) override { return ""; }
    std::string get_run_cmd(const std::string& id) override { return "/usr/bin/python3"; }
    
    std::vector<std::string> get_run_args(const std::string& id, int memory_limit_mb) override {
        return { "python3", get_src_filename(id) };
    }
    
    rlim_t get_rlimit_as(int memory_limit_mb) override {
        return (rlim_t)(memory_limit_mb + 50) * 1024 * 1024; 
    }

    void cleanup(const std::string& id) override {}
};

// --- Java Strategy ---
class JavaStrategy : public LanguageStrategy {
public:
    std::string get_src_filename(const std::string& id) override { return "Main.java"; }
    bool needs_compilation() override { return true; }
    
    std::string get_compile_cmd(const std::string& id) override {
        return "javac " + get_src_filename(id) + " 2> compile_err_" + id + ".txt";
    }
    
    std::string get_run_cmd(const std::string& id) override { return "/usr/bin/java"; }
    
    std::vector<std::string> get_run_args(const std::string& id, int memory_limit_mb) override {
        return {
            "java",
            "-Xmx" + std::to_string(memory_limit_mb) + "m", // Max Heap
            "-Xms16m",
            "-XX:+UseSerialGC",
            "-Xss64m",
            "-XX:+ExitOnOutOfMemoryError",
            "-cp", ".", 
            "Main"
        };
    }
    
    rlim_t get_rlimit_as(int memory_limit_mb) override {
        return RLIM_INFINITY;
    }

    void cleanup(const std::string& id) override {
        remove("Main.class");
    }
};

// --- Factory ---
class LanguageFactory {
public:
    static std::unique_ptr<LanguageStrategy> create(const std::string& lang) {
        if (lang == "cpp") return std::make_unique<CppStrategy>();
        if (lang == "python") return std::make_unique<PythonStrategy>();
        if (lang == "java") return std::make_unique<JavaStrategy>();
        return nullptr;
    }
};