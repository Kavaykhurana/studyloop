package com.studyloop.seed;

import java.util.List;

/** Shape of resources/seed/demo-doubts.json. */
public record SeedFile(List<SeedDoubt> doubts) {

    public record SeedDoubt(String key, String subject, String topic, int difficulty, String author, String title,
                            String body, String status, String resolution, double ageHours, int solvedAfterMinutes,
                            int views, List<SeedAnswer> answers, List<SeedHint> hints) {}

    public record SeedAnswer(String author, int afterMinutes, boolean accepted, Integer rating, String body) {}

    public record SeedHint(int level, String content) {}
}
