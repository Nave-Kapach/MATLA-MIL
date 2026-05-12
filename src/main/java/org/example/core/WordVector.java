package org.example.core;
/**
 * המחלקה WordVector מייצגת מילה אחת ואת הווקטור המספרי שלה.
 * כל אובייקט כזה שומר את הטקסט של המילה ואת המיקום שלה במרחב הווקטורי.
 */
public class WordVector {
    private final String word; // המילה עצמה
    private final double[] vector; // הווקטור המספרי שמייצג את המילה

    // בנאי שמקבל מילה ואת הווקטור שלה ושומר אותם באובייקט
    public WordVector(String word, double[] vector) {
        this.word = word;
        this.vector = vector;
    }
    // מחזיר את הטקסט של המילה
    public String getWord() {
        return word;
    }
    // מחזיר את הווקטור המספרי של המילה
    public double[] getVector() {
        return vector;
    }
    // מחזיר את מספר הממדים של הווקטור
    public int getDimension() {
        return vector.length;
    }
}