package com.example.asdproject.model;

/**
 * Enum representing all supported emotional states in the logging flow.
 * Using an enum ensures type-safety and eliminates string typing errors.
 */
public enum Feeling {
    HAPPY("Happy"),
    SAD("Sad"),
    ANGRY("Angry"),
    SURPRISED("Surprised"),
    AFRAID("Afraid"),
    DISGUST("Disgust"),
    UNSURE("Unsure"),
    OTHER("Other");

    private final String label;

    Feeling(String label) {
        this.label = label;
    }

    /** Returns the user-friendly label stored in Firestore. */
    public String getLabel() {
        return label;
    }
}
