// ===============================
// NewReportActivity.java (FIELD-ID ONLY) ✅
// ✅ childId is treated as childID FIELD (e.g., "214578903") always
// ✅ fetch child doc by: whereEqualTo("childID", childIdField)
// ✅ save locally using "childID" (capital D) so history filter matches
// ✅ still sends report to Firestore reports + therapist notification
// ✅ FIX: use finalTherapistId / finalChildName inside callbacks (no "effectively final" errors)
// ===============================

package com.example.asdproject.view.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.asdproject.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class NewReportActivity extends AppCompatActivity {

    private static final String PREFS = "reports_prefs";
    private static final String KEY_REPORTS_PREFIX = "reports_list_";

    private EditText edtSituation, edtDateTime, edtLocation,
            edtChildReaction, edtHowHandled, edtQuestionsTherapist;

    private final Calendar selectedDateTime = Calendar.getInstance();
    private final SimpleDateFormat dateTimeFormat =
            new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

    // ✅ this is the FIELD childID (not doc id)
    private String childIdField;
    private String childName;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_report);

        db = FirebaseFirestore.getInstance();

        Intent intent = getIntent();

        // accept multiple keys, but treat value as FIELD childID always
        childIdField = firstNonEmpty(
                intent.getStringExtra("CHILD_ID"),
                intent.getStringExtra("childID"),
                intent.getStringExtra("childId")
        );

        childName = firstNonEmpty(
                intent.getStringExtra("CHILD_NAME"),
                intent.getStringExtra("childName")
        );

        edtSituation          = findViewById(R.id.edtSituation);
        edtDateTime           = findViewById(R.id.edtDateTime);
        edtLocation           = findViewById(R.id.edtLocation);
        edtChildReaction      = findViewById(R.id.edtChildReaction);
        edtHowHandled         = findViewById(R.id.edtHowHandled);
        edtQuestionsTherapist = findViewById(R.id.edtQuestionsTherapist);

        Button btnViewHistory  = findViewById(R.id.btnViewHistory);
        Button btnSendReport   = findViewById(R.id.btnSendReport);
        Button btnGoBackReport = findViewById(R.id.btnGoBackReport);

        edtDateTime.setFocusable(false);
        edtDateTime.setClickable(true);
        edtDateTime.setLongClickable(false);
        edtDateTime.setOnClickListener(v -> showDateTimePicker());

        btnViewHistory.setOnClickListener(v -> {
            Intent i = new Intent(NewReportActivity.this, ReportsHistoryActivity.class);
            if (!isEmpty(childIdField)) {
                i.putExtra("CHILD_ID", childIdField);   // ✅ pass FIELD id
                i.putExtra("CHILD_NAME", childName);
            }
            startActivity(i);
        });

        btnSendReport.setOnClickListener(v -> sendReport());
        btnGoBackReport.setOnClickListener(v -> finish());
    }

    // --------- DATE + TIME PICKERS (no future allowed) ----------

    private void showDateTimePicker() {
        int year  = selectedDateTime.get(Calendar.YEAR);
        int month = selectedDateTime.get(Calendar.MONTH);
        int day   = selectedDateTime.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dp = new DatePickerDialog(
                this,
                (view, y, m, d) -> {

                    Calendar chosen = Calendar.getInstance();
                    chosen.set(y, m, d, 0, 0, 0);
                    chosen.set(Calendar.MILLISECOND, 0);

                    Calendar today = Calendar.getInstance();
                    today.set(Calendar.HOUR_OF_DAY, 0);
                    today.set(Calendar.MINUTE, 0);
                    today.set(Calendar.SECOND, 0);
                    today.set(Calendar.MILLISECOND, 0);

                    if (chosen.after(today)) {
                        Toast.makeText(this,
                                "You cannot choose a future date!",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    selectedDateTime.set(Calendar.YEAR, y);
                    selectedDateTime.set(Calendar.MONTH, m);
                    selectedDateTime.set(Calendar.DAY_OF_MONTH, d);

                    showTimePicker();
                },
                year, month, day
        );

        dp.getDatePicker().setMaxDate(System.currentTimeMillis());
        dp.show();
    }

    private void showTimePicker() {
        int hour   = selectedDateTime.get(Calendar.HOUR_OF_DAY);
        int minute = selectedDateTime.get(Calendar.MINUTE);

        TimePickerDialog tp = new TimePickerDialog(
                this,
                (view, h, m) -> {

                    Calendar chosen = (Calendar) selectedDateTime.clone();
                    chosen.set(Calendar.HOUR_OF_DAY, h);
                    chosen.set(Calendar.MINUTE, m);
                    chosen.set(Calendar.SECOND, 0);
                    chosen.set(Calendar.MILLISECOND, 0);

                    Calendar now = Calendar.getInstance();

                    Calendar today = Calendar.getInstance();
                    if (chosen.get(Calendar.YEAR)  == today.get(Calendar.YEAR) &&
                            chosen.get(Calendar.MONTH) == today.get(Calendar.MONTH) &&
                            chosen.get(Calendar.DAY_OF_MONTH) == today.get(Calendar.DAY_OF_MONTH)) {

                        if (chosen.after(now)) {
                            Toast.makeText(this,
                                    "You cannot choose a future time!",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }

                    selectedDateTime.set(Calendar.HOUR_OF_DAY, h);
                    selectedDateTime.set(Calendar.MINUTE, m);

                    String formatted = dateTimeFormat.format(selectedDateTime.getTime());
                    edtDateTime.setText(formatted);
                },
                hour, minute,
                true
        );

        tp.show();
    }

    private String getReportsKeyForCurrentParent() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String uid = (user != null) ? user.getUid() : "guest";
        return KEY_REPORTS_PREFIX + uid;
    }

    private void sendReport() {
        String situation = edtSituation.getText().toString().trim();
        String timestamp = edtDateTime.getText().toString().trim();
        String location  = edtLocation.getText().toString().trim();

        String childReaction = edtChildReaction.getText().toString().trim();
        String howHandled    = edtHowHandled.getText().toString().trim();
        String questions     = edtQuestionsTherapist.getText().toString().trim();

        if (situation.isEmpty() || timestamp.isEmpty()
                || location.isEmpty() || childReaction.isEmpty()
                || howHandled.isEmpty()) {

            Toast.makeText(this,
                    "Please fill all the required fields!!",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (isEmpty(childIdField)) {
            Toast.makeText(this, "Missing childID", Toast.LENGTH_SHORT).show();
            return;
        }

        // ✅ Always fetch by FIELD childID
        db.collection("children")
                .whereEqualTo("childID", childIdField)
                .limit(1)
                .get()
                .addOnSuccessListener(qs -> {
                    if (qs == null || qs.isEmpty()) {
                        Toast.makeText(this, "Child not found (childID).", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Avoid "var" to prevent Java version issues
                    com.google.firebase.firestore.DocumentSnapshot childDoc =
                            qs.getDocuments().get(0);

                    String therapistId = childDoc.getString("therapistID"); // children doc uses therapistID
                    if (therapistId == null) therapistId = "";
                    therapistId = therapistId.trim();

                    String parentIdField = childDoc.getString("parentID");
                    if (parentIdField == null) parentIdField = "";
                    parentIdField = parentIdField.trim();

                    // ✅ Save locally (field id only)
                    saveFullReportLocal(situation, timestamp, location, childReaction, howHandled, questions);

                    // ✅ Send to Firestore reports (for therapist history)
                    if (!therapistId.isEmpty()) {

                        // ✅ FIX: make them final for async callbacks
                        final String finalTherapistId = therapistId;
                        final String finalChildName = childName;

                        Map<String, Object> report = new HashMap<>();
                        report.put("childName", finalChildName);
                        report.put("therapistId", finalTherapistId); // reports collection uses therapistId
                        report.put("situation", situation);
                        report.put("timestamp", timestamp);          // string time (when it happened)
                        report.put("location", location);
                        report.put("childReaction", childReaction);
                        report.put("howHandled", howHandled);
                        report.put("questions", questions);

                        report.put("parentID", parentIdField);
                        report.put("childID", childIdField);         // ✅ field id

                        db.collection("reports")
                                .add(report)
                                .addOnSuccessListener(docRef -> {

                                    Map<String, Object> notif = new HashMap<>();
                                    notif.put("receiverType", "THERAPIST");
                                    notif.put("receiverId", finalTherapistId);
                                    notif.put("read", false);

                                    String cn = (finalChildName == null || finalChildName.trim().isEmpty())
                                            ? "Child" : finalChildName.trim();
                                    notif.put("message", cn + " has a new report from parent.");

                                    notif.put("createdAt", System.currentTimeMillis());
                                    db.collection("notifications").add(notif);

                                    Toast.makeText(this, "Report sent", Toast.LENGTH_SHORT).show();
                                    finish();
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                                );

                    } else {
                        Toast.makeText(this, "Report saved (no therapist linked).", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    // ✅ Local save uses "childID" (capital D) for consistent filtering
    private void saveFullReportLocal(String situation, String timestamp, String location,
                                     String childReaction, String howHandled, String questions) {

        String key = getReportsKeyForCurrentParent();

        try {
            String json = getSharedPreferences(PREFS, MODE_PRIVATE)
                    .getString(key, "[]");

            JSONArray arr = new JSONArray(json);

            JSONObject obj = new JSONObject();
            obj.put("situation",     situation);
            obj.put("timestamp",     timestamp);
            obj.put("location",      location);
            obj.put("childReaction", childReaction);
            obj.put("howHandled",    howHandled);
            obj.put("questions",     questions);

            // ✅ store FIELD id (capital D)
            obj.put("childID",   childIdField);
            obj.put("childName", childName);

            arr.put(obj);

            getSharedPreferences(PREFS, MODE_PRIVATE)
                    .edit()
                    .putString(key, arr.toString())
                    .apply();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }

    private String firstNonEmpty(String... vals) {
        if (vals == null) return null;
        for (String v : vals) {
            if (!isEmpty(v)) return v.trim();
        }
        return null;
    }
}
