package com.studyloop.user;

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
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long campusId;

    private String name;

    @Column(name = "email", columnDefinition = "citext")
    private String email;

    private String passwordHash;

    private String role = "STUDENT";

    @JdbcTypeCode(SqlTypes.SMALLINT)
    private Integer yearOfStudy;

    private String branch;

    private String bio;

    private int reputation;

    @Column(name = "is_demo")
    private boolean demo;

    private boolean onboarded;

    private Instant lastActiveAt;

    private Instant createdAt;

    public String firstName() {
        int space = name.indexOf(' ');
        return space < 0 ? name : name.substring(0, space);
    }
}
