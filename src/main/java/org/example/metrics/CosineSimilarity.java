package org.example.metrics;
/**
 * המחלקה CosineSimilarity מחשבת מרחק לפי דמיון קוסינוס.
 * המדד בודק את הזווית בין שני וקטורים,
 * ולכן הוא מתאים לבדיקה האם שתי מילים דומות בכיוון שלהן במרחב.
 *
 * מכיוון ש-Cosine מחזיר דמיון, אנחנו מחזירים 1 - similarity
 * כדי לקבל ערך שמתנהג כמו מרחק: ככל שהערך קטן יותר, הווקטורים דומים יותר.
 */
public class CosineSimilarity implements DistanceMetric {
    @Override
    public double calculate(double[] v1, double[] v2) {

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < v1.length; i++) {
            dotProduct += v1[i] * v2[i];//dotProduct -
            normA += Math.pow(v1[i], 2);//normA - האורך של הווקטור הראשון בריבוע
            normB += Math.pow(v2[i], 2);//normB - האורך של הווקטור השני בריבוע
        }
        double similarity = dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));// נוסחת Cosine Similarity מכפלה סקלרית חלקי האורכים של הווקטורים

        return 1.0 - similarity;//הופכים את הדמיון למרחק דמיון גובה -->מרחק קטן
    }
}