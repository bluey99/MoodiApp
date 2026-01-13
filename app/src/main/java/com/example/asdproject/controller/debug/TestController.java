package com.example.asdproject.controller.debug;

import android.util.Log;

import com.example.asdproject.controller.EmotionRepository;
import com.example.asdproject.controller.FirebaseManager;
import com.example.asdproject.model.EmotionLog;
import com.example.asdproject.model.EmotionLogDraft;

/**
 * Utility class used for development-time testing of Firestore connectivity
 * and EmotionRepository functionality.
 *
 * Not used in production — call manually during debugging.
 */
public class TestController {

    /**
     * Writes a simple test document to Firestore to verify connectivity.
     */
    public static void testFirestore() {
        FirebaseManager.getDb()
                .collection("test")
                .add(new java.util.HashMap<String, Object>() {{
                    put("message", "Firebase connected!");
                }})
                .addOnSuccessListener(doc ->
                        Log.d("FIREBASE", "Document saved with ID: " + doc.getId()))
                .addOnFailureListener(e ->
                        Log.e("FIREBASE", "Error adding document", e));
    }

    /**
     * Demonstrates building and saving an EmotionLog from a fake draft.
     * Used only for manual debugging.
     */
    public static void testAddEmotion() {
        EmotionRepository repo = new EmotionRepository();

        // Create a fake draft
        EmotionLogDraft draft = new EmotionLogDraft();
        draft.situation = "At school";
        draft.location = "Playground";
        draft.feeling = "Happy";
        draft.intensity = 5;
        draft.photoUri = null;
        draft.note = "I felt very good today.";

        // IMPORTANT: this is the REAL childID value (field childID in Firestore)
        EmotionLog log = new EmotionLog("child001", draft);

        // Save using the new signature
        repo.addEmotionLog(
                log,
                () -> Log.d("TEST", "Emotion log saved successfully"),
                () -> Log.e("TEST", "Failed to save emotion log")
        );
    }
}
