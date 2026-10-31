package com.studyloop.doubt.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateDoubtRequest(
        @NotBlank(message = "is required")
        @Size.List({@Size(min = 10, message = "must be at least 10 characters"),
                @Size(max = 160, message = "must be at most 160 characters")})
        String title,
        @NotBlank(message = "is required")
        @Size.List({@Size(min = 20, message = "must be at least 20 characters"),
                @Size(max = 8000, message = "must be at most 8000 characters")})
        String bodyMd,
        Long subjectId,
        @Pattern(regexp = "TEXT|SNAP", message = "must be TEXT or SNAP") String source) {}
