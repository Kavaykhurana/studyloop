package com.studyloop.answer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PostAnswerRequest(
        @NotBlank(message = "is required")
        @Size.List({@Size(min = 20, message = "must be at least 20 characters"),
                @Size(max = 10000, message = "must be at most 10000 characters")})
        String bodyMd) {}
