package com.studyloop.embedding;

import com.studyloop.ai.AiUnavailableException;
import com.studyloop.ai.EmbeddingClient;
import com.studyloop.subject.SubjectCatalog;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Fills in embeddings that could not be computed at post time because the AI was unavailable. */
@Component
public class EmbeddingBackfillJob {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingBackfillJob.class);
    private static final int BATCH = 20;

    private final EmbeddingStore store;
    private final EmbeddingClient embeddings;
    private final JdbcTemplate jdbc;
    private final SubjectCatalog catalog;

    public EmbeddingBackfillJob(EmbeddingStore store, EmbeddingClient embeddings, JdbcTemplate jdbc,
                                SubjectCatalog catalog) {
        this.store = store;
        this.embeddings = embeddings;
        this.jdbc = jdbc;
        this.catalog = catalog;
    }

    @Scheduled(initialDelay = 30_000, fixedDelay = 120_000)
    public void run() {
        runOnce();
    }

    /** Embeds up to 20 doubts that have no vector; returns how many were stored. */
    public int runOnce() {
        List<Long> ids = store.findNullEmbeddingIds(BATCH);
        if (ids.isEmpty()) {
            return 0;
        }
        List<String> texts = new ArrayList<>();
        for (Long id : ids) {
            texts.add(jdbc.queryForObject("SELECT title, subject_id, topic_id, body_md FROM doubts WHERE id = ?",
                    (rs, i) -> EmbeddingText.forDoubt(rs.getString("title"),
                            catalog.byId(rs.getLong("subject_id")).orElseThrow().name(),
                            catalog.topic((Long) rs.getObject("topic_id")).map(t -> t.name())
                                    .orElse(SubjectCatalog.GENERAL),
                            rs.getString("body_md")), id));
        }
        try {
            List<float[]> vectors = embeddings.embedBatch(texts);
            for (int i = 0; i < ids.size(); i++) {
                store.saveDoubtEmbedding(ids.get(i), vectors.get(i));
            }
            log.info("Backfilled {} embeddings", ids.size());
            return ids.size();
        } catch (AiUnavailableException e) {
            log.warn("Embedding backfill skipped: {}", e.getCode());
            return 0;
        }
    }
}
