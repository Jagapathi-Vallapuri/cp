package com.project.code_judge.Dto;

import lombok.Data;

@Data
public class SubmissionRequest {
    private String code;
    private Long problemId;
    private String username;
    private String language;
}
