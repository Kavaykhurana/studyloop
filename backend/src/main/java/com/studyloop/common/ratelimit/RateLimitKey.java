package com.studyloop.common.ratelimit;

public enum RateLimitKey {
    LOGIN(10, 60),
    SIGNUP(5, 60),
    DEMO(30, 60),
    DOUBT_CREATE(10, 3600),
    ANSWER(30, 3600),
    SIMILAR(60, 3600),
    HINT(30, 3600),
    SNAP(10, 3600),
    PLAN(5, 3600);

    private final int limit;
    private final int windowSeconds;

    RateLimitKey(int limit, int windowSeconds) {
        this.limit = limit;
        this.windowSeconds = windowSeconds;
    }

    public int limit() {
        return limit;
    }

    public int windowSeconds() {
        return windowSeconds;
    }
}
