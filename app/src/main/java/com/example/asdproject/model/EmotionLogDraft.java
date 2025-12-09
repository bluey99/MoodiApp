package com.example.asdproject.model;

/**
 * Temporary in-memory object that stores all user inputs
 * during the multi-step logging process.
 */
public class EmotionLogDraft implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    // Step 1
    public String situation;

    // Step 2
    public String location;

    // Step 3
    public String feeling;   // renamed from "emotion"

    // Step 4
    public int intensity;    // 1–5 scale

    // Step 5
    public String photoUri;

    // Step 6
    public String note;      // Optional free-text note

    // Future extension
    // public String voiceUri;
}
