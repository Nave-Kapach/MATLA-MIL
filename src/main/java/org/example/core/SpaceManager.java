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

        if (v1 == null || v2 == null) return -1.0; // אם אחת המילים לא קיימת, מחזירים ערך שמסמן שגיאה

        return metric.calculate(v1.getVector(), v2.getVector()); // החישוב עצמו מתבצע דרך DistanceMetric
    }

    // מחזיר את כל המילים שקיימות במרחב
    public Set<String> getAllWords() {
        return wordMap.keySet();
    }

    // מוצא את k המילים הקרובות ביותר למילה קיימת במרחב
    public List<WordDistancePair> findNearestNeighbors(String targetWord, int k, DistanceMetric metric) {
        WordVector target = wordMap.get(targetWord);

        if (target == null) return new ArrayList<>(); // אם המילה לא קיימת, מחזירים רשימה ריקה

        return findNearestNeighborsToVector(
                target.getVector(),
                k,
                metric,
                Collections.singletonList(targetWord) // לא רוצים שהמילה תהיה שכן של עצמה
        );
    }

    //מוצא את K המילים הקרובות שיותר לווקטור מסיום רלוונטי גם למילה קיימת וגם לווקטור חדש שנוצר מחישוב כמו אנלוגיה או מרכז כובד
    public List<WordDistancePair> findNearestNeighborsToVector(double[] targetVector, int k, DistanceMetric metric, List<String> excludeWords) {
        List<WordDistancePair> distances = new ArrayList<>(); // רשימה של מילים והמרחק שלהן מהווקטור

        for (WordVector wv : wordMap.values()) {
            if (excludeWords != null && excludeWords.contains(wv.getWord())) continue; // מדלג על מילים שלא רוצים לכלול בתוצאה

            double dist = metric.calculate(targetVector, wv.getVector()); // חישוב המרחק בין הווקטור לבין המילה הנוכחית
            distances.add(new WordDistancePair(wv.getWord(), dist)); // שמירת המילה יחד עם המרחק שלה
        }

        distances.sort(Comparator.comparingDouble(p -> p.distance)); // מיון לפי מרחק מהקטן לגדול

        return distances.subList(0, Math.min(k, distances.size())); // מחזיר עד k שכנים קרובים
    }

    //מחשב ערך הקרנה של מילה על ציר שמוגדר על ידי שתי מילים אחרות בודקים איפה targetWord נמצא ביחס לציר שבין axisWord1 לaxisWord2
    public double getProjectionValue(String targetWord, String axisWord1, String axisWord2) {
        WordVector target = wordMap.get(targetWord);
        WordVector w1 = wordMap.get(axisWord1);
        WordVector w2 = wordMap.get(axisWord2);

        if (target == null || w1 == null || w2 == null) return 0.0; // אם אחת המילים חסרה, אין הקרנה תקינה

        double[] vT = target.getVector(); // הווקטור של המילה שרוצים להקרין
        double[] v1 = w1.getVector(); // תחילת הציר
        double[] v2 = w2.getVector(); // סוף הציר

        double[] axis = new double[v1.length]; // וקטור הכיוון של הציר
        double axisMagnitudeSq = 0; // אורך הציר בריבוע

        for (int i = 0; i < axis.length; i++) {
            axis[i] = v2[i] - v1[i]; // בניית וקטור הציר
            axisMagnitudeSq += axis[i] * axis[i]; // חישוב האורך בריבוע
        }

        if (axisMagnitudeSq == 0) return 0.0; // אם שתי מילות הציר זהות, אין כיוון לציר

        double dotProduct = 0;

        for (int i = 0; i < vT.length; i++) {
            dotProduct += (vT[i] - v1[i]) * axis[i]; // מכפלה סקלרית של המילה ביחס לציר
        }

        return dotProduct / Math.sqrt(axisMagnitudeSq); // ערך ההקרנה על הציר
    }

    //מחשב אנלוגיה ווקטורית בצורה V1-V2+V3
    public double[] calculateAnalogy(String w1, String w2, String w3) {
        WordVector v1 = wordMap.get(w1);
        WordVector v2 = wordMap.get(w2);
        WordVector v3 = wordMap.get(w3);

        if (v1 == null || v2 == null || v3 == null) return null; // אם אחת המילים לא קיימת, אין תוצאה

        double[] result = new double[v1.getVector().length]; // וקטור התוצאה של האנלוגיה

        for (int i = 0; i < result.length; i++) {
            result[i] = v1.getVector()[i] - v2.getVector()[i] + v3.getVector()[i]; // חישוב לפי כל ממד בנפרד
        }

        return result;
    }

    //מחשב מרכז כובד של קבוצת מילים המרכז הוא הממוצע של כל הווקטורים שנבחרו
    public double[] calculateCentroid(List<String> words) {
        if (words == null || words.isEmpty()) return null; // אין מילים לחישוב

        int dimensions = wordMap.values().iterator().next().getVector().length; // מספר הממדים של הווקטורים
        double[] centroid = new double[dimensions]; // וקטור מרכז הכובד

        int count = 0; // סופר כמה מילים תקינות נכנסו לחישוב

        for (String word : words) {
            WordVector wv = wordMap.get(word);

            if (wv != null) {
                for (int i = 0; i < dimensions; i++) {
                    centroid[i] += wv.getVector()[i]; // סכימת הערכים בכל ממד
                }

                count++;
            }
        }

        if (count == 0) return null; // אם אף מילה לא נמצאה, אין מרכז כובד

        for (int i = 0; i < dimensions; i++) {
            centroid[i] /= count; // ממוצע חשבוני בכל ממד
        }

        return centroid;
    }

    //מחלקה פנימית שמייצגת זוג של מילה ומרחק משתמשים בה כדי להחזיר רשימת שכנים קרובים יחד עם המרחק שלהם
    public static class WordDistancePair {
        private final String word; // המילה שנמצאה
        private final double distance; // המרחק שלה מהווקטור שנבדק

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