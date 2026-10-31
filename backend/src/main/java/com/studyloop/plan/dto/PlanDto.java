package com.studyloop.plan.dto;

import com.studyloop.subject.dto.SubjectRef;
import com.studyloop.subject.dto.TopicRef;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record PlanDto(Long id, String examName, LocalDate examDate, BigDecimal hoursPerDay, String summary,
                      Instant createdAt, Progress progress, List<Day> days) {

    public record Progress(long done, long total) {}

    public record Day(int dayIndex, LocalDate date, String focus, int totalMinutes, List<Task> tasks) {}

    public record Task(Long id, String title, int minutes, String kind, SubjectRef subject, TopicRef topic,
                       boolean done) {}
}
