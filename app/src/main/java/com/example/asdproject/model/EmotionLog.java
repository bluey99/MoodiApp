package com.example.asdproject.model;

import java.util.Date;

/**
 * Data model representing a single emotion entry logged by a child.
 * This class is stored under the Firestore path:
 * children/{childId}/history/{emotionLogId}
 *
 * Firestore requires a public no-argument constructor and public setters
 * to deserialize objects into this model.
 */
public class EmotionLog implements java.io.Serializable {

    /**
     * Firestore document ID for this log entry.
     * Set manually after retrieving the document.
     */
    private String id;

    /**
     * Identifier of the child who created the emotion entry.
     * Matches the Firestore child document ID.
     */
    private String childId;

    /** The emotion selected by the user (e.g., Happy, Sad, Angry). */
    private String emotion;

    /** Intensity level of the emotion (0–100 or SeekBar scale). */
    private int intensity;

    /** Optional descriptive note written by the child. */
    private String note;

    /** Timestamp indicating when the emotion entry was created. */
    private Date timestamp;

    /**
     * Required empty constructor for Firestore deserialization.
     * Do not remove.
     */
    public EmotionLog() {
    }

    /**
     * Creates a new emotion log entry at the current time.
     *
     * @param childId   Firestore ID of the child
     * @param emotion   Selected emotion label
     * @param intensity Selected intensity level
     * @param note      Optional note entered by the user
     */
    public EmotionLog(String childId, String emotion, int intensity, String note) {
        this.childId = childId;
        this.emotion = emotion;
        this.intensity = intensity;
        this.note = note;
        this.timestamp = new Date();
    }

    // Getters and setters required by Firestore

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getChildId() {
        return childId;
    }

    public void setChildId(String childId) {
        this.childId = childId;
    }

    public String getEmotion() {
        return emotion;
    }

    public void setEmotion(String emotion) {
        this.emotion = emotion;
    }

    public int getIntensity() {
        return intensity;
    }

    public void setIntensity(int intensity) {
        this.intensity = intensity;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
}
