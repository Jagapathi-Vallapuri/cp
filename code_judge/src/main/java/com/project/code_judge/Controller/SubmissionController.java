package com.project.code_judge.Controller;

import com.project.code_judge.Dto.SubmissionRequest;
import com.project.code_judge.Entity.Submission;
import com.project.code_judge.Repository.SubmissionRepository;
import com.project.code_judge.Service.SubmissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/submit")
@CrossOrigin(origins = "http://localhost:5173")
public class SubmissionController {
    private final SubmissionService submissionService;
    private final SubmissionRepository submissionRepository;

    @PostMapping
    public Submission submit(@RequestBody SubmissionRequest request){
        return submissionService.submitCode(
                request.getCode(),
                request.getProblemId(),
                request.getUsername(),
                request.getLanguage()
        );
    }


    @GetMapping("/{id}")
    public Submission getSubmissionStatus(@PathVariable UUID id){
        return submissionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Submission not found."));
    }


}

