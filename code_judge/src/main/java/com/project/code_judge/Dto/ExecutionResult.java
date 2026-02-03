package com.project.code_judge.Dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.project.code_judge.Entity.Verdict;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExecutionResult {
    private String id;
    @Enumerated(EnumType.STRING)
    private Verdict verdict;

    @JsonProperty
    private Long time_ms;

    @JsonProperty
    private Long memory_kb;

    private String error;
}
