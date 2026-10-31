package com.studyloop.embedding;

import com.studyloop.ai.EmbeddingClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

/**
 * Keeps every stored vector in the vector space of the configured embedding model. Runs at startup after the demo
 * seeder: vectors written by another model (for example the offline fake before GEMINI_API_KEY was set, or a renamed
 * Gemini model) are cleared, then re-embedded right away in batches. If the AI is unavailable the rows stay NULL, are
 * excluded from similarity search, and {@link EmbeddingBackfillJob} fills them in later.
 */
@Component
public class EmbeddingSpaceGuard implements ApplicationRunner, Ordered {

    static final int ORDER = 20;
    private static final Logger log = LoggerFactory.getLogger(EmbeddingSpaceGuard.class);
    private static final int BATCH = 50;
    private static final int MAX_BATCHES = 40;

    private final EmbeddingStore store;
    private final EmbeddingBackfillJob backfill;
    private final EmbeddingClient embeddings;
    private volatile boolean ready;

    public EmbeddingSpaceGuard(EmbeddingStore store, EmbeddingBackfillJob backfill, EmbeddingClient embeddings) {
        this.store = store;
        this.backfill = backfill;
        this.embeddings = embeddings;
    }

    @Override
    public int getOrder() {
        return ORDER;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            int cleared = store.clearForeignEmbeddings();
            if (cleared > 0) {
                log.warn("Cleared {} embeddings from a different model; re-embedding with {}", cleared,
                        embeddings.modelId());
            }
            int missing = store.countMissingEmbeddings();
            int embedded = 0;
            for (int i = 0; i < MAX_BATCHES && missing > 0; i++) {
                int n = backfill.runOnce(BATCH);
                if (n == 0) {
                    break;
                }
                embedded += n;
                missing -= n;
            }
            if (embedded > 0) {
                log.info("Embedded {} doubts at startup with {}", embedded, embeddings.modelId());
            }
        } finally {
            ready = true;
        }
    }

    public boolean isReady() {
        return ready;
    }
}
