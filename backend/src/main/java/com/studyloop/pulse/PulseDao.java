package com.studyloop.pulse;

import com.studyloop.pulse.dto.PulseDto;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/** All Campus Pulse SQL. Every query is scoped to one campus. */
@Component
public class PulseDao {

    public record DoubtTotals(long posted, long solved, long solvedByPeers, long solvedByHints, Double median) {}

    public record TopicRow(long subjectId, long topicId, long count, long openCount) {}

    private final NamedParameterJdbcTemplate jdbc;

    public PulseDao(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public DoubtTotals doubtTotals(long campusId, Instant since) {
        return jdbc.queryForObject("""
                SELECT COUNT(*) FILTER (WHERE created_at >= :since) AS posted,
                       COUNT(*) FILTER (WHERE solved_at >= :since) AS solved,
                       COUNT(*) FILTER (WHERE solved_at >= :since AND resolution = 'PEER_ANSWER') AS by_peers,
                       COUNT(*) FILTER (WHERE solved_at >= :since AND resolution = 'AI_HINT') AS by_hints,
                       percentile_cont(0.5) WITHIN GROUP (
                           ORDER BY EXTRACT(EPOCH FROM first_answer_at - created_at) / 60)
                           FILTER (WHERE created_at >= :since AND first_answer_at IS NOT NULL) AS median
                FROM doubts WHERE campus_id = :campus""", params(campusId, since), (rs, i) -> new DoubtTotals(
                rs.getLong("posted"), rs.getLong("solved"), rs.getLong("by_peers"), rs.getLong("by_hints"),
                rs.getObject("median") == null ? null : rs.getDouble("median")));
    }

    public long deflections(long campusId, Instant since) {
        Long n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM deflection_events WHERE campus_id = :campus AND created_at >= :since",
                params(campusId, since), Long.class);
        return n == null ? 0 : n;
    }

    public long activeHelpers(long campusId, Instant since) {
        Long n = jdbc.queryForObject("""
                SELECT COUNT(DISTINCT a.author_id) FROM answers a JOIN doubts d ON d.id = a.doubt_id
                WHERE d.campus_id = :campus AND a.created_at >= :since""", params(campusId, since), Long.class);
        return n == null ? 0 : n;
    }

    public List<PulseDto.Daily> daily(long campusId, LocalDate from, LocalDate to) {
        return jdbc.query("""
                SELECT g.day::date AS day,
                       (SELECT COUNT(*) FROM doubts d WHERE d.campus_id = :campus
                          AND (d.created_at AT TIME ZONE 'Asia/Kolkata')::date = g.day::date) AS posted,
                       (SELECT COUNT(*) FROM doubts d WHERE d.campus_id = :campus AND d.solved_at IS NOT NULL
                          AND (d.solved_at AT TIME ZONE 'Asia/Kolkata')::date = g.day::date) AS solved
                FROM generate_series(CAST(:from AS date), CAST(:to AS date), interval '1 day') AS g(day)
                ORDER BY g.day""", new MapSqlParameterSource().addValue("campus", campusId)
                        .addValue("from", java.sql.Date.valueOf(from)).addValue("to", java.sql.Date.valueOf(to)),
                (rs, i) -> new PulseDto.Daily(rs.getDate("day").toLocalDate(), rs.getLong("posted"),
                        rs.getLong("solved")));
    }

    public List<TopicRow> topicCounts(long campusId, Instant since) {
        return jdbc.query("""
                SELECT subject_id, topic_id, COUNT(*) AS cnt, COUNT(*) FILTER (WHERE status = 'OPEN') AS open_cnt
                FROM doubts WHERE campus_id = :campus AND created_at >= :since AND topic_id IS NOT NULL
                GROUP BY subject_id, topic_id ORDER BY subject_id, cnt DESC, topic_id""", params(campusId, since),
                (rs, i) -> new TopicRow(rs.getLong("subject_id"), rs.getLong("topic_id"), rs.getLong("cnt"),
                        rs.getLong("open_cnt")));
    }

    public List<PulseDto.TopHelper> topHelpers(long campusId, Instant since) {
        return jdbc.query("""
                SELECT u.id, u.name, u.reputation, COUNT(*) AS accepted
                FROM answers a JOIN doubts d ON d.id = a.doubt_id JOIN users u ON u.id = a.author_id
                WHERE d.campus_id = :campus AND a.is_accepted AND a.accepted_at >= :since
                GROUP BY u.id, u.name, u.reputation
                ORDER BY accepted DESC, u.reputation DESC, u.id LIMIT 5""", params(campusId, since),
                (rs, i) -> new PulseDto.TopHelper(rs.getLong("id"), rs.getString("name"), rs.getLong("accepted"),
                        rs.getInt("reputation")));
    }

    public PulseDto.AiHealth aiHealth(Instant since) {
        return jdbc.queryForObject("""
                SELECT COUNT(*) AS calls, COUNT(*) FILTER (WHERE success) AS ok,
                       percentile_cont(0.5) WITHIN GROUP (ORDER BY latency_ms) AS p50
                FROM ai_calls WHERE created_at >= :since""",
                new MapSqlParameterSource("since", Timestamp.from(since)), (rs, i) -> {
                    long calls = rs.getLong("calls");
                    return new PulseDto.AiHealth(calls, calls == 0 ? 0 : (double) rs.getLong("ok") / calls,
                            rs.getObject("p50") == null ? null : Math.round(rs.getDouble("p50")));
                });
    }

    public boolean hasDemoUsers(long campusId) {
        Boolean b = jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM users WHERE campus_id = :campus AND is_demo)",
                new MapSqlParameterSource("campus", campusId), Boolean.class);
        return Boolean.TRUE.equals(b);
    }

    public String campusShortName(long campusId) {
        return jdbc.queryForObject("SELECT short_name FROM campuses WHERE id = :campus",
                new MapSqlParameterSource("campus", campusId), String.class);
    }

    private static MapSqlParameterSource params(long campusId, Instant since) {
        return new MapSqlParameterSource().addValue("campus", campusId).addValue("since", Timestamp.from(since));
    }
}
