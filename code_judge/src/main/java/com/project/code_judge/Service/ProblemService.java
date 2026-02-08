package com.project.code_judge.Service;

import com.project.code_judge.Dto.CreateProblem;
import com.project.code_judge.Entity.Problem;
import com.project.code_judge.Repository.ProblemRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

@Service
public class ProblemService {
    @Autowired
    private ProblemRepository problemRepository;

    @Autowired
    private FileService fileService;

    @Value("${judge.data.path}")
    private String storagePath;

    public Problem createProblem(CreateProblem dto){
        if(dto.getSlug() == null || dto.getSlug().isEmpty()){
            String generatedSlug = dto.getTitle().toLowerCase().replaceAll("[^a-z0-9\\s]", "").replace(" ", "-");
            dto.setSlug(generatedSlug);
        }

        if(problemRepository.existsBySlug(dto.getSlug())){
            throw new RuntimeException("Slug already exists: " + dto.getSlug());
        }

        Problem newProblem = new Problem();
        newProblem.setTitle(dto.getTitle());
        newProblem.setDescription(dto.getDescription());
        newProblem.setSlug(dto.getSlug());
        newProblem.setDifficulty(dto.getDifficulty());
        newProblem.setTestCaseCount(0);
        newProblem.setMemoryLimitMb(dto.getMemoryLimitMb());
        newProblem.setTimeLimitSeconds(dto.getTimeLimitSeconds());
        return problemRepository.save(newProblem);
    }

    @Transactional
    public void uploadTestCases(Long problemId, MultipartFile zipFile) throws IOException {
        Problem problem = problemRepository.findById(problemId)
                .orElseThrow(() -> new RuntimeException("Problem not found"));
        Path targetDir = Paths.get(storagePath, String.valueOf(problemId));
        fileService.unzipAndSave(zipFile, targetDir);
        try(Stream<Path> files = Files.list(targetDir)){
            long count = files.filter(p -> p.getFileName().toString().endsWith("_in.txt")).count();
            problem.setTestCaseCount((int) count);
        }
        problemRepository.save(problem);
    }

    public List<Problem> getAllProblems(){
        return problemRepository.findAll();
    }

    public Problem getProblem(Long id){
        return problemRepository.findById(id).orElseThrow(() -> new RuntimeException("Problem not found"));
    }

}
