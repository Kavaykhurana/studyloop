package com.studyloop.common.web;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Updates users.last_active_at at most once per five minutes per user. */
@Component
public class UserActivityTracker {

    private static final Duration GATE = Duration.ofMinutes(5);

    private final JdbcTemplate jdbc;
    private final Clock clock;
    private final ConcurrentHashMap<Long, Instant> lastWrite = new ConcurrentHashMap<>();

    public UserActivityTracker(JdbcTemplate jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    public void touch(long userId) {
        Instant now = clock.instant();
        boolean[] write = {false};
        lastWrite.compute(userId, (id, previous) -> {
            if (previous == null || Duration.between(previous, now).compareTo(GATE) >= 0) {
                write[0] = true;
                return now;
            }
            return previous;
        });
        if (write[0]) {
            jdbc.update("UPDATE users SET last_active_at = ? WHERE id = ?", java.sql.Timestamp.from(now), userId);
        }
    }
}
