package com.project.code_judge.Dto;

import com.project.code_judge.Entity.Difficulty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateProblem {
    @NotBlank(message = "Problem title is required ")
    private String title;
    @NotBlank(message = "Problem description is required")
    private String description;
    @NotBlank(message = "Slug is required")
    private String slug;
    @NotNull(message = "Difficulty is required")
    private Difficulty difficulty;

    @Min(value = 1, message = "Time Limit must be at least 1 second")
    private Double timeLimitSeconds;
    @Min(value = 64, message = "Memory limit must be at least 64Mb")
    private Integer memoryLimitMb;
}
