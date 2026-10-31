package com.studyloop.reputation;

import java.sql.Timestamp;
import java.time.Clock;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Idempotent reputation ledger: users.reputation only moves when a new ledger row is actually inserted. */
@Service
public class ReputationService {

    private final JdbcTemplate jdbc;
    private final Clock clock;

    public ReputationService(JdbcTemplate jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    /** Returns the points actually awarded: the reason's delta the first time, 0 for a repeat. */
    @Transactional(propagation = Propagation.MANDATORY)
    public int award(long userId, long subjectId, ReputationReason reason, long answerId) {
        int inserted = jdbc.update("""
                INSERT INTO reputation_events (user_id, subject_id, delta, reason, source_answer_id, created_at)
                VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT (user_id, reason, source_answer_id) DO NOTHING""",
                userId, subjectId, reason.delta(), reason.name(), answerId, Timestamp.from(clock.instant()));
        if (inserted == 0) {
            return 0;
        }
        jdbc.update("UPDATE users SET reputation = reputation + ? WHERE id = ?", reason.delta(), userId);
        return reason.delta();
    }
}
