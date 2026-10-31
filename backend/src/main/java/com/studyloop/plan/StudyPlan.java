package com.studyloop.plan;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "study_plans")
@Getter
@Setter
@NoArgsConstructor
public class StudyPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    private String examName;

    private LocalDate examDate;

    @Column(precision = 3, scale = 1)
    private BigDecimal hoursPerDay;

    private String summary;

    private String model;

    @Column(name = "is_active")
    private boolean active = true;

    private Instant createdAt;
}
