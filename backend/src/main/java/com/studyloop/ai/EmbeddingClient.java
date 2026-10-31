package com.studyloop.ai;

import java.util.List;

public interface EmbeddingClient {

    /** Returns an L2-normalised 768-d vector. */
    float[] embed(String text);

    /**
     * Identifies the vector space this client produces. Stored next to every embedding so vectors from a different
     * model (for example the offline fake, before a Gemini key was added) are never compared with new ones.
     */
    String modelId();

    /** Embeds up to 50 texts in one call. */
    List<float[]> embedBatch(List<String> texts);
}
