package com.studyloop.doubt;

import com.studyloop.ai.AiUnavailableException;
import com.studyloop.ai.EmbeddingClient;
import com.studyloop.common.error.ApiException;
import com.studyloop.common.error.ErrorCode;
import com.studyloop.common.ratelimit.RateLimitKey;
import com.studyloop.common.ratelimit.RateLimiter;
import com.studyloop.common.text.ContentGuard;
import com.studyloop.common.text.PlainText;
import com.studyloop.common.web.CurrentUser;
import com.studyloop.config.AppProperties;
import com.studyloop.doubt.dto.DeflectionRequest;
import com.studyloop.doubt.dto.SimilarRequest;
import com.studyloop.doubt.dto.SimilarResponse;
import com.studyloop.embedding.EmbeddingStore;
import com.studyloop.embedding.EmbeddingText;
import com.studyloop.embedding.SimilarSolved;
import com.studyloop.embedding.VectorMath;
import com.studyloop.subject.SubjectCatalog;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class DuplicateRadarService {

    private static final Logger log = LoggerFactory.getLogger(DuplicateRadarService.class);
    private static final int CANDIDATES = 8;
    private static final int MAX_RESULTS = 3;
    private static final int EXCERPT_CHARS = 280;

    private final EmbeddingClient embeddings;
    private final EmbeddingStore store;
    private final SubjectCatalog catalog;
    private final RateLimiter limiter;
    private final AppProperties props;
    private final DoubtRepository doubts;
    private final JdbcTemplate jdbc;
    private final Clock clock;

    public DuplicateRadarService(EmbeddingClient embeddings, EmbeddingStore store, SubjectCatalog catalog,
                                 RateLimiter limiter, AppProperties props, DoubtRepository doubts, JdbcTemplate jdbc,
                                 Clock clock) {
        this.embeddings = embeddings;
        this.store = store;
        this.catalog = catalog;
        this.limiter = limiter;
        this.props = props;
        this.doubts = doubts;
        this.jdbc = jdbc;
        this.clock = clock;
    }

    public SimilarResponse similar(CurrentUser me, SimilarRequest req) {
        limiter.check(RateLimitKey.SIMILAR, String.valueOf(me.userId()));
        String title = ContentGuard.title(req.title());
        String body = ContentGuard.text(req.bodyMd());
        ContentGuard.rejectUnsafe(title, body);
        float[] vector;
        try {
            vector = embeddings.embed(EmbeddingText.forQuery(title, body));
        } catch (AiUnavailableException e) {
            log.warn("Duplicate radar skipped: {}", e.getCode());
            return new SimilarResponse(List.of());
        }
        List<SimilarSolved> hits = store.findSimilarSolved(me.campusId(), VectorMath.toLiteral(vector), CANDIDATES, 0);
        double threshold = props.similarity().duplicateThreshold();
        List<SimilarResponse.Item> items = hits.stream()
                .filter(h -> h.similarity() >= threshold)
                .limit(MAX_RESULTS)
                .map(h -> new SimilarResponse.Item(h.doubtId(), h.title(), catalog.ref(h.subjectId()),
                        catalog.topic(h.topicId()).orElse(null), Math.round(h.similarity() * 10_000d) / 10_000d,
                        PlainText.excerpt(h.answerBody(), EXCERPT_CHARS), h.answerAuthor(), h.solvedAt()))
                .toList();
        return new SimilarResponse(items);
    }

    /** Records that the radar spared the student a new post. One event per (user, source doubt). */
    public void recordDeflection(CurrentUser me, DeflectionRequest req) {
        Doubt source = doubts.findByIdAndCampusId(req.sourceDoubtId(), me.campusId())
                .filter(d -> d.getStatus() == DoubtStatus.SOLVED)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        String query = ContentGuard.text(req.queryText());
        ContentGuard.rejectUnsafe(query);
        double similarity = Math.max(0d, Math.min(1d, req.similarity()));
        jdbc.update("""
                INSERT INTO deflection_events (campus_id, user_id, source_doubt_id, query_text, similarity, created_at)
                VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT (user_id, source_doubt_id) DO NOTHING""",
                me.campusId(), me.userId(), source.getId(), query.length() <= 500 ? query : query.substring(0, 500),
                (float) similarity, Timestamp.from(clock.instant()));
    }
}
