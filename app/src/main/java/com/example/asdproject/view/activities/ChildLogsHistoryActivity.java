package com.example.asdproject.view.activities;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.asdproject.R;
import com.example.asdproject.controller.FirebaseManager;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChildLogsHistoryActivity extends AppCompatActivity {

    private TableLayout table;
    private Button btnGoBack, btnFilterBy;

    private String childId;
    private String childName;

    private final SimpleDateFormat sdf =
            new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());

    private final List<TableRow> dataRows = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_child_logs_history);

        childId = getIntent().getStringExtra("CHILD_ID");
        childName = getIntent().getStringExtra("CHILD_NAME");

        if (childName != null && !childName.trim().isEmpty()) {
            setTitle("Child Logs – " + childName);
        } else {
            setTitle("Child Logs History");
        }

        table = findViewById(R.id.tableChildLogs);
        btnFilterBy = findViewById(R.id.btnFilterBy);
        btnGoBack = findViewById(R.id.btnGoBackChildLogs);

        btnGoBack.setOnClickListener(v -> finish());
        btnFilterBy.setOnClickListener(v -> showFilterDialog());

        if (childId == null || childId.trim().isEmpty()) {
            Toast.makeText(this, "Missing child id", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseManager.init(this);
        listenToChildLogsLive();
    }

    private void listenToChildLogsLive() {
        FirebaseFirestore db = FirebaseManager.getDb();

        db.collection("children")
                .document(childId)
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

                        String situation = safe(doc.getString("situation"));

                        // ✅ FILTER: show ONLY SELF logs (skip task logs)
                        if (isTaskLog(situation)) {
                            continue;
                        }

                        String location  = safe(doc.getString("location"));
                        String emotion   = safe(doc.getString("feeling"));
                        String note      = safe(doc.getString("note"));

                        Long intensityLong = doc.getLong("intensity");
                        int intensity = (intensityLong == null) ? 0 : intensityLong.intValue();

                        String photo = safe(doc.getString("photoUri"));
                        if (photo.isEmpty()) photo = safe(doc.getString("photoUrl"));
                        if (photo.isEmpty()) photo = "—";

                        Timestamp ts = doc.getTimestamp("timestamp");
                        long tsMillis = (ts == null) ? 0L : ts.toDate().getTime();
                        String tsText = (tsMillis == 0L) ? "" : sdf.format(new Date(tsMillis));

                        addRow(tsText, situation, location, emotion, String.valueOf(intensity), note, photo, tsMillis);
                    }
                });
    }

    private boolean isTaskLog(String situation) {
        return situation != null && situation.trim().toLowerCase().startsWith("task:");
    }

    private void clearDataRows() {
        int count = table.getChildCount();
        if (count > 1) {
            table.removeViews(1, count - 1);
        }
        dataRows.clear();
    }

    private void addRow(String timestamp,
                        String situation,
                        String location,
                        String emotion,
                        String intensity,
                        String note,
                        String photo,
                        long tsMillis) {

        TableRow row = new TableRow(this);

        row.addView(makeCell(timestamp));
        row.addView(makeCell(situation));
        row.addView(makeCell(location));
        row.addView(makeCell(emotion));
        row.addView(makeCell(intensity));
        row.addView(makeCell(note));
        row.addView(makeCell(photo));

        row.setTag(tsMillis);

        table.addView(row);
        dataRows.add(row);
    }

    private TextView makeCell(String text) {
        TextView tv = new TextView(this);
        tv.setText(text == null ? "" : text);
        tv.setPadding(10, 10, 10, 10);
        tv.setBackgroundResource(R.drawable.table_cell_bg);
        return tv;
    }

    private String safe(String s) {
        return (s == null) ? "" : s.trim();
    }

    // ---------------- FILTER UI ----------------

    private void showFilterDialog() {
        String[] options = {
                "Emotion",
                "Situation",
                "Location",
                "Timestamp",
                "Clear filters"
        };

        new AlertDialog.Builder(this)
                .setTitle("Filter by:")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) showEmotionFilterDialog();
                    else if (which == 1) showSituationFilterDialog();
                    else if (which == 2) showLocationFilterDialog();
                    else if (which == 3) showTimestampFilterDialog();
                    else showAllRows();
                })
                .show();
    }

    private void showAllRows() {
        for (TableRow r : dataRows) {
            r.setVisibility(View.VISIBLE);
        }
    }

    private void showEmotionFilterDialog() {
        String[] emotions = {"Happy", "Sad", "Angry", "Surprised", "Afraid", "Disgust", "Unsure", "Other..."};

        new AlertDialog.Builder(this)
                .setTitle("Choose emotion")
                .setItems(emotions, (dialog, which) -> {
                    String chosen = emotions[which];
                    if (chosen.equals("Other...")) {
                        showOtherEmotionInput();
                    } else {
                        filterByTextColumn(3, chosen);
                    }
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
                    filterByTextColumn(3, typed);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showSituationFilterDialog() {
        final android.widget.EditText input = new android.widget.EditText(this);
        input.setHint("Type situation (ex: School)");

        new AlertDialog.Builder(this)
                .setTitle("Filter by Situation")
                .setView(input)
                .setPositiveButton("Apply", (d, w) -> filterByTextColumn(1, input.getText().toString()))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showLocationFilterDialog() {
        final android.widget.EditText input = new android.widget.EditText(this);
        input.setHint("Type location (ex: Home)");

        new AlertDialog.Builder(this)
                .setTitle("Filter by Location")
                .setView(input)
                .setPositiveButton("Apply", (d, w) -> filterByTextColumn(2, input.getText().toString()))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void filterByTextColumn(int colIndex, String query) {
        String q = (query == null) ? "" : query.trim().toLowerCase();

        for (TableRow row : dataRows) {

            if (q.isEmpty()) {
                row.setVisibility(View.VISIBLE);
                continue;
            }

            View cellView = row.getChildAt(colIndex);
            if (!(cellView instanceof TextView)) {
                row.setVisibility(View.VISIBLE);
                continue;
            }

            String cellText = ((TextView) cellView).getText().toString().trim().toLowerCase();
            row.setVisibility(cellText.contains(q) ? View.VISIBLE : View.GONE);
        }
    }

    private void showTimestampFilterDialog() {
        String[] options = {"Newest -> Oldest", "Oldest -> Newest"};

        new AlertDialog.Builder(this)
                .setTitle("Sort by time")
                .setItems(options, (dialog, which) -> {
                    boolean newestFirst = (which == 0);
                    sortByTimestamp(newestFirst);
                })
                .show();
    }

    private void sortByTimestamp(boolean newestFirst) {
        dataRows.sort((r1, r2) -> {
            long t1 = (r1.getTag() instanceof Long) ? (Long) r1.getTag() : 0L;
            long t2 = (r2.getTag() instanceof Long) ? (Long) r2.getTag() : 0L;
            return newestFirst ? Long.compare(t2, t1) : Long.compare(t1, t2);
        });

        int count = table.getChildCount();
        if (count > 1) table.removeViews(1, count - 1);

        for (TableRow row : dataRows) {
            table.addView(row);
        }
    }
}
