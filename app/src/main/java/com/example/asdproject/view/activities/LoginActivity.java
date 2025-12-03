package com.example.asdproject.view.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

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
        String name = editName.getText().toString().trim();
        String pin = editPin.getText().toString().trim();

        // Basic format validation for login credentials
        if (name.isEmpty() || pin.length() != 4) {
            Toast.makeText(this, "Enter name and 4-digit PIN", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseFirestore db = FirebaseManager.getDb();

        // Look for a child document matching the provided name and PIN
        db.collection("children")
                .whereEqualTo("name", name)
                .whereEqualTo("pin", pin)
                .get()
                .addOnSuccessListener(query -> {
                    if (!query.isEmpty()) {

                        // A matching child document was found; retrieve its data
                        DocumentSnapshot doc = query.getDocuments().get(0);

                        String childName = doc.getString("name");
                        String childId = doc.getId(); // Firestore document ID

                        // Navigate to the child's home dashboard with identifying data
                        Intent intent = new Intent(LoginActivity.this, ChildHomeActivity.class);
                        intent.putExtra("childName", childName);
                        intent.putExtra("childId", childId);
                        startActivity(intent);

                        finish();
                    } else {
                        Toast.makeText(this, "Invalid name or PIN", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
