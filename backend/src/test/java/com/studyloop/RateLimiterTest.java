package com.studyloop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.studyloop.common.error.ApiException;
import com.studyloop.common.error.ErrorCode;
import com.studyloop.common.ratelimit.RateLimitKey;
import com.studyloop.common.ratelimit.RateLimiter;
import com.studyloop.support.MutableClock;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class RateLimiterTest {

    private final MutableClock clock = new MutableClock();
    private final RateLimiter limiter = new RateLimiter(clock);

    @Test
    void theLimitIsEnforcedAndRetryAfterIsSet() {
        for (int i = 0; i < RateLimitKey.LOGIN.limit(); i++) {
            limiter.check(RateLimitKey.LOGIN, "1.2.3.4");
        }
        assertThatThrownBy(() -> limiter.check(RateLimitKey.LOGIN, "1.2.3.4")).isInstanceOfSatisfying(
                ApiException.class, e -> {
                    assertThat(e.code()).isEqualTo(ErrorCode.RATE_LIMITED);
                    assertThat(e.retryAfterSeconds()).isBetween(1, 60);
                    assertThat(e.getMessage()).startsWith("Slow down a little — try again in ")
                            .endsWith(" seconds.");
                });
    }

    @Test
    void subjectsAreCountedIndependently() {
        for (int i = 0; i < RateLimitKey.LOGIN.limit(); i++) {
            limiter.check(RateLimitKey.LOGIN, "1.2.3.4");
        }
        assertThatCode(() -> limiter.check(RateLimitKey.LOGIN, "5.6.7.8")).doesNotThrowAnyException();
        assertThatCode(() -> limiter.check(RateLimitKey.SIGNUP, "1.2.3.4")).doesNotThrowAnyException();
    }

    @Test
    void theWindowResetsAfterItsDuration() {
        for (int i = 0; i < RateLimitKey.LOGIN.limit(); i++) {
            limiter.check(RateLimitKey.LOGIN, "1.2.3.4");
        }
        clock.advance(Duration.ofSeconds(61));
        assertThatCode(() -> limiter.check(RateLimitKey.LOGIN, "1.2.3.4")).doesNotThrowAnyException();
    }

    @Test
    void hourlyLimitsFollowTheSpecification() {
        assertThat(RateLimitKey.DOUBT_CREATE.limit()).isEqualTo(10);
        assertThat(RateLimitKey.ANSWER.limit()).isEqualTo(30);
        assertThat(RateLimitKey.SIMILAR.limit()).isEqualTo(60);
        assertThat(RateLimitKey.HINT.limit()).isEqualTo(30);
        assertThat(RateLimitKey.SNAP.limit()).isEqualTo(10);
        assertThat(RateLimitKey.PLAN.limit()).isEqualTo(5);
        assertThat(RateLimitKey.SIGNUP.limit()).isEqualTo(5);
        assertThat(RateLimitKey.DOUBT_CREATE.windowSeconds()).isEqualTo(3600);
    }
}
