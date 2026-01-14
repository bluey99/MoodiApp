package com.example.asdproject.view.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import com.example.asdproject.R;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ParentSignUpActivity extends AppCompatActivity {

    private EditText edtEmail, edtName, edtPassword;
    private FirebaseFirestore db;

    private static final String PARENTS_COLLECTION = "parents";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_sign_up);

        db = FirebaseFirestore.getInstance();

        // 🔹 Only parent fields
        edtEmail    = findViewById(R.id.edtEmail);
        edtName     = findViewById(R.id.edtName);
        edtPassword = findViewById(R.id.edtPassword);

        findViewById(R.id.btnSignUp).setOnClickListener(v -> doSignUp());
        findViewById(R.id.btnCancel).setOnClickListener(v -> finish());
    }

    private void doSignUp() {
        String email = text(edtEmail);
        String name  = text(edtName);
        String pass  = text(edtPassword);

        if (email.isEmpty() || name.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "Please fill all required fields.", Toast.LENGTH_SHORT).show();
            return;
        }

        // ✅ docId = name (exactly like your screenshot)
        String docId = name.trim();

        if (docId.contains("/")) {
            Toast.makeText(this, "Name cannot contain /", Toast.LENGTH_SHORT).show();
            return;
        }

        // ✅ ONLY parent data
        Map<String, Object> parentData = new HashMap<>();
        parentData.put("email", email);
        parentData.put("name", name);
        parentData.put("password", pass); // keeps your current login working

        db.collection(PARENTS_COLLECTION)
                .document(docId)
                .set(parentData)
                .addOnSuccessListener(v -> {
                    Toast.makeText(this, "Sign up successful!", Toast.LENGTH_SHORT).show();

                    // return to login screen
                    Intent i = new Intent(this, ParentLoginActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(i);
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    private String text(EditText e) {
        return (e.getText() == null) ? "" : e.getText().toString().trim();
    }
}
