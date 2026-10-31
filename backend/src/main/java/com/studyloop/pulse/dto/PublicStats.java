package com.studyloop.pulse.dto;

public record PublicStats(long doubtsSolved, double selfServeRate, Double medianFirstAnswerMinutes,
                          boolean isSeededDemo) {}
