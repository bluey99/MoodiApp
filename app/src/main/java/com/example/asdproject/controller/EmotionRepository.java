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
    public void addEmotionLog(
            EmotionLog log,
            Runnable onSuccess,
            Runnable onFailure
    ) {

        if (log.getChildId() == null || log.getChildId().trim().isEmpty()) {
            Log.e(TAG, "Cannot save log: childId is null or empty");
            if (onFailure != null) onFailure.run();
            return;
        }

        db.collection("children")
                .whereEqualTo("childID", log.getChildId())
                .limit(1)
                .get()
                .addOnSuccessListener(query -> {

                    if (query.isEmpty()) {
                        Log.e(TAG, "No child document found for childID=" + log.getChildId());
                        if (onFailure != null) onFailure.run();
                        return;
                    }

                    String childDocId = query.getDocuments().get(0).getId();

                    db.collection("children")
                            .document(childDocId)
                            .collection("history")
                            .add(log)
                            .addOnSuccessListener(docRef -> {
                                log.setId(docRef.getId());
                                Log.d(TAG, "Emotion log saved. ID = " + docRef.getId());
                                if (onSuccess != null) onSuccess.run();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to save emotion log", e);
                                if (onFailure != null) onFailure.run();
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to resolve child document", e);
                    if (onFailure != null) onFailure.run();
                });
    }


}
