package com.studyloop.plan.dto;

import jakarta.validation.constraints.NotNull;

public record TaskToggleRequest(@NotNull(message = "is required") Boolean done) {}
