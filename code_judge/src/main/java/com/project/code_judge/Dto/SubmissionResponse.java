package com.project.code_judge.Dto;

import com.project.code_judge.Entity.SubmissionStatus;
import com.project.code_judge.Entity.Verdict;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class SubmissionResponse {
    private UUID id;
    private SubmissionStatus status;
    private Verdict verdict;
    private LocalDateTime submissionTime;

    private Long timeTaken;
    private Long memoryUsed;

    private String error;

    private Long problemId;
    private String problemTitle;
}
