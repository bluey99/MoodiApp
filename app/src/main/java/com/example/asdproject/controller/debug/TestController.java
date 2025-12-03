package com.example.asdproject.controller.debug;

import android.util.Log;

import com.example.asdproject.controller.EmotionRepository;
import com.example.asdproject.controller.FirebaseManager;
import com.example.asdproject.model.EmotionLog;

/**
 * Utility class used for development-time testing of Firestore connectivity
 * and EmotionRepository functionality.
 *
 * This class is not used in the production workflow and should only be
 * executed manually during debugging.
 */
public class TestController {

    /**
     * Writes a simple test document to Firestore to verify database connectivity.
     * Creates a collection named "test" with a single message field.
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
     * Demonstrates creating and saving an EmotionLog
     * using the EmotionRepository class.
     * This method is intended for manual testing only.
     */
    public static void testAddEmotion() {
        EmotionRepository repo = new EmotionRepository();

        EmotionLog log = new EmotionLog(
                "child001",       // Example child ID; must match an existing document
                "Happy",
                5,
                "I felt very good today."
        );

        repo.addEmotionLog(log);
    }
}
