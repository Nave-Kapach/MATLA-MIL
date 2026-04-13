package org.example.metrics;

public class CosineSimilarity implements DistanceMetric {
    @Override
    public double calculate(double[] v1, double[] v2) {
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < v1.length; i++) {
            dotProduct += v1[i] * v2[i];
            normA += Math.pow(v1[i], 2);
            normB += Math.pow(v2[i], 2);
        }

        double similarity = dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
        return 1.0 - similarity;
    }
}