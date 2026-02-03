package com.project.code_judge.Dto;

import lombok.Data;
import com.project.code_judge.Entity.Difficulty;

@Data
public class ProblemResponse {
    private Long id;
    private String title;
    private String slug;
    private String description;
    private Difficulty difficulty;
    private Double timeLimitSeconds;
    private Integer memoryLimitMb;
}
