package com.studyloop.embedding;

import com.studyloop.ai.EmbeddingClient;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** All pgvector SQL lives here; embeddings are never mapped through JPA. */
@Component
public class EmbeddingStore {

    private static final String SIMILAR_SOLVED = """
            SELECT d.id, d.title, d.body_md, d.subject_id, d.topic_id, d.solved_at,
                   1 - (d.embedding <=> CAST(? AS vector)) AS similarity,
                   a.id AS answer_id, a.body_md AS answer_body, u.name AS answer_author
            FROM doubts d
            JOIN answers a ON a.doubt_id = d.id AND a.is_accepted
            JOIN users u   ON u.id = a.author_id
            WHERE d.campus_id = ? AND d.status = 'SOLVED' AND d.embedding IS NOT NULL AND d.id <> ?
            ORDER BY d.embedding <=> CAST(? AS vector)
            LIMIT ?""";

    private final JdbcTemplate jdbc;
    private final EmbeddingClient embeddings;

    public EmbeddingStore(JdbcTemplate jdbc, EmbeddingClient embeddings) {
        this.jdbc = jdbc;
        this.embeddings = embeddings;
    }

    /** Stores the vector together with the id of the model that produced it. */
    public void saveDoubtEmbedding(long doubtId, float[] vector) {
        jdbc.update("UPDATE doubts SET embedding = CAST(? AS vector), embedding_model = ? WHERE id = ?",
                VectorMath.toLiteral(vector), embeddings.modelId(), doubtId);
    }

    /**
     * Clears vectors produced by any model other than the current one: they live in a different vector space, so
     * comparing them with new query vectors would return meaningless similarities. Returns how many were cleared; the
     * cleared rows are then re-embedded like any other missing embedding.
     */
    public int clearForeignEmbeddings() {
        return jdbc.update("""
                UPDATE doubts SET embedding = NULL, embedding_model = NULL
                WHERE embedding IS NOT NULL AND embedding_model IS DISTINCT FROM ?""", embeddings.modelId());
    }

    public int countMissingEmbeddings() {
        Integer n = jdbc.queryForObject("SELECT count(*) FROM doubts WHERE embedding IS NULL", Integer.class);
        return n == null ? 0 : n;
    }

    /** The stored vector as a pgvector literal, ready to be passed back into similarity queries. */
    public Optional<String> getDoubtEmbedding(long doubtId) {
        List<String> rows = jdbc.query("SELECT embedding::text FROM doubts WHERE id = ? AND embedding IS NOT NULL",
                (rs, i) -> rs.getString(1), doubtId);
        return rows.stream().findFirst();
    }

    public List<Long> findNullEmbeddingIds(int limit) {
        return jdbc.queryForList("SELECT id FROM doubts WHERE embedding IS NULL ORDER BY id LIMIT ?", Long.class,
                limit);
    }

    /** Solved doubts with a peer-accepted answer, nearest first. Pass 0 to exclude nothing. */
    public List<SimilarSolved> findSimilarSolved(long campusId, String vectorLiteral, int limit,
                                                 long excludeDoubtId) {
        return jdbc.query(SIMILAR_SOLVED, (rs, i) -> new SimilarSolved(rs.getLong("id"), rs.getString("title"),
                rs.getString("body_md"), rs.getLong("subject_id"), (Long) rs.getObject("topic_id"),
                rs.getTimestamp("solved_at").toInstant(), rs.getDouble("similarity"), rs.getLong("answer_id"),
                rs.getString("answer_body"), rs.getString("answer_author")),
                vectorLiteral, campusId, excludeDoubtId, vectorLiteral, limit);
    }
}
