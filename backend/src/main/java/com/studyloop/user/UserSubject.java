package com.studyloop.user;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "user_subjects")
@Getter
@Setter
@NoArgsConstructor
public class UserSubject {

    public enum Kind { STRONG, NEEDS_HELP }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    private Long subjectId;

    @Enumerated(EnumType.STRING)
    private Kind kind;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    private Integer level;

    public UserSubject(Long userId, Long subjectId, Kind kind, Integer level) {
        this.userId = userId;
        this.subjectId = subjectId;
        this.kind = kind;
        this.level = level;
    }
}
