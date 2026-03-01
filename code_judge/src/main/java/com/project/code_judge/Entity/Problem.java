package com.project.code_judge.Entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "problems")
public class Problem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String title;
    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(unique = true, nullable = false)
    private String slug;

    private Integer testCaseCount;
    private Double timeLimitSeconds;
    private Integer memoryLimitMb;
    @Enumerated(value = EnumType.STRING)
    private Difficulty difficulty;
}
