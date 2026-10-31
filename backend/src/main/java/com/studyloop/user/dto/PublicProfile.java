package com.studyloop.user.dto;

import com.studyloop.doubt.dto.DoubtSummary;
import java.util.List;

public record PublicProfile(UserBrief user, String bio, List<StrongSubject> strongSubjects,
                            List<SubjectReputation> subjectReputation, Stats stats,
                            List<DoubtSummary> recentAccepted) {

    public record StrongSubject(String code, int level) {}

    public record SubjectReputation(String code, int reputation) {}

    public record Stats(long answers, long accepted, Double avgHelpful) {}
}
