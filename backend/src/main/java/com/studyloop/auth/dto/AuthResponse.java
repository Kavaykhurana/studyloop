package com.studyloop.auth.dto;

import com.studyloop.user.dto.UserMe;

public record AuthResponse(String accessToken, long expiresIn, UserMe user) {}
