package com.studyloop.doubt.dto;

import com.studyloop.answer.dto.AnswerDto;
import com.studyloop.hint.dto.HintDto;
import com.studyloop.matching.dto.MatchDto;
import com.studyloop.subject.dto.SubjectRef;
import com.studyloop.subject.dto.TopicRef;
import com.studyloop.user.dto.UserBrief;
import java.time.Instant;
import java.util.List;

public record DoubtDetail(Long id, String title, SubjectRef subject, TopicRef topic, Integer difficulty,
                          String status, String resolution, String source, UserBrief author, int answerCount,
                          Instant createdAt, Instant solvedAt, String bodyMd, int viewCount, Instant firstAnswerAt,
                          List<AnswerDto> answers, List<MatchDto> matches, Hints hints, Permissions permissions) {

    public record Hints(int levelUnlocked, List<HintDto> items) {}

    public record Permissions(boolean isAuthor, boolean canAnswer, boolean canAccept, boolean canResolveByHint,
                              boolean canDelete, boolean canRefreshMatches) {}
}
