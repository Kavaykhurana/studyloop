package com.studyloop.doubt;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

/** The embedding column is deliberately not mapped; see EmbeddingStore. */
@Entity
@Table(name = "doubts")
@Getter
@Setter
@NoArgsConstructor
public class Doubt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long campusId;

    private Long authorId;

    private Long subjectId;

    private Long topicId;

    private String title;

    @Column(name = "body_md", columnDefinition = "text")
    private String bodyMd;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    private Integer difficulty;

    @Enumerated(EnumType.STRING)
    private DoubtStatus status = DoubtStatus.OPEN;

    @Enumerated(EnumType.STRING)
    private DoubtResolution resolution;

    @Enumerated(EnumType.STRING)
    private DoubtSource source = DoubtSource.TEXT;

    private int answerCount;

    private int viewCount;

    private Instant createdAt;

    private Instant firstAnswerAt;

    private Instant solvedAt;
}
