package com.example.asdproject.view.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.asdproject.R;
import com.example.asdproject.util.LocaleManager;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Map;

public class ParentLoginActivity extends BaseActivity {

    private EditText etId;
    private EditText etName;
    private Button btnLogin;
    private TextView txtCancel;
    private TextView txtGoToSignUp;
    private TextView txtAddChild;

    private Button btnLanguage; // 🌐

    private FirebaseFirestore db;
    private static final String PARENTS_COLLECTION = "parents";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_login);

        db = FirebaseFirestore.getInstance();

        etId    = findViewById(R.id.edtId);
        etName  = findViewById(R.id.edtName);
        btnLogin = findViewById(R.id.btnLogin);
        txtCancel = findViewById(R.id.txtCancel);
        txtGoToSignUp = findViewById(R.id.txtGoToSignUp);
        txtAddChild = findViewById(R.id.txtAddChild);

        btnLanguage = findViewById(R.id.btnLanguage);

        btnLogin.setOnClickListener(v -> loginParent());
        txtCancel.setOnClickListener(v -> finish());

        txtGoToSignUp.setOnClickListener(v -> {
            Intent i = new Intent(ParentLoginActivity.this, ParentSignUpActivity.class);
            startActivity(i);
        });

        txtAddChild.setOnClickListener(v -> {
            Intent i = new Intent(ParentLoginActivity.this, AddChildActivity.class);
            startActivity(i);
        });

        // ✅ 🌐 toggle language
        btnLanguage.setOnClickListener(v -> {
            LocaleManager.toggleLanguage(this);
            recreate(); // refresh same screen with new language
        });
    }

    private void loginParent() {
        String idInput = etId.getText().toString().trim();
        String nameInput = etName.getText().toString().trim();

        if (idInput.isEmpty() || nameInput.isEmpty()) {
            Toast.makeText(this, getString(R.string.error_fill_all), Toast.LENGTH_SHORT).show();
            return;
        }

        if (idInput.length() != 9) {
            Toast.makeText(this, getString(R.string.error_id_9_digits), Toast.LENGTH_SHORT).show();
            return;
        }

        btnLogin.setEnabled(false);

        db.collection(PARENTS_COLLECTION)
                .document(idInput)
                .get()
                .addOnSuccessListener(doc -> handleParentDocForLogin(doc, nameInput))
                .addOnFailureListener(e -> {
                    btnLogin.setEnabled(true);
                    Toast.makeText(this, getString(R.string.error_firestore) + " " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void handleParentDocForLogin(DocumentSnapshot doc, String nameInput) {
        btnLogin.setEnabled(true);

        if (doc == null || !doc.exists()) {
            Toast.makeText(this, getString(R.string.error_not_found), Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> data = doc.getData();
        String dbName = (data != null) ? safeStr(data.get("name")) : null;

        boolean nameOk = (dbName != null && dbName.trim().equalsIgnoreCase(nameInput.trim()));
        if (!nameOk) {
            Toast.makeText(this, getString(R.string.error_wrong_name), Toast.LENGTH_SHORT).show();
            return;
        }

        Intent i = new Intent(ParentLoginActivity.this, ParentHomeActivity.class);
        i.putExtra("PARENT_ID", doc.getId());
        i.putExtra("PARENT_NAME", dbName);
        startActivity(i);
        finish();
    }

    private String safeStr(Object o) {
        return (o == null) ? null : o.toString();
    }
}