package org.example.metrics;
/**
 * המחלקה EuclideanDistance מחשבת מרחק אוקלידי בין שני וקטורים.
 * זה המרחק הגיאומטרי הרגיל בין שתי נקודות במרחב.
 *
 * ככל שהתוצאה קטנה יותר, הווקטורים קרובים יותר אחד לשני.
 */
public class EuclideanDistance implements DistanceMetric {
    @Override
    public double calculate(double[] v1, double[] v2) {
        double sum = 0;
        for (int i = 0; i < v1.length; i++) {
            sum += Math.pow(v1[i] - v2[i], 2);//חישוב  את סכום ריבועי בין שני הערכים של הווקטורים מכפילים בריבוע
        }
        return Math.sqrt(sum);//עושים שורש כדי לקבל את המרחק האוקלידי האמיתי
    }
}