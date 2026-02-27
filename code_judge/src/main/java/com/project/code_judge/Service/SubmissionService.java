package com.project.code_judge.Service;

import com.project.code_judge.Config.RabbitMQConfig;
import com.project.code_judge.Dto.*;
import com.project.code_judge.Entity.Problem;
import com.project.code_judge.Entity.Submission;
import com.project.code_judge.Entity.SubmissionStatus;
import com.project.code_judge.Entity.User;
import com.project.code_judge.Repository.SubmissionRepository;
import com.project.code_judge.Repository.UserRepository;
import com.project.code_judge.Repository.ProblemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SubmissionService {
    private final SubmissionRepository submissionRepository;
    private final UserRepository userRepository;
    private final ProblemRepository problemRepository;
    private final RabbitTemplate rabbitTemplate;

    public Submission submitCode(Long problemId, String language, String code){
        Problem problem = problemRepository.findById(problemId)
                .orElseThrow(() -> new RuntimeException("Problem not found"));

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if(authentication == null || !authentication.isAuthenticated())
            throw new RuntimeException("User not authenticated");

        String username =  authentication.getName();

        User user = userRepository.findByEmail(username).orElseThrow(() -> new RuntimeException("User not found"));
        Submission submission = new Submission();
        submission.setCode(code);
        submission.setUser(user);
        submission.setSubmissionTime(LocalDateTime.now());
        submission.setStatus(SubmissionStatus.PENDING);
        submission.setLanguage(language);
        submission.setProblem(problem);

        Submission savedSubmission = submissionRepository.save(submission);


        Map<String, Object> message = new HashMap<>();
        message.put("id", savedSubmission.getId().toString());
        message.put("code", savedSubmission.getCode());
        message.put("time_limit", submission.getProblem().getTimeLimitSeconds());
        message.put("memory_limit", submission.getProblem().getMemoryLimitMb());
        message.put("language", submission.getLanguage());
        message.put("problem_id", submission.getProblem().getId());
        message.put("test_case_count", submission.getProblem().getTestCaseCount());



        rabbitTemplate.convertAndSend(RabbitMQConfig.SUBMISSION_QUEUE, message);
        System.out.println("Sent submission " + savedSubmission.getId() + " to Queue");

        return savedSubmission;
    }

    public SubmissionResponse getSubmission(UUID id){
        Submission submission = submissionRepository.findById(id).orElseThrow(() -> new RuntimeException("Submission not found"));
        return mapToResponse(submission);
    }

    private SubmissionResponse mapToResponse(Submission submission) {
        SubmissionResponse response = new SubmissionResponse();
        response.setId(submission.getId());
        response.setStatus(submission.getStatus());
        response.setVerdict(submission.getVerdict());
        response.setSubmissionTime(submission.getSubmissionTime());
        response.setTimeTaken(submission.getTimeTaken());
        response.setMemoryUsed(submission.getMemoryUsed());
        response.setError(submission.getError());

        if (submission.getProblem() != null) {
            response.setProblemId(submission.getProblem().getId());
            response.setProblemTitle(submission.getProblem().getTitle());
        }

        return response;
    }

}
