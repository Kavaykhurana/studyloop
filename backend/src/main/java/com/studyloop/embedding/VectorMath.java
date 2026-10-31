package com.studyloop.embedding;

public final class VectorMath {

    private VectorMath() {}

    /** Returns a new L2-normalised copy; an all-zero vector becomes the unit vector at index 0. */
    public static float[] normalize(float[] v) {
        double sum = 0;
        for (float x : v) {
            sum += (double) x * x;
        }
        float[] out = new float[v.length];
        if (sum == 0) {
            out[0] = 1f;
            return out;
        }
        double norm = Math.sqrt(sum);
        for (int i = 0; i < v.length; i++) {
            out[i] = (float) (v[i] / norm);
        }
        return out;
    }

    public static double cosine(float[] a, float[] b) {
        double dot = 0;
        double na = 0;
        double nb = 0;
        for (int i = 0; i < a.length; i++) {
            dot += (double) a[i] * b[i];
            na += (double) a[i] * a[i];
            nb += (double) b[i] * b[i];
        }
        return na == 0 || nb == 0 ? 0 : dot / (Math.sqrt(na) * Math.sqrt(nb));
    }

    /** pgvector text literal: [v1,v2,...]. */
    public static String toLiteral(float[] v) {
        StringBuilder sb = new StringBuilder(v.length * 10).append('[');
        for (int i = 0; i < v.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(Float.toString(v[i]));
        }
        return sb.append(']').toString();
    }
}
