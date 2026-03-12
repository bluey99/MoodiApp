package com.example.asdproject.view.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputFilter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.asdproject.R;
import com.example.asdproject.util.LocaleManager;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ParentSignUpActivity extends BaseActivity {

    private EditText edtName, edtId, edtChildName, edtChildId;
    private FirebaseFirestore db;

    private static final String PARENTS_COLLECTION = "parents";
    private static final String CHILDREN_COLLECTION = "children";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_sign_up);

        db = FirebaseFirestore.getInstance();

        edtName = findViewById(R.id.edtName);
        edtId   = findViewById(R.id.edtId);
        edtChildName = findViewById(R.id.edtChildName);
        edtChildId   = findViewById(R.id.edtChildId);

        edtId.setFilters(new InputFilter[]{ new InputFilter.LengthFilter(9) });
        edtChildId.setFilters(new InputFilter[]{ new InputFilter.LengthFilter(9) });

        findViewById(R.id.btnSignUp).setOnClickListener(v -> doSignUp());
        findViewById(R.id.btnCancel).setOnClickListener(v -> finish());
        Button btnLanguage = findViewById(R.id.btnLanguage);
        btnLanguage.setOnClickListener(v -> {
            LocaleManager.toggleLanguage(this);
            recreate();
        });
    }

    private void doSignUp() {

        String parentName = text(edtName);
        String parentId   = text(edtId);
        String childName  = text(edtChildName);
        String childId    = text(edtChildId);

        if (parentName.isEmpty() || parentId.isEmpty()
                || childName.isEmpty() || childId.isEmpty()) {

            Toast.makeText(this, getString(R.string.error_fill_all), Toast.LENGTH_SHORT).show();
            return;
        }

        if (parentId.length() != 9) {
            Toast.makeText(this, getString(R.string.error_parent_id_9_digits), Toast.LENGTH_SHORT).show();
            return;
        }

        if (childId.length() != 9) {
            Toast.makeText(this, getString(R.string.error_child_id_9_digits), Toast.LENGTH_SHORT).show();
            return;
        }

        verifyChildThenCreateParent(parentName, parentId, childName, childId);
    }

    private void verifyChildThenCreateParent(String parentName,
                                             String parentId,
                                             String childName,
                                             String childId) {

        db.collection(CHILDREN_COLLECTION)
                .whereEqualTo("childID", childId)
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {

                    if (snapshot.isEmpty()) {
                        Toast.makeText(this, getString(R.string.error_child_not_found), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    QueryDocumentSnapshot doc =
                            (QueryDocumentSnapshot) snapshot.getDocuments().get(0);

                    String dbChildName = doc.getString("name");
                    String dbParentId  = doc.getString("parentID");

                    boolean nameOk = dbChildName != null &&
                            dbChildName.trim().equalsIgnoreCase(childName.trim());

                    boolean parentOk = dbParentId != null &&
                            dbParentId.trim().equals(parentId.trim());

                    if (!nameOk) {
                        Toast.makeText(this, getString(R.string.error_child_name_mismatch), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (!parentOk) {
                        Toast.makeText(this, getString(R.string.error_child_not_linked_to_parent), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    createParent(parentName, parentId, childId);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                getString(R.string.error_verification_failed) + " " + e.getMessage(),
                                Toast.LENGTH_LONG).show()
                );
    }

    private void createParent(String parentName,
                              String parentId,
                              String firstChildId) {

        String docId = parentId.trim();

        Map<String, Object> parentData = new HashMap<>();
        parentData.put("ID", parentId);
        parentData.put("name", parentName);

        // 🔥 NEW: store first linked child inside array
        parentData.put("linkedChildren", Arrays.asList(firstChildId));

        db.collection(PARENTS_COLLECTION)
                .document(docId)
                .set(parentData)
                .addOnSuccessListener(v -> {
                    Toast.makeText(this, getString(R.string.signup_success), Toast.LENGTH_SHORT).show();

                    Intent i = new Intent(this, ParentLoginActivity.class);
                    startActivity(i);
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                getString(R.string.error_failed_prefix) + " " + e.getMessage(),
                                Toast.LENGTH_LONG).show()
                );
    }

    private String text(EditText e) {
        return (e.getText() == null) ? "" :
                e.getText().toString().trim();
    }
}