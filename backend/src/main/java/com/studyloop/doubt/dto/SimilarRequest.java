package com.studyloop.doubt.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SimilarRequest(
        @NotBlank(message = "is required")
        @Size.List({@Size(min = 10, message = "must be at least 10 characters"),
                @Size(max = 160, message = "must be at most 160 characters")})
        String title,
        @Size(max = 8000, message = "must be at most 8000 characters") String bodyMd,
        Long subjectId) {}
