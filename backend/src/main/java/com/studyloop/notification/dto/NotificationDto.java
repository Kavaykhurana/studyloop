package com.studyloop.notification.dto;

import com.studyloop.user.dto.UserBrief;
import java.time.Instant;

public record NotificationDto(Long id, String type, String message, Long doubtId, UserBrief actor, boolean read,
                              Instant createdAt) {}
