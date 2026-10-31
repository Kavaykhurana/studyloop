package com.studyloop.answer.dto;

import com.studyloop.user.dto.UserBrief;
import java.time.Instant;

public record AnswerDto(Long id, Long doubtId, String bodyMd, UserBrief author, boolean isAccepted,
                        Integer helpfulRating, Instant createdAt, Instant acceptedAt, boolean isMine) {}
