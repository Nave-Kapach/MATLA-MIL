package org.example;

// Interface for different distance calculation methods
public interface DistanceMetric {
    // Calculates the distance between two numerical arrays
    double calculate(double[] v1, double[] v2);
}

// Implementation of Euclidean Distance
class EuclideanDistance implements DistanceMetric {
    @Override
    public double calculate(double[] v1, double[] v2) {
        double sum = 0;
        for (int i = 0; i < v1.length; i++) {
            sum += Math.pow(v1[i] - v2[i], 2);
        }
        return Math.sqrt(sum);
    }
}

// Implementation of Cosine Similarity
class CosineSimilarity implements DistanceMetric {
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

        // תיקון: הופכים את הדמיון למרחק (מרחק 0 זה הכי קרוב)
        return 1.0 - similarity;
    }
}