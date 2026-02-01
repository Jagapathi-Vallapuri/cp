package com.project.code_judge.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Problem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column
    private String description;

    @Column(columnDefinition = "TEXT")
    private String testInput;

    @Column(columnDefinition = "TEXT")
    private String expectedOutput;

    private Double timeLimitSeconds;
    private Integer memoryLimitMb;
}
