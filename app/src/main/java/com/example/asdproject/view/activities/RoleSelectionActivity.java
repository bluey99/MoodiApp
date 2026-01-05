package com.example.asdproject.view.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.example.asdproject.R;
import com.example.asdproject.controller.FirebaseManager;

/**
 * Entry point of the application.
 * Allows the user to select their role (Child or Parent).
 * Initializes Firebase before navigating to authentication.
 */
public class RoleSelectionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize Firebase before any DB/Auth usage
        FirebaseManager.init(this);

        setContentView(R.layout.activity_role_selection);

        // Role buttons
        LinearLayout btnChild = findViewById(R.id.btnChild);
        LinearLayout btnParent = findViewById(R.id.btnParent);

        // Child role → Child login
        btnChild.setOnClickListener(v -> {
            Intent intent = new Intent(RoleSelectionActivity.this, LoginActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        // Parent role → Parent login
        btnParent.setOnClickListener(v -> {
            Intent intent = new Intent(RoleSelectionActivity.this, ParentLoginActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });
    }
}
