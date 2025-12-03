package com.example.asdproject.view.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.example.asdproject.R;
import com.example.asdproject.controller.FirebaseManager;

/**
 * Entry point of the application.
 * Allows the user to select their role. Currently only the child role is enabled.
 * Initializes Firebase before navigating to authentication.
 */
public class RoleSelectionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Firebase initialization is required before any authentication or database operations
        FirebaseManager.init(this);

        setContentView(R.layout.activity_role_selection);

        // UI element representing the child role option
        LinearLayout childBtn = findViewById(R.id.btnChild);

        // Navigate to LoginActivity when the child role is selected
        childBtn.setOnClickListener(v -> {
            Intent intent = new Intent(RoleSelectionActivity.this, LoginActivity.class);
            startActivity(intent);

            // Apply a simple fade transition during navigation
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });
    }
}
