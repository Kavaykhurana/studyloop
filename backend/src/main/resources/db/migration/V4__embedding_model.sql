-- Which embedding model produced doubts.embedding. Vectors from different models live in different spaces, so on
-- startup any vector whose model differs from the configured one is cleared and re-embedded (EmbeddingSpaceGuard).
ALTER TABLE doubts ADD COLUMN embedding_model VARCHAR(80);

CREATE INDEX ix_doubts_embedding_model ON doubts (embedding_model) WHERE embedding IS NOT NULL;
