package com.project.code_judge.Controller;

import com.project.code_judge.Entity.Problem;
import com.project.code_judge.Service.ProblemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.project.code_judge.Dto.CreateProblem;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/")
public class ProblemController {

    @Autowired
    ProblemService problemService;

    @PostMapping("admin/problems")
    public ResponseEntity<Problem> createProblem(@RequestBody CreateProblem dto){
        Problem created = problemService.createProblem(dto);
        return ResponseEntity.ok(created);
    }

    @PostMapping("admin/problems/{id}/testcases")
    public ResponseEntity<String> uploadTestCases(@PathVariable Long id, @RequestParam("file") MultipartFile file){
        try{
            problemService.uploadTestCases(id, file);
            return ResponseEntity.ok("Test cases uploaded successfully");
        }catch (Exception e){
            return ResponseEntity.badRequest().body("Upload Failed: " + e.getMessage());
        }
    }
}





