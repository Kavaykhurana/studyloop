package com.studyloop.user.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public record UpdateSubjectsRequest(
        @NotEmpty(message = "Pick at least one subject.")
        @Size(max = 10, message = "must have at most 10 subjects")
        List<@Valid Entry> subjects) {

    public record Entry(
            @NotNull(message = "is required") Long subjectId,
            @NotNull(message = "is required") @Pattern(regexp = "STRONG|NEEDS_HELP", message = "must be STRONG or NEEDS_HELP")
            String kind,
            @NotNull(message = "is required") @Min(value = 1, message = "must be between 1 and 5")
            @Max(value = 5, message = "must be between 1 and 5") Integer level) {}
}
