package com.studyloop.ai.dto;

import java.util.List;

public record PlanResult(String summary, List<Day> days) {

    public record Day(String date, String focus, List<Task> tasks) {}

    public record Task(String title, Integer minutes, String kind, String subjectCode, String topic) {}
}
