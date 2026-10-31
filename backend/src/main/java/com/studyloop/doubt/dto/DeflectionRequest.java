package com.studyloop.doubt.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DeflectionRequest(
        @NotNull(message = "is required") Long sourceDoubtId,
        @NotBlank(message = "is required") @Size(max = 500, message = "must be at most 500 characters")
        String queryText,
        @NotNull(message = "is required") @DecimalMin(value = "0.0", message = "must be between 0 and 1")
        @DecimalMax(value = "1.0", message = "must be between 0 and 1") Double similarity) {}
