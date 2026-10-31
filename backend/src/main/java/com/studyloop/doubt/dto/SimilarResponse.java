package com.studyloop.doubt.dto;

import com.studyloop.subject.dto.SubjectRef;
import com.studyloop.subject.dto.TopicRef;
import java.time.Instant;
import java.util.List;

public record SimilarResponse(List<Item> items) {

    public record Item(Long doubtId, String title, SubjectRef subject, TopicRef topic, double similarity,
                       String acceptedAnswerExcerpt, String acceptedAnswerAuthor, Instant solvedAt) {}
}
