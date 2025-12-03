package com.example.asdproject.controller;

import android.content.Context;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;

/**
 * Centralized manager for Firebase services.
 * This class ensures that all Firebase components are initialized once
 * and provides access to shared instances throughout the application.
 *
 * Required initialization call:
 * FirebaseManager.init(context);
 * This should be executed before any Firebase service is used.
 */
public class FirebaseManager {

    private static FirebaseAuth auth;
    private static FirebaseFirestore db;
    private static FirebaseStorage storage;

    /**
     * Initializes all Firebase components.
     * This should be called in the earliest activity (RoleSelectionActivity or MainActivity).
     *
     * @param context application or activity context used to initialize FirebaseApp.
     */
    public static void init(Context context) {
        FirebaseApp.initializeApp(context);
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
    }

    /**
     * Provides a shared FirebaseAuth instance.
     */
    public static FirebaseAuth getAuth() {
        return auth;
    }

    /**
     * Provides a shared Firestore database instance.
     */
    public static FirebaseFirestore getDb() {
        return db;
    }

    /**
     * Provides a shared Firebase Storage instance.
     */
    public static FirebaseStorage getStorage() {
        return storage;
    }
}
