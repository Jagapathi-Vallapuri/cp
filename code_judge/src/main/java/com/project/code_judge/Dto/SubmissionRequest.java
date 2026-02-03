package com.project.code_judge.Dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class SubmissionRequest {

    @NotNull(message = "Source code cannot be empty")
    private String code;

    @NotNull(message = "ProblemId is required")
    private Long problemId;
    @NotBlank(message = "Language is required")
    private String language;
    private String username;
}
