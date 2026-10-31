package com.studyloop.common.web;

import org.springframework.security.oauth2.jwt.Jwt;

/** Identity of the caller, resolved from the access token. Services take it explicitly. */
public record CurrentUser(long userId, long campusId, String role, boolean demo) {

    public static CurrentUser from(Jwt jwt) {
        Number campus = jwt.getClaim("cmp");
        Boolean demo = jwt.getClaim("demo");
        return new CurrentUser(Long.parseLong(jwt.getSubject()), campus.longValue(), jwt.getClaimAsString("role"),
                Boolean.TRUE.equals(demo));
    }
}
