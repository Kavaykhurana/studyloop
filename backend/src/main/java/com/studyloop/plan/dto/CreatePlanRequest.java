package com.studyloop.plan.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CreatePlanRequest(
        @NotBlank(message = "is required") @Size(min = 3, max = 100, message = "must be 3 to 100 characters")
        String examName,
        @NotNull(message = "is required") LocalDate examDate,
        @NotNull(message = "is required") @DecimalMin(value = "0.5", message = "must be between 0.5 and 12")
        @DecimalMax(value = "12", message = "must be between 0.5 and 12") BigDecimal hoursPerDay,
        @NotEmpty(message = "Pick at least one subject.") @Size(max = 4, message = "Pick at most four subjects.")
        List<Long> subjectIds) {}
