package com.project.code_judge.Controller;

import com.project.code_judge.Dto.SubmissionRequest;
import com.project.code_judge.Dto.SubmissionResponse;
import com.project.code_judge.Entity.Submission;
import com.project.code_judge.Service.SubmissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/submissions")
public class SubmissionController {
    private final SubmissionService submissionService;

    @PostMapping
    public Submission submit(@RequestBody SubmissionRequest request){
        return submissionService.submitCode(
                request.getProblemId(),
                request.getLanguage(),
                request.getCode()
        );
    }


    @GetMapping("/{id}")
    public ResponseEntity<SubmissionResponse> getSubmissionStatus(@PathVariable UUID id){
        return ResponseEntity.ok(submissionService.getSubmission(id));
    }

}

