package com.example.asdproject.model;

import java.util.Date;

/**
 * Final data model saved to Firestore after the 7-step child emotion-logging flow.
 *
 * This class is used ONLY for persistent storage (the final result).
 * All in-progress values during the flow are stored inside EmotionLogDraft.
 *
 * Firestore requires:
 *  - A public no-argument constructor
 *  - Public getters and setters for each field (for deserialization)
 *
 * Stored under Firestore path:
 *      children/{childId}/history/{generatedLogId}
 */
public class EmotionLog implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    // ----------------------------- Firestore fields -----------------------------

    /** Firestore document ID (set after saving or when reading from DB). */
    private String id;

    /** Child ID used in Firestore path. Must not be null. */
    private String childId;

    /** Step 1: Situation chosen by the child. */
    private String situation;

    /** Step 2: Location selected by the child. */
    private String location;

    /** Step 3: Feeling label (e.g., Happy, Angry, Scared). */
    private String feeling;

    /** Step 4: Intensity level on a 1–5 scale. */
    private int intensity;

    /** Step 5: Download URL of the uploaded photo (or null if skipped). */
    private String photoUri;

    /** Step 6: Optional note text written by the child. */
    private String note;

    /** Automatically assigned when saving the log. */
    private Date timestamp;

    /** Required empty constructor for Firestore. DO NOT REMOVE. */
    public EmotionLog() { }

    /**
     * Creates a final EmotionLog object from the EmotionLogDraft.
     * This is called in Step 7 when the child confirms their log.
     *
     * @param childId Firestore child document ID
     * @param draft   All collected data from the 7-step flow
     */
    public EmotionLog(String childId, EmotionLogDraft draft) {
        this.childId = childId;
        this.situation = draft.situation;
        this.location = draft.location;
        this.feeling = draft.feeling;
        this.intensity = draft.intensity;
        this.photoUri = draft.photoUri;
        this.note = draft.note;

        // The timestamp should be the moment of saving the final log
        this.timestamp = new Date();
    }

    // ----------------------------- Getters & Setters -----------------------------

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getChildId() { return childId; }
    public void setChildId(String childId) { this.childId = childId; }

    public String getSituation() { return situation; }
    public void setSituation(String situation) { this.situation = situation; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getFeeling() { return feeling; }
    public void setFeeling(String feeling) { this.feeling = feeling; }

    public int getIntensity() { return intensity; }
    public void setIntensity(int intensity) { this.intensity = intensity; }

    public String getPhotoUri() { return photoUri; }
    public void setPhotoUri(String photoUri) { this.photoUri = photoUri; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }
}
