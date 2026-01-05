package com.example.asdproject.view.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.example.asdproject.R;

public class EmotionHistoryActivity extends AppCompatActivity {

    private TableLayout table;
    private Button btnGoBack, btnFilterBy;

    private String childId;
    private String childName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emotion_history);

        // child context
        childId = getIntent().getStringExtra("CHILD_ID");
        childName = getIntent().getStringExtra("CHILD_NAME");

        if (childName != null && !childName.isEmpty()) {
            setTitle("History – " + childName);
        }

        table = findViewById(R.id.tableEmotionHistory);
        btnGoBack = findViewById(R.id.btnGoBackEmotion);
        btnFilterBy = findViewById(R.id.btnFilterBy);

        btnGoBack.setOnClickListener(v -> finish());
        btnFilterBy.setOnClickListener(v -> showFilterDialog());

        // Note:
        // Later if you connect this to Firestore, you'll use childId
        // to load only this child's history.
    }

    private void showFilterDialog() {
        String[] options = {
                "Emotion",
                "Task Name",
                "Timestamp",
                "Clear filters"
        };

        new AlertDialog.Builder(this)
                .setTitle("Filter by:")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) { showEmotionFilterDialog(); }
                    else if (which == 1) { showTaskNameFilterDialog(); }
                    else if (which == 2) { showTimestampFilterDialog(); }
                    else if (which == 3) { showAllRows(); }
                })
                .show();
    }

    private void showAllRows() {
        for (int i = 0; i < table.getChildCount(); i++) {
            android.view.View v = table.getChildAt(i);
            if (v instanceof TableRow) {
                v.setVisibility(android.view.View.VISIBLE);
            }
        }
    }

    private void showEmotionFilterDialog() {
        String[] emotions = {"Happy", "Sad", "Angry", "Calm", "Anxious", "Other..."};

        new AlertDialog.Builder(this)
                .setTitle("Choose emotion")
                .setItems(emotions, (dialog, which) -> {
                    String chosen = emotions[which];

                    if (chosen.equals("Other...")) {
                        showOtherEmotionInput();
                    } else {
                        filterTableByEmotion(chosen);
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

                    filterTableByEmotion(typed);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void filterTableByEmotion(String chosenEmotion) {
        final int EMOTION_COL_INDEX = 2;

        for (int i = 0; i < table.getChildCount(); i++) {
            android.view.View v = table.getChildAt(i);

            if (!(v instanceof TableRow)) continue;

            TableRow row = (TableRow) v;

            // header stays visible
            if (i == 0) {
                row.setVisibility(android.view.View.VISIBLE);
                continue;
            }

            if (row.getChildCount() <= EMOTION_COL_INDEX) continue;

            android.view.View cellView = row.getChildAt(EMOTION_COL_INDEX);
            if (!(cellView instanceof TextView)) continue;

            String rowEmotion = ((TextView) cellView).getText().toString().trim();

            if (rowEmotion.equalsIgnoreCase(chosenEmotion)) {
                row.setVisibility(android.view.View.VISIBLE);
            } else {
                row.setVisibility(android.view.View.GONE);
            }
        }
    }

    private void showTaskNameFilterDialog() {
        final android.widget.EditText input = new android.widget.EditText(this);
        input.setHint("Type task name (ex: home, walk...)");

        new AlertDialog.Builder(this)
                .setTitle("Search by Task Name")
                .setView(input)
                .setPositiveButton("Search", (d, w) -> {
                    String q = input.getText().toString();
                    filterTableByTaskName(q);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void filterTableByTaskName(String query) {
        final int TASK_COL_INDEX = 1; // 0 Timestamp, 1 Task Name, 2 Emotion, 3 Level, 4 Notes

        String q = (query == null) ? "" : query.trim().toLowerCase();

        for (int i = 0; i < table.getChildCount(); i++) {
            android.view.View v = table.getChildAt(i);
            if (!(v instanceof TableRow)) continue;

            TableRow row = (TableRow) v;

            // keep header visible
            if (i == 0) {
                row.setVisibility(android.view.View.VISIBLE);
                continue;
            }

            if (q.isEmpty()) {
                row.setVisibility(android.view.View.VISIBLE);
                continue;
            }

            if (row.getChildCount() <= TASK_COL_INDEX) continue;

            android.view.View cellView = row.getChildAt(TASK_COL_INDEX);
            if (!(cellView instanceof TextView)) continue;

            String taskText = ((TextView) cellView).getText().toString().trim().toLowerCase();

            if (taskText.contains(q)) {
                row.setVisibility(android.view.View.VISIBLE);
            } else {
                row.setVisibility(android.view.View.GONE);
            }
        }
    }

    private void showTimestampFilterDialog() {
        String[] options = {"Newest -> Oldest", "Oldest -> Newest"};

        new AlertDialog.Builder(this)
                .setTitle("Sort by time")
                .setItems(options, (dialog, which) -> {
                    boolean newestFirst = (which == 0);
                    sortTableByTimestamp(newestFirst);
                })
                .show();
    }

    private void sortTableByTimestamp(boolean newestFirst) {
        final int TIMESTAMP_COL_INDEX = 0; // first column

        java.util.List<TableRow> rows = new java.util.ArrayList<>();

        // collect data rows (skip header)
        for (int i = 1; i < table.getChildCount(); i++) {
            android.view.View v = table.getChildAt(i);
            if (v instanceof TableRow) {
                rows.add((TableRow) v);
            }
        }

        rows.sort((r1, r2) -> {
            android.view.View v1 = r1.getChildAt(TIMESTAMP_COL_INDEX);
            android.view.View v2 = r2.getChildAt(TIMESTAMP_COL_INDEX);

            if (!(v1 instanceof TextView) || !(v2 instanceof TextView))
                return 0;

            String t1 = ((TextView) v1).getText().toString();
            String t2 = ((TextView) v2).getText().toString();

            int cmp = t1.compareTo(t2);
            return newestFirst ? -cmp : cmp;
        });

        table.removeViews(1, table.getChildCount() - 1);

        for (TableRow row : rows) {
            table.addView(row);
        }
    }
}