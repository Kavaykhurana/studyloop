package com.studyloop.hint;

import com.studyloop.hint.dto.HintDto;
import java.sql.Array;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/** ai_hints and hint_progress persistence (array columns handled with plain JDBC). */
@Component
public class HintDao {

    public record Row(int level, String contentMd, List<Long> citedIds, String confidence, String model) {}

    private final JdbcTemplate jdbc;
    private final NamedParameterJdbcTemplate named;
    private final Clock clock;

    public HintDao(JdbcTemplate jdbc, NamedParameterJdbcTemplate named, Clock clock) {
        this.jdbc = jdbc;
        this.named = named;
        this.clock = clock;
    }

    public Optional<Row> find(long doubtId, int level) {
        return findUpTo(doubtId, level).stream().filter(r -> r.level() == level).findFirst();
    }

    public List<Row> findUpTo(long doubtId, int maxLevel) {
        if (maxLevel < 1) {
            return List.of();
        }
        return jdbc.query("""
                SELECT level, content_md, cited_doubt_ids, confidence, model FROM ai_hints
                WHERE doubt_id = ? AND level <= ? ORDER BY level""", (rs, i) -> {
            List<Long> cited = new ArrayList<>();
            Array arr = rs.getArray("cited_doubt_ids");
            if (arr != null) {
                for (Object o : (Object[]) arr.getArray()) {
                    cited.add(((Number) o).longValue());
                }
            }
            return new Row(rs.getInt("level"), rs.getString("content_md"), cited, rs.getString("confidence"),
                    rs.getString("model"));
        }, doubtId, maxLevel);
    }

    /** Race-safe insert: whoever loses the race simply re-reads the winner's row. */
    public void insertIfAbsent(long doubtId, int level, String content, List<Long> cited, String confidence,
                               String model) {
        jdbc.update("""
                INSERT INTO ai_hints (doubt_id, level, content_md, cited_doubt_ids, confidence, model, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (doubt_id, level) DO NOTHING""", ps -> {
            ps.setLong(1, doubtId);
            ps.setInt(2, level);
            ps.setString(3, content);
            ps.setArray(4, ps.getConnection().createArrayOf("bigint", cited.toArray(new Long[0])));
            ps.setString(5, confidence);
            ps.setString(6, model);
            ps.setTimestamp(7, Timestamp.from(clock.instant()));
        });
    }

    public int levelUnlocked(long userId, long doubtId) {
        List<Integer> rows = jdbc.queryForList(
                "SELECT level_unlocked FROM hint_progress WHERE user_id = ? AND doubt_id = ?", Integer.class, userId,
                doubtId);
        return rows.isEmpty() ? 0 : rows.get(0);
    }

    public void upsertProgress(long userId, long doubtId, int level) {
        jdbc.update("""
                INSERT INTO hint_progress (user_id, doubt_id, level_unlocked, updated_at) VALUES (?, ?, ?, ?)
                ON CONFLICT (user_id, doubt_id) DO UPDATE
                SET level_unlocked = GREATEST(hint_progress.level_unlocked, EXCLUDED.level_unlocked),
                    updated_at = EXCLUDED.updated_at""", userId, doubtId, level, Timestamp.from(clock.instant()));
    }

    /** Converts stored rungs to DTOs, resolving cited doubt titles (campus-filtered) in a single query. */
    public List<HintDto> toDtos(List<Row> rows, long campusId) {
        Set<Long> ids = new LinkedHashSet<>();
        rows.forEach(r -> ids.addAll(r.citedIds()));
        Map<Long, String> titles = titles(ids, campusId);
        return rows.stream().map(r -> new HintDto(r.level(), r.contentMd(),
                r.citedIds().stream().filter(titles::containsKey)
                        .map(id -> new HintDto.CitedDoubt(id, titles.get(id))).toList(),
                r.confidence())).toList();
    }

    private Map<Long, String> titles(Collection<Long> ids, long campusId) {
        Map<Long, String> result = new HashMap<>();
        if (ids.isEmpty()) {
            return result;
        }
        named.query("SELECT id, title FROM doubts WHERE id IN (:ids) AND campus_id = :campus",
                new MapSqlParameterSource().addValue("ids", ids).addValue("campus", campusId), rs -> {
                    result.put(rs.getLong("id"), rs.getString("title"));
                });
        return result;
    }
}
