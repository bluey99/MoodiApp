package com.example.asdproject.model;

/**
 * Temporary in-memory object that stores all user inputs
 * during the multi-step logging process.
 */
public class EmotionLogDraft {

    // Step 1
    public String situation;

    // Step 2
    public String location;

    // Step 3
    public String feeling;   // renamed from "emotion"

    // Step 4
    public int intensity;    // 1–5 scale later

    // Step 5
    public String photoUri;

    // Step 6
    public String note;   // Optional free-text note from Step 6


    // Optional voice recording (future) public String voiceUri;
}
