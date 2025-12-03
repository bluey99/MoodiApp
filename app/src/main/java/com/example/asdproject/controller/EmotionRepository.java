package com.example.asdproject.controller;

import com.example.asdproject.model.EmotionLog;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Repository responsible for writing emotion log entries to Firestore.
 * The data path used is:
 * children/{childId}/history/{generatedLogId}
 *
 * This class abstracts database access for emotion-related operations.
 */
public class EmotionRepository {

    /** Shared Firestore instance obtained through FirebaseManager. */
    private final FirebaseFirestore db = FirebaseManager.getDb();

    /**
     * Adds a new emotion log entry under the child's history collection.
     *
     * @param log EmotionLog object provided by the UI layer.
     *            The log must contain a valid childId.
     */
    public void addEmotionLog(EmotionLog log) {
        // Validate childId before writing to Firestore
        if (log.getChildId() == null) {
            // Child ID is required to correctly store logs under the child document
            return;
        }

        db.collection("children")
                .document(log.getChildId())
                .collection("history")
                .add(log)
                .addOnSuccessListener(docRef -> {
                    // Entry successfully written to Firestore
                })
                .addOnFailureListener(e -> {
                    // Handle write failure (logging recommended for debugging)
                });
    }
}
