package org.example;

// This class represents a single word and its vector of numbers
public class WordVector {
    private final String word;
    private final double[] vector;

    // Constructor to initialize word and vector
    public WordVector(String word, double[] vector) {
        this.word = word;
        this.vector = vector;
    }

    // Get the word text
    public String getWord() {
        return word;
    }

    // Get the numerical vector array
    public double[] getVector() {
        return vector;
    }

    // Get how many dimensions this vector has
    public int getDimension() {
        return vector.length;
    }
}