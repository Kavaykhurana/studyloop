package com.studyloop.common.web;

import jakarta.servlet.http.HttpServletRequest;

public final class ClientIp {

    private ClientIp() {}

    /** First entry of X-Forwarded-For when present, otherwise the socket address. */
    public static String of(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
