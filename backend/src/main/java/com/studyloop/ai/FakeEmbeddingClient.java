package com.studyloop.ai;

import com.studyloop.embedding.VectorMath;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Deterministic bag-of-words hashing embedder for dev, offline use and tests. */
public class FakeEmbeddingClient implements EmbeddingClient {

    static final int DIMENSIONS = 768;

    @Override
    public float[] embed(String text) {
        float[] v = new float[DIMENSIONS];
        if (text != null) {
            for (String token : text.toLowerCase(Locale.ROOT).split("[^a-z0-9]+")) {
                if (token.length() < 3) {
                    continue;
                }
                v[bucket(token)] += 1.0f;
                v[bucket(token + "#")] += 0.5f;
            }
        }
        return VectorMath.normalize(v);
    }

    @Override
    public List<float[]> embedBatch(List<String> texts) {
        List<float[]> out = new ArrayList<>(texts.size());
        texts.forEach(t -> out.add(embed(t)));
        return out;
    }

    private static int bucket(String token) {
        int h = token.hashCode();
        return (h == Integer.MIN_VALUE ? 0 : Math.abs(h)) % DIMENSIONS;
    }
}
