package com.studyloop.pulse.dto;

import com.studyloop.subject.dto.SubjectRef;
import java.time.LocalDate;
import java.util.List;

public record PulseDto(String campus, int days, boolean isSeededDemo, Totals totals, List<Daily> daily,
                       List<SubjectConfusion> confusionMap, List<TopHelper> topHelpers, AiHealth ai) {

    public record Totals(long doubtsPosted, long solved, double solvedRate, long solvedByPeers, long solvedByHints,
                         long deflections, double selfServeRate, Double medianFirstAnswerMinutes,
                         long activeHelpers) {}

    public record Daily(LocalDate date, long posted, long solved) {}

    public record SubjectConfusion(SubjectRef subject, List<TopicCount> topics) {}

    public record TopicCount(Long topicId, String name, long count, long openCount) {}

    public record TopHelper(Long id, String name, long acceptedInPeriod, int reputation) {}

    public record AiHealth(long calls, double successRate, Long p50LatencyMs) {}
}
