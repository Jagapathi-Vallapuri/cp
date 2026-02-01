package com.project.code_judge.Repository;

import com.project.code_judge.Entity.Problem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProblemRepository extends JpaRepository<Problem, Long> {
}
