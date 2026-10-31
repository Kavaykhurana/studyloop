package com.studyloop.seed;

import com.studyloop.common.error.ApiException;
import com.studyloop.common.error.ErrorCode;
import com.studyloop.common.ratelimit.RateLimiter;
import com.studyloop.config.AppProperties;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Operator-only endpoint that wipes the demo campus content and seeds it again. */
@RestController
@RequestMapping("/api/admin/demo")
@Tag(name = "Admin")
public class DemoResetController {

    private static final Logger log = LoggerFactory.getLogger(DemoResetController.class);
    private static final long TIET = 1L;

    private final AppProperties props;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    private final DemoSeeder seeder;
    private final RateLimiter rateLimiter;
    private final AtomicBoolean running = new AtomicBoolean();

    public DemoResetController(AppProperties props, JdbcTemplate jdbc, TransactionTemplate tx, DemoSeeder seeder,
                               RateLimiter rateLimiter) {
        this.props = props;
        this.jdbc = jdbc;
        this.tx = tx;
        this.seeder = seeder;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/reset")
    public ResponseEntity<Void> reset(@RequestHeader(value = "X-Admin-Token", required = false) String token) {
        String expected = props.demo().adminToken();
        if (expected == null || expected.isBlank()) {
            throw new ApiException(ErrorCode.NOT_FOUND);
        }
        boolean ok = token != null && MessageDigest.isEqual(token.getBytes(StandardCharsets.UTF_8),
                expected.getBytes(StandardCharsets.UTF_8));
        if (!ok) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }
        if (!running.compareAndSet(false, true)) {
            return ResponseEntity.accepted().build();
        }
        Thread.ofVirtual().name("demo-reset").start(() -> {
            try {
                wipe();
                rateLimiter.reset();
                seeder.seed();
            } catch (RuntimeException e) {
                log.error("Demo reset failed", e);
            } finally {
                running.set(false);
            }
        });
        return ResponseEntity.accepted().build();
    }

    private void wipe() {
        tx.executeWithoutResult(status -> {
            jdbc.update("DELETE FROM doubts WHERE campus_id = ?", TIET);
            jdbc.update("DELETE FROM users WHERE is_demo");
            jdbc.update("""
                    UPDATE users u SET reputation =
                        COALESCE((SELECT SUM(e.delta) FROM reputation_events e WHERE e.user_id = u.id), 0)
                    WHERE u.campus_id = ?""", TIET);
        });
    }
}
