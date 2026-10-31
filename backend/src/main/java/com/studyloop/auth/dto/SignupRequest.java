package com.studyloop.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @NotBlank(message = "is required") @Size(min = 2, max = 80, message = "must be 2 to 80 characters") String name,
        @NotBlank(message = "is required") @Email(message = "must be a valid email") @Size(max = 254, message = "is too long")
        String email,
        @NotBlank(message = "is required") @Size(max = 72, message = "must be at most 72 characters") String password,
        @Min(value = 1, message = "must be between 1 and 5") @Max(value = 5, message = "must be between 1 and 5")
        Integer yearOfStudy,
        @Size(max = 40, message = "must be at most 40 characters") String branch) {}
