package com.example.asdproject.controller;

import android.content.Context;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;

/**
 * Centralized manager for Firebase services.
 * Initializes Firebase SDK and ensures authentication is established
 * before any database operations are performed.
 */
public class FirebaseManager {

    private static FirebaseAuth auth;
    private static FirebaseFirestore db;
    private static FirebaseStorage storage;
    private static boolean initialized = false;

    /**
     * Initializes all Firebase components.
     * Must be called once at app startup (RoleSelectionActivity).
     */
    public static void init(Context context) {
        if (initialized) return;

        FirebaseApp.initializeApp(context);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        // 🔐 Ensure Firebase Authentication (Anonymous)
        if (auth.getCurrentUser() == null) {
            Log.e("AUTH_CHECK", "❌ No Firebase user, signing in anonymously");

            auth.signInAnonymously()
                    .addOnSuccessListener(result ->
                            Log.e(
                                    "AUTH_CHECK",
                                    "✅ Signed in anonymously: " + result.getUser().getUid()
                            )
                    )
                    .addOnFailureListener(e ->
                            Log.e("AUTH_CHECK", "❌ Anonymous auth failed", e)
                    );
        } else {
            Log.e(
                    "AUTH_CHECK",
                    "✅ Already authenticated: " + auth.getCurrentUser().getUid()
            );
        }

        initialized = true;
    }

    public static FirebaseAuth getAuth() {
        return auth;
    }

    public static FirebaseFirestore getDb() {
        return db;
    }

    public static FirebaseStorage getStorage() {
        return storage;
    }
}
