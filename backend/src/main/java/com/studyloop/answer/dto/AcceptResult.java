package com.studyloop.answer.dto;

public record AcceptResult(Long doubtId, Long answerId, Awarded awarded) {

    public record Awarded(int helper, int author) {}
}
