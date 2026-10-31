package com.studyloop.matching;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/** Bulk queries feeding the matching algorithm: one query per signal, never one per candidate. */
@Component
public class MatchCandidateDao {

    public record Semantic(double bestSim, int similarSolved) {}

    private final NamedParameterJdbcTemplate named;

    public MatchCandidateDao(NamedParameterJdbcTemplate named) {
        this.named = named;
    }

    public Map<Long, Semantic> semantic(String vector, long campusId, long subjectId, long authorId, Instant since,
                                        double groundingThreshold) {
        Map<Long, Semantic> result = new HashMap<>();
        named.query("""
                SELECT a.author_id AS helper_id,
                       MAX(1 - (d.embedding <=> CAST(:vec AS vector)))                                AS best_sim,
                       COUNT(*) FILTER (WHERE 1 - (d.embedding <=> CAST(:vec AS vector)) >= :ground)  AS similar_solved
                FROM answers a
                JOIN doubts d ON d.id = a.doubt_id
                WHERE a.is_accepted
                  AND d.campus_id = :campus
                  AND d.subject_id = :subject
                  AND d.embedding IS NOT NULL
                  AND a.accepted_at > :since
                  AND a.author_id <> :author
                GROUP BY a.author_id""",
                new MapSqlParameterSource().addValue("vec", vector).addValue("ground", groundingThreshold)
                        .addValue("campus", campusId).addValue("subject", subjectId)
                        .addValue("since", Timestamp.from(since)).addValue("author", authorId),
                rs -> {
                    result.put(rs.getLong("helper_id"),
                            new Semantic(rs.getDouble("best_sim"), rs.getInt("similar_solved")));
                });
        return result;
    }

    public Map<Long, Integer> strongLevels(long campusId, long subjectId, long authorId) {
        Map<Long, Integer> result = new HashMap<>();
        named.query("""
                SELECT us.user_id, us.level FROM user_subjects us JOIN users u ON u.id = us.user_id
                WHERE us.subject_id = :subject AND us.kind = 'STRONG' AND u.campus_id = :campus AND u.id <> :author""",
                new MapSqlParameterSource().addValue("subject", subjectId).addValue("campus", campusId)
                        .addValue("author", authorId),
                rs -> {
                    result.put(rs.getLong("user_id"), rs.getInt("level"));
                });
        return result;
    }

    public Set<Long> acceptedAuthors(long campusId, long subjectId, long authorId) {
        Set<Long> result = new HashSet<>();
        named.query("""
                SELECT DISTINCT a.author_id FROM answers a JOIN doubts d ON d.id = a.doubt_id
                WHERE a.is_accepted AND d.campus_id = :campus AND d.subject_id = :subject AND a.author_id <> :author""",
                new MapSqlParameterSource().addValue("campus", campusId).addValue("subject", subjectId)
                        .addValue("author", authorId),
                rs -> {
                    result.add(rs.getLong("author_id"));
                });
        return result;
    }

    public Map<Long, Instant> lastActive(Collection<Long> ids) {
        Map<Long, Instant> result = new HashMap<>();
        if (ids.isEmpty()) {
            return result;
        }
        named.query("SELECT id, last_active_at FROM users WHERE id IN (:ids)",
                new MapSqlParameterSource("ids", ids), rs -> {
                    result.put(rs.getLong("id"), rs.getTimestamp("last_active_at").toInstant());
                });
        return result;
    }

    public Map<Long, Integer> subjectReputation(long subjectId, Collection<Long> ids) {
        Map<Long, Integer> result = new HashMap<>();
        if (ids.isEmpty()) {
            return result;
        }
        named.query("""
                SELECT user_id, SUM(delta) AS rep FROM reputation_events
                WHERE subject_id = :subject AND user_id IN (:ids) GROUP BY user_id""",
                new MapSqlParameterSource().addValue("subject", subjectId).addValue("ids", ids), rs -> {
                    result.put(rs.getLong("user_id"), rs.getInt("rep"));
                });
        return result;
    }

    public int maxSubjectReputation(long subjectId, long campusId) {
        Integer max = named.queryForObject("""
                SELECT COALESCE(MAX(s), 0) FROM (
                    SELECT SUM(re.delta) AS s FROM reputation_events re JOIN users u ON u.id = re.user_id
                    WHERE re.subject_id = :subject AND u.campus_id = :campus GROUP BY re.user_id) t""",
                new MapSqlParameterSource().addValue("subject", subjectId).addValue("campus", campusId),
                Integer.class);
        return max == null ? 0 : max;
    }

    /** Open doubts matched to each helper in the last 48 h that the helper has not answered. */
    public Map<Long, Integer> openMatches(Collection<Long> ids, Instant since, long excludeDoubtId) {
        Map<Long, Integer> result = new HashMap<>();
        if (ids.isEmpty()) {
            return result;
        }
        named.query("""
                SELECT dm.helper_id, COUNT(*) AS open_matches
                FROM doubt_matches dm JOIN doubts d ON d.id = dm.doubt_id
                WHERE dm.helper_id IN (:ids) AND dm.created_at > :since AND d.status = 'OPEN' AND d.id <> :self
                  AND NOT EXISTS (SELECT 1 FROM answers a WHERE a.doubt_id = d.id AND a.author_id = dm.helper_id)
                GROUP BY dm.helper_id""",
                new MapSqlParameterSource().addValue("ids", ids).addValue("since", Timestamp.from(since))
                        .addValue("self", excludeDoubtId), rs -> {
                    result.put(rs.getLong("helper_id"), rs.getInt("open_matches"));
                });
        return result;
    }

    public List<Long> rematchCandidates(Instant createdAfter, Instant createdBefore, Instant matchOlderThan) {
        return named.queryForList("""
                SELECT d.id FROM doubts d
                WHERE d.status = 'OPEN' AND d.answer_count = 0
                  AND d.created_at > :after AND d.created_at <= :before
                  AND (SELECT COUNT(*) FROM doubt_matches m WHERE m.doubt_id = d.id) < 5
                  AND (NOT EXISTS (SELECT 1 FROM doubt_matches m WHERE m.doubt_id = d.id)
                       OR EXISTS (SELECT 1 FROM doubt_matches m WHERE m.doubt_id = d.id AND m.created_at < :old))
                ORDER BY d.id""",
                new MapSqlParameterSource().addValue("after", Timestamp.from(createdAfter))
                        .addValue("before", Timestamp.from(createdBefore))
                        .addValue("old", Timestamp.from(matchOlderThan)), Long.class);
    }
}
