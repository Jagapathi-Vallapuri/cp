package com.project.code_judge.Repository;

import com.project.code_judge.Entity.Problem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProblemRepository extends JpaRepository<Problem, Long> {
    Optional<Problem> findBySlug(String slug);
    boolean existsBySlug(String slug);
}
