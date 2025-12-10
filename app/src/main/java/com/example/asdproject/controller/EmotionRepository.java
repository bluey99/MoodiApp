package com.example.asdproject.controller;

import android.util.Log;

import com.example.asdproject.model.EmotionLog;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Repository responsible for writing emotion log entries to Firestore.
 *
 * Firestore data path:
 *   children/{childId}/history/{logId}
 *
 * This class abstracts all emotion-log related database operations.
 */
public class EmotionRepository {

    private static final String TAG = "EmotionRepository";

    /** Shared Firestore instance from FirebaseManager. */
    private final FirebaseFirestore db = FirebaseManager.getDb();

    /**
     * Saves a completed EmotionLog under:
     *   children/{childId}/history/
     *
     * This method is fire-and-forget for now:
     *  - EmotionLogActivity calls this
     *  - Success/failure is only logged to Logcat
     *
     * @param log Final EmotionLog produced in Step 7.
     */
    public void addEmotionLog(EmotionLog log) {

        // Safety check
        if (log.getChildId() == null || log.getChildId().trim().isEmpty()) {
            Log.e(TAG, "Cannot save log: childId is null or empty");
            return;
        }

        db.collection("children")
                .document(log.getChildId())
                .collection("history")
                .add(log)
                .addOnSuccessListener(docRef -> {
                    // Save the Firestore document ID inside the model
                    log.setId(docRef.getId());
                    Log.d(TAG, "Emotion log saved. ID = " + docRef.getId());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to save emotion log", e);
                });
    }
}
