package com.studyloop.plan;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "plan_tasks")
@Getter
@Setter
@NoArgsConstructor
public class PlanTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long planId;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    private Integer dayIndex;

    private LocalDate planDate;

    private String dayFocus;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    private Integer position;

    private String title;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    private Integer minutes;

    @Enumerated(EnumType.STRING)
    private PlanTaskKind kind;

    private Long subjectId;

    private Long topicId;

    private Instant doneAt;
}
