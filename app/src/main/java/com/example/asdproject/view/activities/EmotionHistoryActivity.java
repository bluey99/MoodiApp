// ===============================
// EmotionHistoryActivity.java
// (TASK logs only: situation starts with "task:")
// ===============================

package com.example.asdproject.view.activities;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.example.asdproject.R;
import com.example.asdproject.controller.FirebaseManager;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EmotionHistoryActivity extends AppCompatActivity {

    private TableLayout table;
    private Button btnGoBack, btnFilterBy;

    // Can be Firestore docId OR the childID field value (e.g. "214578903")
    private String childIdOrField;
    // Always the real Firestore document id after resolving
    private String childDocId;

    private String childName;

    private ListenerRegistration historyReg;

    private final SimpleDateFormat sdf =
            new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());

    private final List<TableRow> dataRows = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emotion_history);

        // ✅ robust: accept multiple keys after refactor
        childIdOrField = firstNonEmpty(
                getIntent().getStringExtra("CHILD_ID"),
                getIntent().getStringExtra("childId"),
                getIntent().getStringExtra("focusId")
        );

        childName = firstNonEmpty(
                getIntent().getStringExtra("CHILD_NAME"),
                getIntent().getStringExtra("childName")
        );

        if (!isEmpty(childName)) {
            setTitle("Tasks History – " + childName);
        }

        table = findViewById(R.id.tableEmotionHistory);
        btnGoBack = findViewById(R.id.btnGoBackEmotion);
        btnFilterBy = findViewById(R.id.btnFilterBy);

        btnGoBack.setOnClickListener(v -> finish());
        btnFilterBy.setOnClickListener(v -> showFilterDialog());

        if (isEmpty(childIdOrField)) {
            Toast.makeText(this, "Missing child id", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        FirebaseManager.init(this);

        // ✅ critical fix: resolve the actual Firestore child document id
        resolveChildDocIdAndListen();
    }

    // ==========================================================
    // ✅ Resolve: handle BOTH cases:
    // 1) childIdOrField is already the document id
    // 2) childIdOrField is the stored field "childID" (capital D!)
    // ==========================================================
    private void resolveChildDocIdAndListen() {
        FirebaseFirestore db = FirebaseManager.getDb();

        // 1) Try assuming it's already the document id
        db.collection("children")
                .document(childIdOrField)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc != null && doc.exists()) {
                        childDocId = doc.getId();
                        listenToHistoryLive();
                    } else {
                        // 2) Otherwise search by FIELD id (Firestore shows: childID)
                        findChildDocIdByField(db);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void findChildDocIdByField(FirebaseFirestore db) {
        // Try field name as shown in your Firestore: childID (capital D)
        db.collection("children")
                .whereEqualTo("childID", childIdOrField)
                .limit(1)
                .get()
                .addOnSuccessListener(qs -> {
                    if (qs != null && !qs.isEmpty()) {
                        childDocId = qs.getDocuments().get(0).getId();
                        listenToHistoryLive();
                    } else {
                        // fallback if some docs use childId instead
                        db.collection("children")
                                .whereEqualTo("childId", childIdOrField)
                                .limit(1)
                                .get()
                                .addOnSuccessListener(qs2 -> {
                                    if (qs2 != null && !qs2.isEmpty()) {
                                        childDocId = qs2.getDocuments().get(0).getId();
                                        listenToHistoryLive();
                                    } else {
                                        Toast.makeText(this,
                                                "Child not found (docId or childID field).",
                                                Toast.LENGTH_LONG).show();
                                        finish();
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    finish();
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    // ==========================================================
    // ✅ LIVE LISTENER
    // ==========================================================
    private void listenToHistoryLive() {
        FirebaseFirestore db = FirebaseManager.getDb();

        if (historyReg != null) historyReg.remove();

        historyReg = db.collection("children")
                .document(childDocId) // ✅ use resolved doc id
                .collection("history")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((snap, e) -> {

                    if (e != null) {
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (snap == null) return;

                    clearDataRows();

                    for (DocumentSnapshot doc : snap.getDocuments()) {

                        // ✅ FILTER: show ONLY TASK logs
                        String situation = safe(doc.getString("situation"));
                        if (!isTaskLog(situation)) continue;

                        String emotion = safe(doc.getString("feeling"));
                        Long intensityLong = doc.getLong("intensity");
                        int intensity = (intensityLong == null) ? 0 : intensityLong.intValue();
                        String note = safe(doc.getString("note"));

                        String taskName = safe(doc.getString("taskName"));
                        if (taskName.isEmpty()) taskName = deriveTaskNameFromSituation(situation);
                        if (taskName.isEmpty()) taskName = "—";

                        Timestamp ts = doc.getTimestamp("timestamp");
                        long tsMillis = (ts == null) ? 0L : ts.toDate().getTime();
                        String tsText = (tsMillis == 0L) ? "" : sdf.format(new Date(tsMillis));

                        addRow(tsText, taskName, emotion, String.valueOf(intensity), note, tsMillis);
                    }
                });
    }

    private boolean isTaskLog(String situation) {
        return situation != null && situation.trim().toLowerCase().startsWith("task:");
    }

    private String deriveTaskNameFromSituation(String situation) {
        if (situation == null) return "";
        String s = situation.trim();
        if (s.toLowerCase().startsWith("task:")) {
            return s.substring(5).trim(); // remove "task:"
        }
        return "";
    }

    private void clearDataRows() {
        int count = table.getChildCount();
        if (count > 1) {
            table.removeViews(1, count - 1);
        }
        dataRows.clear();
    }

    // ==========================================================
    // ✅ TABLE ROW + CELLS (your style kept)
    // ==========================================================
    private void addRow(String timestamp, String taskName, String emotion,
                        String intensity, String note, long tsMillis) {

        TableRow row = new TableRow(this);

        // allow cells to stretch to row height
        row.setBaselineAligned(false);

        row.setLayoutParams(new TableLayout.LayoutParams(
                TableLayout.LayoutParams.WRAP_CONTENT,
                TableLayout.LayoutParams.WRAP_CONTENT
        ));

        row.addView(makeCell(timestamp, 1.3f, false));
        row.addView(makeCell(taskName, 1.3f, false));
        row.addView(makeCell(emotion,  0.9f, false));
        row.addView(makeCell(intensity,0.7f, false));
        row.addView(makeCell(note,     2.6f, true));

        row.setTag(tsMillis);

        table.addView(row);
        dataRows.add(row);
    }

    private TextView makeCell(String text, float weight, boolean isNotes) {
        TextView tv = new TextView(this);
        tv.setText(text == null ? "" : text);

        TableRow.LayoutParams lp =
                new TableRow.LayoutParams(
                        0,
                        TableRow.LayoutParams.MATCH_PARENT,
                        weight
                );
        tv.setLayoutParams(lp);

        tv.setPadding(12, 12, 12, 12);
        tv.setBackgroundResource(R.drawable.table_cell_bg);

        tv.setGravity(Gravity.CENTER_VERTICAL);
        tv.setSingleLine(false);
        tv.setHorizontallyScrolling(false);

        if (isNotes) {
            tv.setMaxLines(Integer.MAX_VALUE);
            tv.setEllipsize(null);
        } else {
            tv.setMaxLines(3);
            tv.setEllipsize(TextUtils.TruncateAt.END);
        }

        return tv;
    }

    private String safe(String s) {
        return (s == null) ? "" : s.trim();
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

    // ---------------- FILTER UI ----------------

    private void showFilterDialog() {
        String[] options = {"Emotion", "Task Name", "Timestamp", "Clear filters"};

        new AlertDialog.Builder(this)
                .setTitle("Filter by:")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) { showEmotionFilterDialog(); }
                    else if (which == 1) { showTaskNameFilterDialog(); }
                    else if (which == 2) { showTimestampFilterDialog(); }
                    else { showAllRows(); }
                })
                .show();
    }

    private void showAllRows() {
        for (TableRow r : dataRows) r.setVisibility(View.VISIBLE);
    }

    private void showEmotionFilterDialog() {
        String[] emotions = {"Happy", "Sad", "Angry", "Surprised", "Afraid", "Disgust", "Unsure", "Other..."};

        new AlertDialog.Builder(this)
                .setTitle("Choose emotion")
                .setItems(emotions, (dialog, which) -> {
                    String chosen = emotions[which];
                    if (chosen.equals("Other...")) showOtherEmotionInput();
                    else filterTableByEmotion(chosen);
                })
                .show();
    }

    private void showOtherEmotionInput() {
        final android.widget.EditText input = new android.widget.EditText(this);
        input.setHint("Type an emotion (e.g., Excited)");

        new AlertDialog.Builder(this)
                .setTitle("Other emotion")
                .setView(input)
                .setPositiveButton("Apply", (d, w) -> {
                    String typed = input.getText().toString().trim();
                    if (typed.isEmpty()) {
                        Toast.makeText(this, "Please enter an emotion", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    filterTableByEmotion(typed);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void filterTableByEmotion(String chosenEmotion) {
        final int EMOTION_COL_INDEX = 2;

        for (TableRow row : dataRows) {
            TextView cell = (TextView) row.getChildAt(EMOTION_COL_INDEX);
            String rowEmotion = cell.getText().toString().trim();
            row.setVisibility(rowEmotion.equalsIgnoreCase(chosenEmotion) ? View.VISIBLE : View.GONE);
        }
    }

    private void showTaskNameFilterDialog() {
        final android.widget.EditText input = new android.widget.EditText(this);
        input.setHint("Type task name (ex: homework, walk...)");

        new AlertDialog.Builder(this)
                .setTitle("Search by Task Name")
                .setView(input)
                .setPositiveButton("Search", (d, w) -> filterTableByTaskName(input.getText().toString()))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void filterTableByTaskName(String query) {
        final int TASK_COL_INDEX = 1;

        String q = (query == null) ? "" : query.trim().toLowerCase();

        for (TableRow row : dataRows) {
            TextView cell = (TextView) row.getChildAt(TASK_COL_INDEX);
            String taskText = cell.getText().toString().trim().toLowerCase();
            row.setVisibility(q.isEmpty() || taskText.contains(q) ? View.VISIBLE : View.GONE);
        }
    }

    private void showTimestampFilterDialog() {
        String[] options = {"Newest -> Oldest", "Oldest -> Newest"};

        new AlertDialog.Builder(this)
                .setTitle("Sort by time")
                .setItems(options, (dialog, which) -> sortTableByTimestamp(which == 0))
                .show();
    }

    private void sortTableByTimestamp(boolean newestFirst) {
        dataRows.sort((r1, r2) -> {
            long t1 = (r1.getTag() instanceof Long) ? (Long) r1.getTag() : 0L;
            long t2 = (r2.getTag() instanceof Long) ? (Long) r2.getTag() : 0L;
            return newestFirst ? Long.compare(t2, t1) : Long.compare(t1, t2);
        });

        int count = table.getChildCount();
        if (count > 1) table.removeViews(1, count - 1);
        for (TableRow row : dataRows) table.addView(row);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (historyReg != null) historyReg.remove();
    }
}
