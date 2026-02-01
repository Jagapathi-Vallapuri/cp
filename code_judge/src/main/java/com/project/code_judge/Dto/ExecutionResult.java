package com.project.code_judge.Dto;

import lombok.Data;

@Data
public class ExecutionResult {
    private String id;
    private String verdict;
    private Long time_ms;
    private Long memory_kb;
}
