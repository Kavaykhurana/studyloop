package com.studyloop.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record DemoRequest(
        @NotBlank(message = "is required") @Pattern(regexp = "learner|helper", message = "must be learner or helper")
        String persona) {}
