package com.studyloop.common.ratelimit;

import com.studyloop.common.error.ApiException;
import com.studyloop.common.error.ErrorCode;
import java.time.Clock;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** In-memory fixed-window counters keyed by (limit key, subject). */
@Component
public class RateLimiter {

    private static final long EVICT_AFTER_SECONDS = 2 * 3600;

    private final Clock clock;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    public RateLimiter(Clock clock) {
        this.clock = clock;
    }

    /** Counts one hit; throws 429 RATE_LIMITED with Retry-After when the window is exhausted. */
    public void check(RateLimitKey key, String subject) {
        long now = clock.instant().getEpochSecond();
        long windowStart = now - (now % key.windowSeconds());
        Window window = windows.compute(key.name() + ":" + subject, (k, existing) -> {
            if (existing == null || existing.start != windowStart) {
                return new Window(windowStart);
            }
            return existing;
        });
        if (window.count.incrementAndGet() > key.limit()) {
            long retryAfter = Math.max(1, windowStart + key.windowSeconds() - now);
            throw new ApiException(ErrorCode.RATE_LIMITED,
                    "Slow down a little — try again in " + retryAfter + " seconds.", (int) retryAfter);
        }
    }

    /** Forgets every counter. Used by the demo reset and by tests that share one application context. */
    public void reset() {
        windows.clear();
    }

    @Scheduled(fixedDelay = 600_000)
    public void evictOldWindows() {
        long cutoff = clock.instant().getEpochSecond() - EVICT_AFTER_SECONDS;
        windows.entrySet().removeIf(e -> e.getValue().start < cutoff);
    }

    private static final class Window {
        final long start;
        final AtomicInteger count = new AtomicInteger();

        Window(long start) {
            this.start = start;
        }
    }
}
