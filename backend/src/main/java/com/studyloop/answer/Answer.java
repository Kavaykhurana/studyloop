package com.studyloop.answer;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "answers")
@Getter
@Setter
@NoArgsConstructor
public class Answer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long doubtId;

    private Long authorId;

    @Column(name = "body_md", columnDefinition = "text")
    private String bodyMd;

    @Column(name = "is_accepted")
    private boolean accepted;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    private Integer helpfulRating;

    private Instant createdAt;

    private Instant acceptedAt;
}
