package com.studyloop.user.dto;

import java.util.List;

public record UserMe(Long id, String name, String email, CampusRef campus, Integer yearOfStudy, String branch,
                     String bio, int reputation, boolean onboarded, boolean isDemo, List<MySubject> subjects) {

    public record CampusRef(Long id, String name, String shortName) {}

    public record MySubject(Long subjectId, String code, String name, String kind, int level) {}
}
