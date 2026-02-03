package com.project.code_judge.Service;

import com.project.code_judge.Config.RabbitMQConfig;
import com.project.code_judge.Entity.Problem;
import com.project.code_judge.Entity.Submission;
import com.project.code_judge.Entity.SubmissionStatus;
import com.project.code_judge.Entity.Verdict;
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
        submission.setSubmissionTime(LocalDateTime.now());
        submission.setUsername(username);
        submission.setStatus(SubmissionStatus.PENDING);
        submission.setLanguage(language);
        submission.setProblem(problem);

        Submission savesSubmission = submissionRepository.save(submission);


        Map<String, Object> message = new HashMap<>();
        message.put("id", savesSubmission.getId().toString());
        message.put("code", savesSubmission.getCode());
        message.put("time_limit", submission.getProblem().getTimeLimitSeconds());
        message.put("memory_limit", submission.getProblem().getMemoryLimitMb());
        message.put("language", submission.getLanguage());
        message.put("problem_id", submission.getProblem().getId());
        message.put("test_case_count", submission.getProblem().getTestCaseCount());



        rabbitTemplate.convertAndSend(RabbitMQConfig.SUBMISSION_QUEUE, message);
        System.out.println("Sent submission " + savesSubmission.getId() + " to Queue");

        return savesSubmission;
    }

}
