package com.project.code_judge.Service;

import com.project.code_judge.Config.RabbitMQConfig;
import com.project.code_judge.Dto.ExecutionResult;
import com.project.code_judge.Entity.Submission;
import com.project.code_judge.Entity.SubmissionStatus;
import com.project.code_judge.Repository.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ResultConsumer {
    private final SubmissionRepository submissionRepository;

    @RabbitListener(queues = RabbitMQConfig.RESULT_QUEUE)
    public void consumeResult(ExecutionResult result){

        UUID submissionId = UUID.fromString(result.getId());
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new RuntimeException("Submission not found."));
        submission.setError(result.getError());
        submission.setVerdict(result.getVerdict());
        submission.setTimeTaken(result.getTime_ms());
        submission.setMemoryUsed(result.getMemory_kb());
        
        // Set status based on whether error occurred
        if (result.getError() != null && !result.getError().isEmpty()) {
            submission.setStatus(SubmissionStatus.FAILED);
        } else {
            submission.setStatus(SubmissionStatus.COMPLETED);
        }
        submissionRepository.save(submission);
    }
}
