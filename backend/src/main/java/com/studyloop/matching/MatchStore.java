package com.studyloop.matching;

import com.studyloop.matching.dto.MatchDto;
import com.studyloop.user.dto.UserBrief;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/** Persistence for doubt_matches (JSONB columns are handled here, not through Hibernate). */
@Component
public class MatchStore {

    private final JdbcTemplate jdbc;
    private final NamedParameterJdbcTemplate named;
    private final JsonMapper mapper;

    public MatchStore(JdbcTemplate jdbc, NamedParameterJdbcTemplate named, JsonMapper mapper) {
        this.jdbc = jdbc;
        this.named = named;
        this.mapper = mapper;
    }

    public List<MatchDto> find(long doubtId) {
        return jdbc.query("""
                SELECT m.rank, m.score, m.breakdown::text AS breakdown, m.reasons::text AS reasons,
                       u.id, u.name, u.year_of_study, u.branch, u.reputation
                FROM doubt_matches m JOIN users u ON u.id = m.helper_id
                WHERE m.doubt_id = ? ORDER BY m.rank""", (rs, i) -> new MatchDto(
                new UserBrief(rs.getLong("id"), rs.getString("name"), (Integer) rs.getObject("year_of_study"),
                        rs.getString("branch"), rs.getInt("reputation")),
                rs.getInt("rank"), rs.getInt("score"),
                mapper.readValue(rs.getString("breakdown"), new TypeReference<Map<String, Double>>() {}),
                mapper.readValue(rs.getString("reasons"), new TypeReference<List<String>>() {})), doubtId);
    }

    public Map<Long, Boolean> notifiedByHelper(long doubtId) {
        Map<Long, Boolean> result = new HashMap<>();
        jdbc.query("SELECT helper_id, notified FROM doubt_matches WHERE doubt_id = ?",
                rs -> {
                    result.put(rs.getLong("helper_id"), rs.getBoolean("notified"));
                }, doubtId);
        return result;
    }

    public void upsert(long doubtId, long helperId, int rank, int score, Map<String, Double> breakdown,
                       List<String> reasons) {
        jdbc.update("""
                INSERT INTO doubt_matches (doubt_id, helper_id, rank, score, breakdown, reasons)
                VALUES (?, ?, ?, ?, CAST(? AS jsonb), CAST(? AS jsonb))
                ON CONFLICT (doubt_id, helper_id) DO UPDATE
                SET rank = EXCLUDED.rank, score = EXCLUDED.score,
                    breakdown = EXCLUDED.breakdown, reasons = EXCLUDED.reasons""",
                doubtId, helperId, rank, score, mapper.writeValueAsString(breakdown),
                mapper.writeValueAsString(reasons));
    }

    public void deleteExcept(long doubtId, List<Long> keepHelperIds) {
        if (keepHelperIds.isEmpty()) {
            jdbc.update("DELETE FROM doubt_matches WHERE doubt_id = ?", doubtId);
            return;
        }
        named.update("DELETE FROM doubt_matches WHERE doubt_id = :doubt AND helper_id NOT IN (:keep)",
                new MapSqlParameterSource().addValue("doubt", doubtId).addValue("keep", keepHelperIds));
    }

    public void markNotified(long doubtId, long helperId) {
        jdbc.update("UPDATE doubt_matches SET notified = true WHERE doubt_id = ? AND helper_id = ?", doubtId,
                helperId);
    }

    /** Open doubts on the caller's campus that were routed to this helper, newest first. */
    public List<Long> openMatchedDoubtIds(long helperId, long campusId) {
        return jdbc.queryForList("""
                SELECT dm.doubt_id FROM doubt_matches dm JOIN doubts d ON d.id = dm.doubt_id
                WHERE dm.helper_id = ? AND d.status = 'OPEN' AND d.campus_id = ?
                ORDER BY d.created_at DESC LIMIT 500""", Long.class, helperId, campusId);
    }

    public int count(long doubtId) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM doubt_matches WHERE doubt_id = ?", Integer.class,
                doubtId);
        return n == null ? 0 : n;
    }

    public List<Long> notifiedHelpers(long doubtId) {
        return new ArrayList<>(jdbc.queryForList(
                "SELECT helper_id FROM doubt_matches WHERE doubt_id = ? AND notified ORDER BY rank", Long.class,
                doubtId));
    }
}
