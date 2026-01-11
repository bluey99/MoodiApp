package com.example.asdproject.view.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.example.asdproject.util.HashUtil;

import com.example.asdproject.R;
import com.example.asdproject.controller.FirebaseManager;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Handles child authentication using name and a 4-digit PIN.
 * If credentials are valid, the child is navigated to the home screen.
 */
public class LoginActivity extends AppCompatActivity {

    private EditText editName;
    private EditText editPin;
    private Button btnLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize Firebase; required before accessing Firestore
        FirebaseManager.init(this);

        setContentView(R.layout.activity_login);

        // Link UI fields
        editName = findViewById(R.id.editChildName);
        editPin = findViewById(R.id.editChildPin);
        btnLogin = findViewById(R.id.btnLoginChild);

        // Attempt login when the user taps the button
        btnLogin.setOnClickListener(v -> logInChild());

        // Close the screen and return to role selection
        findViewById(R.id.txtCancel).setOnClickListener(v -> finish());
    }

    /**
     * Validates input and queries Firestore to authenticate the child.
     */
    private void logInChild() {

        String username = editName.getText().toString().trim();
        String pin = editPin.getText().toString().trim();

        if (username.isEmpty() || pin.length() != 4) {
            Toast.makeText(this, "Enter username and 4-digit PIN", Toast.LENGTH_SHORT).show();
            return;
        }

        String pinHash = HashUtil.sha256(pin);
        if (pinHash == null) {
            Toast.makeText(this, "Login error", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseFirestore db = FirebaseManager.getDb();

        db.collection("children")
                .whereEqualTo("username", username)
                .get()
                .addOnSuccessListener(query -> {

                    if (query.isEmpty()) {
                        Toast.makeText(this, "Invalid username or PIN", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    DocumentSnapshot doc = query.getDocuments().get(0);

                    String storedPinHash = doc.getString("pinHash");

                    if (storedPinHash != null && storedPinHash.equals(pinHash)) {

                        Intent intent = new Intent(this, ChildHomeActivity.class);
                        intent.putExtra("childId", doc.getId());
                        intent.putExtra("childName", doc.getString("name")); // display only
                        startActivity(intent);
                        finish();

                    } else {
                        Toast.makeText(this, "Invalid username or PIN", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

}
