package com.example.asdproject.view.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.asdproject.view.activities.ParentHomeActivity;
import com.example.asdproject.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Map;

public class ParentLoginActivity extends AppCompatActivity {

    private EditText etEmail;
    private EditText etPassword;
    private Button btnLogin;
    private TextView txtCancel;

    private FirebaseFirestore db;
    private static final String TAG = "ParentLoginFirestore";
    private static final String PARENTS_COLLECTION = "parents"; // 👈 collection name in Firestore

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_login);

        setTitle("Parent Login");

        db = FirebaseFirestore.getInstance();

        etEmail    = findViewById(R.id.edtName);      // EMAIL field in layout
        etPassword = findViewById(R.id.edtPassword);  // PASSWORD field in layout
        btnLogin   = findViewById(R.id.btnLogin);
        txtCancel  = findViewById(R.id.txtCancel);

        btnLogin.setOnClickListener(v -> loginParent());
        txtCancel.setOnClickListener(v -> finish());
    }

    private void loginParent() {
        String emailInput = etEmail.getText().toString().trim();
        String passwordInput = etPassword.getText().toString().trim();

        if (emailInput.isEmpty() || passwordInput.isEmpty()) {
            Toast.makeText(
                    this,
                    "Please enter both email and password.",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        btnLogin.setEnabled(false);

        Log.d(TAG, "Login attempt email='" + emailInput + "', password='" + passwordInput + "'");

        // 🔍 NEW STRATEGY:
        // 1) Load ALL parents
        // 2) Auto-detect which fields are email/password
        // 3) Compare in Java (no whereEqualTo)
        db.collection(PARENTS_COLLECTION)
                .get()
                .addOnSuccessListener(this::handleAllParentsForLogin)
                .addOnFailureListener(e -> {
                    btnLogin.setEnabled(true);
                    Log.e(TAG, "Firestore error when reading parents collection", e);
                    Toast.makeText(
                            ParentLoginActivity.this,
                            "Firestore error: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void handleAllParentsForLogin(QuerySnapshot allParents) {
        btnLogin.setEnabled(true);

        if (allParents.isEmpty()) {
            Toast.makeText(
                    this,
                    "No parents found in Firestore (collection is empty).",
                    Toast.LENGTH_LONG
            ).show();
            Log.d(TAG, "parents collection is EMPTY.");
            return;
        }

        Log.d(TAG, "Total parents in collection: " + allParents.size());

        // 1️⃣ Auto-detect field names (email/password/name) from the FIRST document
        String emailFieldKey = null;
        String passwordFieldKey = null;
        String nameFieldKey = null;

        DocumentSnapshot firstDoc = allParents.getDocuments().get(0);
        Map<String, Object> firstData = firstDoc.getData();

        if (firstData != null) {
            for (String key : firstData.keySet()) {
                String lower = key.toLowerCase();

                // guess email field
                if (emailFieldKey == null && lower.contains("email")) {
                    emailFieldKey = key;
                }

                // guess password field
                if (passwordFieldKey == null &&
                        (lower.contains("password") || lower.equals("pass") || lower.equals("pwd"))) {
                    passwordFieldKey = key;
                }

                // guess name field
                if (nameFieldKey == null && lower.contains("name")) {
                    nameFieldKey = key;
                }
            }
        }

        Log.d(TAG, "Auto-detected keys: emailFieldKey=" + emailFieldKey
                + ", passwordFieldKey=" + passwordFieldKey
                + ", nameFieldKey=" + nameFieldKey);

        if (emailFieldKey == null) {
            Toast.makeText(
                    this,
                    "Could not find an email field in parent documents (field name should contain 'email').",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        if (passwordFieldKey == null) {
            Toast.makeText(
                    this,
                    "Could not find a password field in parent documents.",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        // 2️⃣ Now we know which fields are email/password → scan all parents
        String emailInput = etEmail.getText().toString().trim();
        String passwordInput = etPassword.getText().toString().trim();

        DocumentSnapshot matchedParent = null;

        for (DocumentSnapshot doc : allParents) {
            Map<String, Object> data = doc.getData();
            if (data == null) continue;

            String docEmail = safeStr(data.get(emailFieldKey));
            String docPassword = safeStr(data.get(passwordFieldKey));

            Log.d(TAG, "Checking doc " + doc.getId()
                    + " -> " + emailFieldKey + "=" + docEmail
                    + ", " + passwordFieldKey + "=" + docPassword);

            if (docEmail != null && docEmail.trim().equals(emailInput)) {
                // email matches
                if (docPassword != null && docPassword.equals(passwordInput)) {
                    matchedParent = doc;
                    break;
                }
            }
        }

        if (matchedParent == null) {
            Toast.makeText(
                    this,
                    "Email or password is incorrect.",
                    Toast.LENGTH_SHORT
            ).show();
            Log.d(TAG, "No matching parent found for given email + password.");
            return;
        }

        // 3️⃣ Successful login
        String parentId = matchedParent.getId();
        Map<String, Object> mpData = matchedParent.getData();

        String parentEmail = (mpData != null) ? safeStr(mpData.get(emailFieldKey)) : null;
        String parentName  = (mpData != null && nameFieldKey != null)
                ? safeStr(mpData.get(nameFieldKey))
                : null;

        if (parentName == null || parentName.trim().isEmpty()) {
            parentName = parentEmail;
        }

        Log.d(TAG, "Login success: id=" + parentId
                + ", email=" + parentEmail
                + ", name=" + parentName
                + ", fullData=" + mpData);

        Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show();

        Intent i = new Intent(ParentLoginActivity.this, ParentHomeActivity.class);
        i.putExtra("PARENT_ID", parentId);
        i.putExtra("PARENT_NAME", parentName);
        i.putExtra("PARENT_EMAIL", parentEmail);
        startActivity(i);
        finish();
    }

    private String safeStr(Object o) {
        return (o == null) ? null : o.toString();
    }
}
