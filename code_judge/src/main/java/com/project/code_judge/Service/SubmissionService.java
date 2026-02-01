package com.project.code_judge.Service;

import com.project.code_judge.Config.RabbitMQConfig;
import com.project.code_judge.Entity.Problem;
import com.project.code_judge.Entity.Submission;
import com.project.code_judge.Repository.ProblemRepository;
import com.project.code_judge.Repository.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SubmissionService {
    private final SubmissionRepository submissionRepository;
    private final ProblemRepository problemRepository;
    private final RabbitTemplate rabbitTemplate;

    public Submission submitCode(String code, Long problemId, String username, String language){
        Problem problem = problemRepository.findById(problemId)
                .orElseThrow(() -> new RuntimeException("Problem not found"));

        Submission submission = new Submission();
        submission.setCode(code);
        submission.setProblemId(problemId);
        submission.setSubmissionTime(LocalDateTime.now());
        submission.setUsername(username);
        submission.setStatus("PENDING");
        submission.setVerdict("WAITING");
        submission.setLanguage(language);

        Submission savesSubmission = submissionRepository.save(submission);

        Map<String, Object> message = new HashMap<>();
        message.put("id", savesSubmission.getId());
        message.put("code", savesSubmission.getCode());
        message.put("input", problem.getTestInput());
        message.put("expected_output", problem.getExpectedOutput());
        message.put("time_limit", problem.getTimeLimitSeconds());
        message.put("language", language);

        rabbitTemplate.convertAndSend(RabbitMQConfig.SUBMISSION_QUEUE, message);
        System.out.println("Sent submission " + savesSubmission.getId() + " to Queue");

        return savesSubmission;
    }

}
