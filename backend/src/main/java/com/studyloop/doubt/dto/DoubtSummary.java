package com.studyloop.doubt.dto;

import com.studyloop.subject.dto.SubjectRef;
import com.studyloop.subject.dto.TopicRef;
import com.studyloop.user.dto.UserBrief;
import java.time.Instant;

public record DoubtSummary(Long id, String title, String excerpt, SubjectRef subject, TopicRef topic,
                           Integer difficulty, String status, String resolution, String source, UserBrief author,
                           int answerCount, Instant createdAt, Instant solvedAt) {}
