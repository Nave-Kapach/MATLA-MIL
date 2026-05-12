package org.example.metrics;
/**
 * הממשק DistanceMetric מגדיר פעולה כללית לחישוב מרחק בין שני וקטורים.
 * כל שיטת מרחק במערכת תממש את הממשק הזה בצורה אחרת.
 *
 * זה מאפשר להחליף בין מדדים שונים, כמו Euclidean או Cosine,
 * בלי לשנות את הקוד שמשתמש בהם.
 */
public interface DistanceMetric {
    double calculate(double[] v1, double[] v2);
}