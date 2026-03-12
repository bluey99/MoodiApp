package com.example.asdproject.view.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.asdproject.R;
import com.example.asdproject.util.LocaleManager;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class AddChildActivity extends BaseActivity {

    private EditText edtParentName, edtParentId;
    private EditText edtChildName, edtChildId;

    private FirebaseFirestore db;

    // 🌐 language button
    private TextView btnLanguage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // apply saved language (same idea as login)
        LocaleManager.setLocale(this);

        setContentView(R.layout.activity_add_child);

        db = FirebaseFirestore.getInstance();

        edtParentName = findViewById(R.id.edtParentName);
        edtParentId   = findViewById(R.id.edtParentId);
        edtChildName  = findViewById(R.id.edtChildName);
        edtChildId    = findViewById(R.id.edtChildId);

        btnLanguage = findViewById(R.id.btnLanguage);
        if (btnLanguage != null) {
            btnLanguage.setOnClickListener(v -> {
                LocaleManager.toggleLanguage(this);
                recreate(); // refresh this screen texts
            });
        }

        findViewById(R.id.btnAddChild).setOnClickListener(v -> addChild());
        findViewById(R.id.btnCancel).setOnClickListener(v -> finish());
    }

    private void addChild() {

        String parentName = text(edtParentName);
        String parentId   = text(edtParentId);
        String childName  = text(edtChildName);
        String childId    = text(edtChildId);

        if (parentName.isEmpty() || parentId.isEmpty()
                || childName.isEmpty() || childId.isEmpty()) {

            Toast.makeText(this, getString(R.string.error_fill_all), Toast.LENGTH_SHORT).show();
            return;
        }

        if (parentId.length() != 9 || childId.length() != 9) {
            Toast.makeText(this, getString(R.string.error_id_9_digits), Toast.LENGTH_SHORT).show();
            return;
        }

        // 1️⃣ Verify parent exists and name matches
        db.collection("parents")
                .document(parentId)
                .get()
                .addOnSuccessListener(parentDoc -> {

                    if (!parentDoc.exists()) {
                        Toast.makeText(this, getString(R.string.error_not_found), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String dbParentName = parentDoc.getString("name");

                    if (dbParentName == null ||
                            !dbParentName.equalsIgnoreCase(parentName)) {

                        Toast.makeText(this, getString(R.string.error_wrong_name), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // ✅ check already linked
                    List<String> linked = (List<String>) parentDoc.get("linkedChildren");
                    if (linked != null && linked.contains(childId)) {
                        Toast.makeText(this, getString(R.string.addchild_already_added), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    verifyChildThenAdd(parentId, childName, childId);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, getString(R.string.error_firestore) + " " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
    }

    private void verifyChildThenAdd(String parentId, String childName, String childId) {

        db.collection("children")
                .whereEqualTo("childID", childId)
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {

                    if (snapshot.isEmpty()) {
                        Toast.makeText(this, getString(R.string.addchild_child_not_found), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    DocumentSnapshot childDoc = snapshot.getDocuments().get(0);

                    String dbChildName = childDoc.getString("name");
                    String dbParentId  = childDoc.getString("parentID");

                    if (dbChildName == null || !childName.equalsIgnoreCase(dbChildName)) {
                        Toast.makeText(this, getString(R.string.addchild_child_name_mismatch), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (dbParentId == null || !parentId.equals(dbParentId)) {
                        Toast.makeText(this, getString(R.string.addchild_child_not_linked), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    db.collection("parents")
                            .document(parentId)
                            .update("linkedChildren", FieldValue.arrayUnion(childId))
                            .addOnSuccessListener(v -> {
                                Toast.makeText(this, getString(R.string.addchild_success), Toast.LENGTH_SHORT).show();
                                finish();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, getString(R.string.error_firestore) + " " + e.getMessage(),
                                            Toast.LENGTH_LONG).show());
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, getString(R.string.error_firestore) + " " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
    }

    private String text(EditText e) {
        return e.getText() == null ? "" : e.getText().toString().trim();
    }
}