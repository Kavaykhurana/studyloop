package com.studyloop.seed;

import com.studyloop.ai.AiUnavailableException;
import com.studyloop.ai.EmbeddingClient;
import com.studyloop.config.AppProperties;
import com.studyloop.doubt.Doubt;
import com.studyloop.doubt.DoubtRepository;
import com.studyloop.doubt.DoubtStatus;
import com.studyloop.embedding.EmbeddingStore;
import com.studyloop.embedding.EmbeddingText;
import com.studyloop.matching.PeerMatchingService;
import com.studyloop.seed.SeedFile.SeedAnswer;
import com.studyloop.seed.SeedFile.SeedDoubt;
import com.studyloop.subject.SubjectCatalog;
import com.studyloop.subject.SubjectInfo;
import com.studyloop.user.UserRepository;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

/** Seeds the TIET demo campus: 25 personas plus 60 doubts and 75 answers of real study content. */
@Component
public class DemoSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoSeeder.class);
    private static final long TIET = 1L;
    private static final int EMBED_CHUNK = 50;

    public record Report(int users, int doubts, int answers, int embedded) {}

    private record Seeded(long id, SeedDoubt source, Instant createdAt) {}

    private record Event(Instant at, String type, long userId, long doubtId, Long actorId, String message) {}

    private final AppProperties props;
    private final UserRepository users;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    private final JsonMapper mapper;
    private final SubjectCatalog catalog;
    private final EmbeddingClient embeddings;
    private final EmbeddingStore embeddingStore;
    private final DoubtRepository doubts;
    private final PeerMatchingService matching;
    private final Clock clock;

    public DemoSeeder(AppProperties props, UserRepository users, JdbcTemplate jdbc, TransactionTemplate tx,
                      JsonMapper mapper, SubjectCatalog catalog, EmbeddingClient embeddings,
                      EmbeddingStore embeddingStore, DoubtRepository doubts, PeerMatchingService matching,
                      Clock clock) {
        this.props = props;
        this.users = users;
        this.jdbc = jdbc;
        this.tx = tx;
        this.mapper = mapper;
        this.catalog = catalog;
        this.embeddings = embeddings;
        this.embeddingStore = embeddingStore;
        this.doubts = doubts;
        this.matching = matching;
        this.clock = clock;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (props.demo().seed() && !users.existsByDemoTrue()) {
            seed();
        }
    }

    public synchronized Report seed() {
        Instant now = clock.instant();
        SeedFile file = load();
        Map<String, Long> userIds = new LinkedHashMap<>();
        List<Seeded> seeded = new ArrayList<>();
        int[] answerCount = {0};
        tx.executeWithoutResult(status -> {
            insertUsers(now, userIds);
            insertContent(file, now, userIds, seeded, answerCount);
            recomputeReputation();
            insertDeflections(seeded, userIds, now);
            insertNotifications(file, seeded, userIds, now);
        });
        int embedded = embedAll(seeded);
        for (Seeded s : seeded) {
            if ("OPEN".equals(s.source().status())) {
                try {
                    doubts.findById(s.id()).ifPresent(matching::matchWithoutNotifying);
                } catch (RuntimeException e) {
                    log.warn("Initial matching skipped for seeded doubt {}: {}", s.id(), e.getMessage());
                }
            }
        }
        Report report = new Report(userIds.size(), seeded.size(), answerCount[0], embedded);
        log.info("Seeded {} users, {} doubts, {} answers; embedded {}/{}", report.users(), report.doubts(),
                report.answers(), report.embedded(), report.doubts());
        return report;
    }

    private SeedFile load() {
        try (InputStream in = new ClassPathResource("seed/demo-doubts.json").getInputStream()) {
            return mapper.readValue(in, SeedFile.class);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read seed/demo-doubts.json", e);
        }
    }

    private void insertUsers(Instant now, Map<String, Long> userIds) {
        int index = 0;
        for (SeedCatalog.Persona p : SeedCatalog.PERSONAS) {
            Instant lastActive = lastActive(p, index++, now);
            Long id = jdbc.queryForObject("""
                    INSERT INTO users (campus_id, name, email, password_hash, role, year_of_study, branch, bio,
                                       reputation, is_demo, onboarded, last_active_at, created_at)
                    VALUES (?, ?, ?, NULL, 'STUDENT', ?, ?, ?, 0, true, true, ?, ?) RETURNING id""", Long.class,
                    TIET, p.name(), p.email(), p.year(), p.branch(), p.bio(), Timestamp.from(lastActive),
                    Timestamp.from(now.minus(Duration.ofDays(21))));
            userIds.put(p.name(), id);
            for (SeedCatalog.Entry e : p.subjects()) {
                SubjectInfo subject = catalog.byCode(e.code()).orElseThrow();
                jdbc.update("INSERT INTO user_subjects (user_id, subject_id, kind, level) VALUES (?, ?, ?, ?)", id,
                        subject.id(), e.kind(), e.level());
            }
        }
    }

    /** Recent helpers within 12 hours, other senior helpers within 6 days, learners within 3 days. */
    private static Instant lastActive(SeedCatalog.Persona p, int index, Instant now) {
        Integer recent = SeedCatalog.RECENT_HELPERS_HOURS.get(p.name());
        if (recent != null) {
            return now.minus(Duration.ofHours(recent));
        }
        boolean helper = p.subjects().stream().anyMatch(s -> "STRONG".equals(s.kind())) && p.year() >= 3;
        long hours = helper ? 14 + (index * 37L) % 130 : 4 + (index * 23L) % 66;
        return now.minus(Duration.ofHours(hours));
    }

    private void insertContent(SeedFile file, Instant now, Map<String, Long> userIds, List<Seeded> seeded,
                               int[] answerCount) {
        for (SeedDoubt d : file.doubts()) {
            SubjectInfo subject = catalog.byCode(d.subject()).orElseThrow();
            long topicId = catalog.topicByName(subject.id(), d.topic()).orElseThrow().id();
            Instant created = now.minus(Duration.ofSeconds((long) (d.ageHours() * 3600)));
            boolean solved = "SOLVED".equals(d.status());
            Instant solvedAt = solved ? created.plus(Duration.ofMinutes(d.solvedAfterMinutes())) : null;
            Instant firstAnswer = d.answers().stream().map(a -> created.plus(Duration.ofMinutes(a.afterMinutes())))
                    .min(Comparator.naturalOrder()).orElse(null);
            Long doubtId = jdbc.queryForObject("""
                    INSERT INTO doubts (campus_id, author_id, subject_id, topic_id, title, body_md, difficulty,
                                        status, resolution, source, answer_count, view_count, created_at,
                                        first_answer_at, solved_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'TEXT', ?, ?, ?, ?, ?) RETURNING id""", Long.class,
                    TIET, userIds.get(d.author()), subject.id(), topicId, d.title(), d.body(), d.difficulty(),
                    d.status(), solved ? d.resolution() : null, d.answers().size(), d.views(),
                    Timestamp.from(created), firstAnswer == null ? null : Timestamp.from(firstAnswer),
                    solvedAt == null ? null : Timestamp.from(solvedAt));
            seeded.add(new Seeded(doubtId, d, created));
            for (SeedAnswer a : d.answers()) {
                Instant answerAt = created.plus(Duration.ofMinutes(a.afterMinutes()));
                Instant acceptedAt = a.accepted() ? solvedAt : null;
                Long answerId = jdbc.queryForObject("""
                        INSERT INTO answers (doubt_id, author_id, body_md, is_accepted, helpful_rating, created_at,
                                             accepted_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id""", Long.class, doubtId, userIds.get(a.author()),
                        a.body(), a.accepted(), a.accepted() ? a.rating() : null, Timestamp.from(answerAt),
                        acceptedAt == null ? null : Timestamp.from(acceptedAt));
                answerCount[0]++;
                if (a.accepted()) {
                    ledger(userIds.get(a.author()), subject.id(), 15, "ANSWER_ACCEPTED", answerId, acceptedAt);
                    if (Duration.between(created, answerAt).toMinutes() <= 30) {
                        ledger(userIds.get(a.author()), subject.id(), 5, "FAST_ANSWER", answerId, acceptedAt);
                    }
                    ledger(userIds.get(d.author()), subject.id(), 2, "LOOP_CLOSED", answerId, acceptedAt);
                    if (a.rating() != null && a.rating() >= 4) {
                        ledger(userIds.get(a.author()), subject.id(), 5, "HELPFUL_RATING", answerId, acceptedAt);
                    }
                }
            }
            if (d.hints() != null && !d.hints().isEmpty()) {
                int top = 0;
                for (SeedFile.SeedHint h : d.hints()) {
                    jdbc.update("""
                            INSERT INTO ai_hints (doubt_id, level, content_md, cited_doubt_ids, confidence, model,
                                                  created_at)
                            VALUES (?, ?, ?, '{}', 'HIGH', 'seed', ?)""", doubtId, h.level(), h.content(),
                            Timestamp.from(created.plus(Duration.ofMinutes(3))));
                    top = Math.max(top, h.level());
                }
                jdbc.update("""
                        INSERT INTO hint_progress (user_id, doubt_id, level_unlocked, updated_at)
                        VALUES (?, ?, ?, ?)""", userIds.get(d.author()), doubtId, top,
                        Timestamp.from(created.plus(Duration.ofMinutes(4))));
            }
        }
    }

    private void ledger(long userId, long subjectId, int delta, String reason, long answerId, Instant at) {
        jdbc.update("""
                INSERT INTO reputation_events (user_id, subject_id, delta, reason, source_answer_id, created_at)
                VALUES (?, ?, ?, ?, ?, ?) ON CONFLICT (user_id, reason, source_answer_id) DO NOTHING""", userId,
                subjectId, delta, reason, answerId, Timestamp.from(at));
    }

    /** Reputation is always derived from the ledger, never hard-coded. */
    private void recomputeReputation() {
        jdbc.update("""
                UPDATE users u SET reputation =
                    COALESCE((SELECT SUM(e.delta) FROM reputation_events e WHERE e.user_id = u.id), 0)
                WHERE u.campus_id = ?""", TIET);
    }

    private void insertDeflections(List<Seeded> seeded, Map<String, Long> userIds, Instant now) {
        List<Seeded> solvedByPeers = seeded.stream()
                .filter(s -> "PEER_ANSWER".equals(s.source().resolution())).toList();
        List<Long> learners = SeedCatalog.PERSONAS.stream().filter(p -> p.year() <= 2)
                .map(p -> userIds.get(p.name())).toList();
        for (int i = 0; i < 30; i++) {
            Seeded source = solvedByPeers.get(i % solvedByPeers.size());
            long user = learners.get((i * 5 + i / solvedByPeers.size()) % learners.size());
            double similarity = 0.80 + 0.15 * ((i * 37) % 100) / 100.0;
            Instant at = now.minus(Duration.ofMinutes(25L + i * 640L / 2 % 19000));
            jdbc.update("""
                    INSERT INTO deflection_events (campus_id, user_id, source_doubt_id, query_text, similarity,
                                                   created_at)
                    VALUES (?, ?, ?, ?, ?, ?) ON CONFLICT (user_id, source_doubt_id) DO NOTHING""", TIET, user,
                    source.id(), source.source().title(), (float) similarity, Timestamp.from(at));
        }
    }

    private void insertNotifications(SeedFile file, List<Seeded> seeded, Map<String, Long> userIds, Instant now) {
        long aanya = userIds.get(SeedCatalog.LEARNER);
        long rohan = userIds.get(SeedCatalog.HELPER);
        List<Event> aanyaEvents = new ArrayList<>();
        List<Event> rohanEvents = new ArrayList<>();
        for (Seeded s : seeded) {
            SeedDoubt d = s.source();
            long authorId = userIds.get(d.author());
            String authorFirst = first(d.author());
            String topic = d.topic();
            for (SeedAnswer a : d.answers()) {
                Instant answerAt = s.createdAt().plus(Duration.ofMinutes(a.afterMinutes()));
                String title = d.title().length() <= 60 ? d.title() : d.title().substring(0, 60);
                if (authorId == aanya) {
                    aanyaEvents.add(new Event(answerAt, "NEW_ANSWER", aanya, s.id(), userIds.get(a.author()),
                            first(a.author()) + " answered your doubt \"" + title + "\""));
                }
                if (a.accepted()) {
                    Instant acceptedAt = s.createdAt().plus(Duration.ofMinutes(d.solvedAfterMinutes()));
                    int points = 15 + (a.afterMinutes() <= 30 ? 5 : 0) + (a.rating() != null && a.rating() >= 4
                            ? 5 : 0);
                    Event accepted = new Event(acceptedAt, "ANSWER_ACCEPTED", userIds.get(a.author()), s.id(),
                            authorId, authorFirst + " accepted your answer. +" + points + " reputation");
                    if (userIds.get(a.author()) == aanya) {
                        aanyaEvents.add(accepted);
                    } else if (userIds.get(a.author()) == rohan) {
                        rohanEvents.add(accepted);
                    }
                }
            }
            SubjectInfo subject = catalog.byCode(d.subject()).orElseThrow();
            Event matched = new Event(s.createdAt().plus(Duration.ofMinutes(2)), "MATCHED", 0, s.id(), authorId,
                    authorFirst + " needs help with " + topic + " (" + subject.code() + ")");
            if ("MATH".equals(d.subject()) && authorId != aanya) {
                aanyaEvents.add(new Event(matched.at(), matched.type(), aanya, s.id(), authorId, matched.message()));
            }
            boolean rohanSubject = "DSA".equals(d.subject()) || "DBMS".equals(d.subject());
            if (rohanSubject && "OPEN".equals(d.status()) && authorId != rohan) {
                rohanEvents.add(new Event(matched.at(), matched.type(), rohan, s.id(), authorId, matched.message()));
            }
        }
        writeNotifications(aanyaEvents, 12, "MATCHED", 0);
        writeNotifications(rohanEvents, 10, "MATCHED", 4);
    }

    /** Keeps the newest {@code total} events, capping matched notifications, and leaves the last three unread. */
    private void writeNotifications(List<Event> events, int total, String capType, int capCount) {
        List<Event> sorted = events.stream().sorted(Comparator.comparing(Event::at).reversed()).toList();
        List<Event> chosen = new ArrayList<>();
        int matched = 0;
        for (Event e : sorted) {
            if (chosen.size() >= total) {
                break;
            }
            if (capCount > 0 && capType.equals(e.type())) {
                if (matched >= capCount) {
                    continue;
                }
                matched++;
            }
            chosen.add(e);
        }
        for (int i = 0; i < chosen.size(); i++) {
            Event e = chosen.get(i);
            boolean unread = i < 3;
            jdbc.update("""
                    INSERT INTO notifications (user_id, type, doubt_id, actor_id, message, read_at, created_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?)""", e.userId(), e.type(), e.doubtId(), e.actorId(), e.message(),
                    unread ? null : Timestamp.from(e.at().plus(Duration.ofHours(1))), Timestamp.from(e.at()));
        }
    }

    private int embedAll(List<Seeded> seeded) {
        Map<Long, String> texts = new HashMap<>();
        for (Seeded s : seeded) {
            SeedDoubt d = s.source();
            SubjectInfo subject = catalog.byCode(d.subject()).orElseThrow();
            texts.put(s.id(), EmbeddingText.forDoubt(d.title(), subject.name(), d.topic(), d.body()));
        }
        int embedded = 0;
        for (int from = 0; from < seeded.size(); from += EMBED_CHUNK) {
            List<Seeded> chunk = seeded.subList(from, Math.min(seeded.size(), from + EMBED_CHUNK));
            try {
                List<float[]> vectors = embeddings.embedBatch(chunk.stream().map(s -> texts.get(s.id())).toList());
                for (int i = 0; i < chunk.size(); i++) {
                    embeddingStore.saveDoubtEmbedding(chunk.get(i).id(), vectors.get(i));
                    embedded++;
                }
            } catch (AiUnavailableException e) {
                log.warn("Seed embeddings skipped for a chunk ({}); the backfill job will retry", e.getCode());
            }
            if (from + EMBED_CHUNK < seeded.size() && props.ai().enabled()) {
                sleep(1000);
            }
        }
        return embedded;
    }

    private static String first(String name) {
        int space = name.indexOf(' ');
        return space < 0 ? name : name.substring(0, space);
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
