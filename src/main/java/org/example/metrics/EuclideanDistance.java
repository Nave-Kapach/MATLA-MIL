package org.example.metrics;

public class EuclideanDistance implements DistanceMetric {
    @Override
    public double calculate(double[] v1, double[] v2) {
        double sum = 0;
        for (int i = 0; i < v1.length; i++) {
            sum += Math.pow(v1[i] - v2[i], 2);
        }
        return Math.sqrt(sum);
    }
}