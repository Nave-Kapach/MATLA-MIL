package org.example;

import java.util.*;

public class SpaceManager {
    private final Map<String, WordVector> wordMap = new HashMap<>();

    public void addWord(String word, double[] vector) {
        wordMap.put(word, new WordVector(word, vector));
    }

    public WordVector getWordVector(String word) {
        return wordMap.get(word);
    }

    public double getSemanticDistance(String word1, String word2, DistanceMetric metric) {
        WordVector v1 = wordMap.get(word1);
        WordVector v2 = wordMap.get(word2);
        if (v1 == null || v2 == null) return -1.0;
        return metric.calculate(v1.getVector(), v2.getVector());
    }

    public Set<String> getAllWords() {
        return wordMap.keySet();
    }

    // Finds the K words closest to an EXISTING word
    public List<WordDistancePair> findNearestNeighbors(String targetWord, int k, DistanceMetric metric) {
        WordVector target = wordMap.get(targetWord);
        if (target == null) return new ArrayList<>();

        // תיקון: שולחים את המילה היחידה כרשימה
        return findNearestNeighborsToVector(target.getVector(), k, metric, Collections.singletonList(targetWord));
    }

    // FIXED: Changed 'String excludeWord' to 'List<String> excludeWords' to handle multiple exclusions
    public List<WordDistancePair> findNearestNeighborsToVector(double[] targetVector, int k, DistanceMetric metric, List<String> excludeWords) {
        List<WordDistancePair> distances = new ArrayList<>();
        for (WordVector wv : wordMap.values()) {

            // תיקון: בודקים אם המילה נמצאת ברשימת המילים שצריך להחריג
            if (excludeWords != null && excludeWords.contains(wv.getWord())) continue;

            double dist = metric.calculate(targetVector, wv.getVector());
            distances.add(new WordDistancePair(wv.getWord(), dist));
        }
        distances.sort(Comparator.comparingDouble(p -> p.distance));
        return distances.subList(0, Math.min(k, distances.size()));
    }

    public double getProjectionValue(String targetWord, String axisWord1, String axisWord2) {
        WordVector target = wordMap.get(targetWord);
        WordVector w1 = wordMap.get(axisWord1);
        WordVector w2 = wordMap.get(axisWord2);

        if (target == null || w1 == null || w2 == null) return 0.0;

        double[] vT = target.getVector();
        double[] v1 = w1.getVector();
        double[] v2 = w2.getVector();

        double[] axis = new double[v1.length];
        double axisMagnitudeSq = 0;
        for (int i = 0; i < axis.length; i++) {
            axis[i] = v2[i] - v1[i];
            axisMagnitudeSq += axis[i] * axis[i];
        }

        if (axisMagnitudeSq == 0) return 0.0;

        double dotProduct = 0;
        for (int i = 0; i < vT.length; i++) {
            dotProduct += (vT[i] - v1[i]) * axis[i]; // Corrected projection math
        }

        return dotProduct / Math.sqrt(axisMagnitudeSq);
    }

    // Calculate Analogy: V1 - V2 + V3 (Requirement Stage B)
    public double[] calculateAnalogy(String w1, String w2, String w3) {
        WordVector v1 = wordMap.get(w1);
        WordVector v2 = wordMap.get(w2);
        WordVector v3 = wordMap.get(w3);
        if (v1 == null || v2 == null || v3 == null) return null;

        double[] result = new double[v1.getVector().length];
        for (int i = 0; i < result.length; i++) {
            result[i] = v1.getVector()[i] - v2.getVector()[i] + v3.getVector()[i];
        }
        return result;
    }

    // Calculate Centroid for a group of words (Requirement Stage B)
    public double[] calculateCentroid(List<String> words) {
        if (words == null || words.isEmpty()) return null;
        int dimensions = wordMap.values().iterator().next().getVector().length;
        double[] centroid = new double[dimensions];

        int count = 0;
        for (String word : words) {
            WordVector wv = wordMap.get(word);
            if (wv != null) {
                for (int i = 0; i < dimensions; i++) {
                    centroid[i] += wv.getVector()[i];
                }
                count++;
            }
        }

        if (count == 0) return null;
        for (int i = 0; i < dimensions; i++) {
            centroid[i] /= count; // Calculate arithmetic mean
        }
        return centroid;
    }

    public static class WordDistancePair {
        private final String word;
        private final double distance;
        public WordDistancePair(String word, double distance) {
            this.word = word;
            this.distance = distance;
        }
        public String getWord() { return word; }
        public double getDistance() { return distance; }
    }
}