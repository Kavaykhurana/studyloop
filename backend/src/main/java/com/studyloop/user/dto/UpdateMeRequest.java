package com.studyloop.user.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateMeRequest(
        @NotBlank(message = "is required") @Size(min = 2, max = 80, message = "must be 2 to 80 characters") String name,
        @Min(value = 1, message = "must be between 1 and 5") @Max(value = 5, message = "must be between 1 and 5")
        Integer yearOfStudy,
        @Size(max = 40, message = "must be at most 40 characters") String branch,
        @Size(max = 280, message = "must be at most 280 characters") String bio) {}
