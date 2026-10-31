package com.studyloop.support;

import com.studyloop.ai.EmbeddingClient;
import com.studyloop.auth.TokenService;
import com.studyloop.common.web.CurrentUser;
import com.studyloop.embedding.EmbeddingStore;
import com.studyloop.embedding.EmbeddingText;
import com.studyloop.subject.SubjectCatalog;
import com.studyloop.subject.SubjectInfo;
import com.studyloop.user.User;
import com.studyloop.user.UserRepository;
import com.studyloop.user.UserSubject;
import com.studyloop.user.UserSubjectRepository;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Direct-to-database builders, so tests set up exact states without going through the full pipelines. */
@Component
public class Fixtures {

    public static final long TIET = 1L;
    public static final long OPEN_CAMPUS = 2L;

    private final UserRepository users;
    private final UserSubjectRepository userSubjects;
    private final JdbcTemplate jdbc;
    private final SubjectCatalog catalog;
    private final TokenService tokens;
    private final EmbeddingClient embeddings;
    private final EmbeddingStore store;
    private final Clock clock;

    public Fixtures(UserRepository users, UserSubjectRepository userSubjects, JdbcTemplate jdbc,
                    SubjectCatalog catalog, TokenService tokens, EmbeddingClient embeddings, EmbeddingStore store,
                    Clock clock) {
        this.users = users;
        this.userSubjects = userSubjects;
        this.jdbc = jdbc;
        this.catalog = catalog;
        this.tokens = tokens;
        this.embeddings = embeddings;
        this.store = store;
        this.clock = clock;
    }

    public User user(String name, long campusId, int year) {
        User u = new User();
        u.setCampusId(campusId);
        u.setName(name);
        u.setEmail(name.toLowerCase().replace(' ', '.') + "@test.studyloop.app");
        u.setYearOfStudy(year);
        u.setDemo(true);
        u.setOnboarded(true);
        u.setCreatedAt(clock.instant());
        u.setLastActiveAt(clock.instant());
        return users.saveAndFlush(u);
    }

    public User tiet(String name) {
        return user(name, TIET, 2);
    }

    public User strongHelper(String name, String subjectCode, int level) {
        User u = user(name, TIET, 4);
        userSubjects.save(new UserSubject(u.getId(), catalog.byCode(subjectCode).orElseThrow().id(),
                UserSubject.Kind.STRONG, level));
        return u;
    }

    public void touch(User u, Duration ago) {
        jdbc.update("UPDATE users SET last_active_at = ? WHERE id = ?",
                Timestamp.from(clock.instant().minus(ago)), u.getId());
    }

    public CurrentUser as(User u) {
        return new CurrentUser(u.getId(), u.getCampusId(), u.getRole(), u.isDemo());
    }

    public String bearer(User u) {
        return "Bearer " + tokens.issueAccessToken(u).token();
    }

    /** Inserts an OPEN doubt with an embedding; returns its id. */
    public long doubt(User author, String subjectCode, String topic, String title, String body) {
        return doubtAt(author, subjectCode, topic, title, body, clock.instant());
    }

    public long doubtAt(User author, String subjectCode, String topic, String title, String body, Instant createdAt) {
        SubjectInfo subject = catalog.byCode(subjectCode).orElseThrow();
        long topicId = catalog.topicByName(subject.id(), topic).orElseThrow().id();
        Long id = jdbc.queryForObject("""
                INSERT INTO doubts (campus_id, author_id, subject_id, topic_id, title, body_md, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id""", Long.class, author.getCampusId(), author.getId(),
                subject.id(), topicId, title, body, Timestamp.from(createdAt));
        store.saveDoubtEmbedding(id, embeddings.embed(EmbeddingText.forDoubt(title, subject.name(), topic, body)));
        return id;
    }

    public long answer(long doubtId, User author, String body, Instant at) {
        Long id = jdbc.queryForObject("""
                INSERT INTO answers (doubt_id, author_id, body_md, created_at) VALUES (?, ?, ?, ?) RETURNING id""",
                Long.class, doubtId, author.getId(), body, Timestamp.from(at));
        jdbc.update("""
                UPDATE doubts SET answer_count = answer_count + 1, first_answer_at = COALESCE(first_answer_at, ?)
                WHERE id = ?""", Timestamp.from(at), doubtId);
        return id;
    }

    /** A doubt that was solved by an accepted peer answer, with the ledger row the real flow would write. */
    public long solvedDoubt(User author, User helper, String subjectCode, String topic, String title, String body,
                            String answerBody) {
        Instant created = clock.instant().minus(Duration.ofDays(5));
        long doubtId = doubtAt(author, subjectCode, topic, title, body, created);
        long answerId = answer(doubtId, helper, answerBody, created.plus(Duration.ofMinutes(20)));
        Instant solved = created.plus(Duration.ofHours(1));
        jdbc.update("UPDATE answers SET is_accepted = true, accepted_at = ? WHERE id = ?", Timestamp.from(solved),
                answerId);
        jdbc.update("UPDATE doubts SET status = 'SOLVED', resolution = 'PEER_ANSWER', solved_at = ? WHERE id = ?",
                Timestamp.from(solved), doubtId);
        long subjectId = catalog.byCode(subjectCode).orElseThrow().id();
        jdbc.update("""
                INSERT INTO reputation_events (user_id, subject_id, delta, reason, source_answer_id, created_at)
                VALUES (?, ?, 15, 'ANSWER_ACCEPTED', ?, ?)""", helper.getId(), subjectId, answerId,
                Timestamp.from(solved));
        jdbc.update("UPDATE users SET reputation = reputation + 15 WHERE id = ?", helper.getId());
        return doubtId;
    }

    public int reputation(User u) {
        Integer r = jdbc.queryForObject("SELECT reputation FROM users WHERE id = ?", Integer.class, u.getId());
        return r == null ? 0 : r;
    }

    public long subjectId(String code) {
        return catalog.byCode(code).orElseThrow().id();
    }
}
