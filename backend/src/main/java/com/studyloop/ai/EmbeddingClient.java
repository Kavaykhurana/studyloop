package com.studyloop.ai;

import java.util.List;

public interface EmbeddingClient {

    /** Returns an L2-normalised 768-d vector. */
    float[] embed(String text);

    /** Embeds up to 50 texts in one call. */
    List<float[]> embedBatch(List<String> texts);
}
