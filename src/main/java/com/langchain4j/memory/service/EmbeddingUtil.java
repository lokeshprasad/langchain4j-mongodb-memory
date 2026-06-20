package com.langchain4j.memory.service;

import java.util.Arrays;

public final class EmbeddingUtil {

    private static final int DIM = 256;

    private EmbeddingUtil() {}

    // Simple hashing vectorizer: hashing trick over tokens
    public static double[] embed(String text) {
        double[] vec = new double[DIM];
        if (text == null || text.isBlank()) return vec;

        String[] tokens = text.toLowerCase().replaceAll("[^a-z0-9\\s]"," ").split("\\s+");
        for (String t : tokens) {
            if (t.isEmpty()) continue;
            int idx = Math.abs(t.hashCode()) % DIM;
            vec[idx] += 1.0;
        }

        // L2 normalize
        double sum = 0.0;
        for (double v : vec) sum += v * v;
        if (sum > 0) {
            double norm = Math.sqrt(sum);
            for (int i = 0; i < vec.length; i++) vec[i] /= norm;
        }

        return vec;
    }

    public static double cosineSimilarity(double[] a, double[] b) {
        if (a == null || b == null) return -1.0;
        int n = Math.min(a.length, b.length);
        double dot = 0.0;
        double na = 0.0;
        double nb = 0.0;
        for (int i = 0; i < n; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        if (na == 0 || nb == 0) return -1.0;
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }
}
