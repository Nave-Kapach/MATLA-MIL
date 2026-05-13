package org.example.core;

import org.example.metrics.DistanceMetric;

import java.util.*;

/**
 * המחלקה SpaceManager היא המחלקה המרכזית שמנהלת את המרחב הווקטורי.
 * היא שומרת את כל המילים והווקטורים שלהן,
 * ומבצעת פעולות לוגיות כמו חישוב מרחקים, שכנים קרובים, אנלוגיות ומרכז כובד.
 */
public class SpaceManager {
    private final Map<String, WordVector> wordMap = new HashMap<>(); // שומר מיפוי בין מילה לבין הווקטור שלה

    // מוסיף מילה חדשה למרחב יחד עם הווקטור שלה
    public void addWord(String word, double[] vector) {
        wordMap.put(word, new WordVector(word, vector));
    }

    // מחזיר את ה-WordVector של מילה מסוימת
    public WordVector getWordVector(String word) {
        return wordMap.get(word);
    }

    // מחשב מרחק סמנטי בין שתי מילים לפי שיטת המרחק שנבחרה
    public double getSemanticDistance(String word1, String word2, DistanceMetric metric) {
        WordVector v1 = wordMap.get(word1);
        WordVector v2 = wordMap.get(word2);

        if (v1 == null || v2 == null) return -1.0;

        return metric.calculate(v1.getVector(), v2.getVector());
    }

    // מחזיר את כל המילים שקיימות במרחב
    public Set<String> getAllWords() {
        return wordMap.keySet();
    }

    // מוצא את k המילים הקרובות ביותר למילה קיימת במרחב
    public List<WordDistancePair> findNearestNeighbors(String targetWord, int k, DistanceMetric metric) {
        WordVector target = wordMap.get(targetWord);

        if (target == null) return new ArrayList<>();

        return findNearestNeighborsToVector(   target.getVector(), k,metric,   Collections.singletonList(targetWord)

        );
    }

    /*
     * מוצא את k המילים הקרובות ביותר לווקטור מסוים.
     * שימושי גם לאנלוגיה, גם ל-centroid וגם לביטוי וקטורי כללי.
     */
    public List<WordDistancePair> findNearestNeighborsToVector(double[] targetVector, int k, DistanceMetric metric, List<String> excludeWords) {
        List<WordDistancePair> distances = new ArrayList<>();

        if (targetVector == null) return distances;

        for (WordVector wv : wordMap.values()) {
            if (excludeWords != null && excludeWords.contains(wv.getWord())) continue;

            double dist = metric.calculate(targetVector, wv.getVector());
            distances.add(new WordDistancePair(wv.getWord(), dist));
        }

        distances.sort(Comparator.comparingDouble(p -> p.distance));

        return distances.subList(0, Math.min(k, distances.size()));
    }

    /*
     * מחשב ערך הקרנה של מילה על ציר שמוגדר על ידי שתי מילים אחרות.
     */
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
            dotProduct += (vT[i] - v1[i]) * axis[i];
        }

        return dotProduct / Math.sqrt(axisMagnitudeSq);
    }

    /*
     * מייצג איבר אחד בביטוי וקטורי.
     * לדוגמה:
     * + king
     * - man
     */
    public static class VectorExpressionTerm {
        private final String word; // המילה בביטוי
        private final int sign; // 1 עבור פלוס, -1 עבור מינוס

        public VectorExpressionTerm(String word, int sign) {
            this.word = word;
            this.sign = sign;
        }

        public String getWord() {
            return word;
        }

        public int getSign() {
            return sign;
        }
    }

    /*
     * מחשב ביטוי וקטורי כללי.
     * לדוגמה:
     * +king -man +woman +royal
     *
     * המתודה מוסיפה או מחסירה כל וקטור לפי הסימן שלו.
     */
    public double[] calculateVectorExpression(List<VectorExpressionTerm> terms) {
        if (terms == null || terms.isEmpty()) return null;
        if (wordMap.isEmpty()) return null;

        int dimensions = wordMap.values().iterator().next().getVector().length;
        double[] result = new double[dimensions];

        boolean hasValidWord = false;

        for (VectorExpressionTerm term : terms) {
            if (term == null || term.getWord() == null) continue;

            WordVector wordVector = wordMap.get(term.getWord());
            if (wordVector == null) continue;

            double[] vector = wordVector.getVector();
            int sign = term.getSign() >= 0 ? 1 : -1;

            for (int i = 0; i < dimensions; i++) {
                result[i] += sign * vector[i];
            }

            hasValidWord = true;
        }

        if (!hasValidWord) return null;

        return result;
    }

    /*
     * מחשב מרכז כובד של קבוצת מילים.
     * המרכז הוא הממוצע של כל הווקטורים שנבחרו.
     */
    public double[] calculateCentroid(List<String> words) {
        if (words == null || words.isEmpty()) return null;
        if (wordMap.isEmpty()) return null;

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
            centroid[i] /= count;
        }

        return centroid;
    }

    /**
     * מחלקה פנימית שמייצגת זוג של מילה ומרחק.
     * משתמשים בה כדי להחזיר שכנים קרובים עם המרחק שלהם.
     */
    public static class WordDistancePair {
        private final String word;
        private final double distance;

        public WordDistancePair(String word, double distance) {
            this.word = word;
            this.distance = distance;
        }

        public String getWord() {
            return word;
        }

        public double getDistance() {
            return distance;
        }
    }
}