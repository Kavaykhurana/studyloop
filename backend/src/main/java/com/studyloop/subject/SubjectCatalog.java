package com.studyloop.subject;

import com.studyloop.subject.dto.SubjectRef;
import com.studyloop.subject.dto.TopicRef;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Immutable in-memory view of subjects and topics, loaded once at startup. */
@Component
public class SubjectCatalog {

    public static final String GENERAL = "General";

    private final JdbcTemplate jdbc;
    private volatile List<SubjectInfo> subjects = List.of();
    private volatile Map<Long, SubjectInfo> byId = Map.of();
    private volatile Map<String, SubjectInfo> byCode = Map.of();
    private volatile Map<Long, TopicRef> topicById = Map.of();
    private volatile Map<Long, Long> topicSubject = Map.of();
    private volatile Map<String, TopicRef> topicByKey = Map.of();

    public SubjectCatalog(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @PostConstruct
    public void load() {
        Map<Long, List<TopicRef>> topics = new LinkedHashMap<>();
        Map<Long, TopicRef> topicIndex = new LinkedHashMap<>();
        Map<Long, Long> topicOwner = new LinkedHashMap<>();
        Map<String, TopicRef> keyIndex = new LinkedHashMap<>();
        jdbc.query("SELECT id, subject_id, name FROM topics ORDER BY subject_id, sort_order", rs -> {
            long subjectId = rs.getLong("subject_id");
            TopicRef topic = new TopicRef(rs.getLong("id"), rs.getString("name"));
            topics.computeIfAbsent(subjectId, k -> new ArrayList<>()).add(topic);
            topicIndex.put(topic.id(), topic);
            topicOwner.put(topic.id(), subjectId);
            keyIndex.put(topicKey(subjectId, topic.name()), topic);
        });
        List<SubjectInfo> loaded = jdbc.query(
                "SELECT id, code, name, color_hex, sort_order FROM subjects ORDER BY sort_order",
                (rs, i) -> new SubjectInfo(rs.getLong("id"), rs.getString("code"), rs.getString("name"),
                        rs.getString("color_hex"), rs.getInt("sort_order"),
                        List.copyOf(topics.getOrDefault(rs.getLong("id"), List.of()))));
        Map<Long, SubjectInfo> idIndex = new LinkedHashMap<>();
        Map<String, SubjectInfo> codeIndex = new LinkedHashMap<>();
        loaded.forEach(s -> {
            idIndex.put(s.id(), s);
            codeIndex.put(s.code().toUpperCase(Locale.ROOT), s);
        });
        this.subjects = List.copyOf(loaded);
        this.byId = Collections.unmodifiableMap(idIndex);
        this.byCode = Collections.unmodifiableMap(codeIndex);
        this.topicById = Collections.unmodifiableMap(topicIndex);
        this.topicSubject = Collections.unmodifiableMap(topicOwner);
        this.topicByKey = Collections.unmodifiableMap(keyIndex);
    }

    public List<SubjectInfo> all() {
        return subjects;
    }

    public Optional<SubjectInfo> byId(Long id) {
        return id == null ? Optional.empty() : Optional.ofNullable(byId.get(id));
    }

    public Optional<SubjectInfo> byCode(String code) {
        return code == null ? Optional.empty() : Optional.ofNullable(byCode.get(code.trim().toUpperCase(Locale.ROOT)));
    }

    public SubjectRef ref(long subjectId) {
        return byId(subjectId).map(SubjectInfo::ref).orElseThrow();
    }

    public Optional<TopicRef> topic(Long topicId) {
        return topicId == null ? Optional.empty() : Optional.ofNullable(topicById.get(topicId));
    }

    public Optional<Long> subjectOfTopic(Long topicId) {
        return topicId == null ? Optional.empty() : Optional.ofNullable(topicSubject.get(topicId));
    }

    public Optional<TopicRef> topicByName(long subjectId, String name) {
        return name == null ? Optional.empty() : Optional.ofNullable(topicByKey.get(topicKey(subjectId, name)));
    }

    public TopicRef general(long subjectId) {
        return topicByName(subjectId, GENERAL).orElseThrow();
    }

    /** code -> ordered topic names, as sent to the model. */
    public Map<String, List<String>> topicNamesByCode() {
        Map<String, List<String>> map = new LinkedHashMap<>();
        subjects.forEach(s -> map.put(s.code(), s.topics().stream().map(TopicRef::name).toList()));
        return map;
    }

    private static String topicKey(long subjectId, String name) {
        return subjectId + "|" + name.trim().toLowerCase(Locale.ROOT);
    }
}
