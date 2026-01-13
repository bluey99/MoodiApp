package com.example.asdproject.view.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.widget.*;

import com.example.asdproject.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;

public class ReportsHistoryActivity extends AppCompatActivity {

    private static final String PREFS = "reports_prefs";
    private static final String KEY_REPORTS_PREFIX = "reports_list_";

    private TableLayout table;

    private static final int COL_SITUATION = 0;
    private static final int COL_TIMESTAMP = 1;
    private static final int COL_LOCATION = 2;

    private String childId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reports_history);

        table = findViewById(R.id.tableReportsHistory);

        childId = getIntent().getStringExtra("CHILD_ID");

        findViewById(R.id.btnGoBackReports).setOnClickListener(v -> finish());
        findViewById(R.id.btnFilterReports).setOnClickListener(v -> showFilterDialog());

        loadTable();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTable();
    }

    private String getReportsKeyForCurrentParent() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String uid = (user != null) ? user.getUid() : "guest";
        return KEY_REPORTS_PREFIX + uid;
    }

    private void loadTable() {
        if (table.getChildCount() > 1)
            table.removeViews(1, table.getChildCount() - 1);

        String key = getReportsKeyForCurrentParent();

        try {
            String json = getSharedPreferences(PREFS, MODE_PRIVATE)
                    .getString(key, "[]");

            JSONArray arr = new JSONArray(json);

            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);

                if (childId != null && !childId.isEmpty()) {
                    String reportChildId = o.optString("childId", null);
                    if (reportChildId == null || !childId.equals(reportChildId)) {
                        continue;
                    }
                }

                addRow(
                        i,
                        o.optString("situation"),
                        o.optString("timestamp"),
                        o.optString("location")
                );
            }
        } catch (Exception ignored) {
        }
    }

    private void addRow(int index, String situation, String timestamp, String location) {
        TableRow row = new TableRow(this);

        row.addView(cell(situation, 160));
        row.addView(cell(timestamp, 150));
        row.addView(cell(location, 120));

        TextView edit = cell("✏️", 80);
        edit.setOnClickListener(v -> openEditDialog(index));

        TextView del = cell("🗑️", 90);
        del.setOnClickListener(v -> deleteReport(index));

        row.addView(edit);
        row.addView(del);

        table.addView(row);
    }

    private TextView cell(String text, int widthDp) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(10, 10, 10, 10);
        tv.setBackgroundResource(R.drawable.table_cell_bg);
        tv.setLayoutParams(new TableRow.LayoutParams(dp(widthDp), dp(44)));
        return tv;
    }

    private int dp(int d) {
        return (int) (d * getResources().getDisplayMetrics().density);
    }

    private void deleteReport(int index) {
        String key = getReportsKeyForCurrentParent();
        try {
            JSONArray arr = new JSONArray(
                    getSharedPreferences(PREFS, MODE_PRIVATE)
                            .getString(key, "[]")
            );

            JSONArray newArr = new JSONArray();
            for (int i = 0; i < arr.length(); i++)
                if (i != index) newArr.put(arr.getJSONObject(i));

            getSharedPreferences(PREFS, MODE_PRIVATE)
                    .edit()
                    .putString(key, newArr.toString())
                    .apply();

            loadTable();
        } catch (Exception ignored) {
        }
    }

    private void openEditDialog(int index) {
        String key = getReportsKeyForCurrentParent();

        try {
            JSONArray arr = new JSONArray(
                    getSharedPreferences(PREFS, MODE_PRIVATE)
                            .getString(key, "[]")
            );

            JSONObject o = arr.getJSONObject(index);

            LinearLayout layout = new LinearLayout(this);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setPadding(20, 10, 20, 10);

            EditText s = input("Situation", o.optString("situation"));
            EditText t = input("Timestamp", o.optString("timestamp"));
            EditText l = input("Location", o.optString("location"));
            EditText r = input("Child’s reaction", o.optString("childReaction"));
            EditText h = input("How handled", o.optString("howHandled"));
            EditText q = input("Questions for therapist (optional)", o.optString("questions"));

            layout.addView(s);
            layout.addView(t);
            layout.addView(l);
            layout.addView(r);
            layout.addView(h);
            layout.addView(q);

            new AlertDialog.Builder(this)
                    .setTitle("Edit report")
                    .setView(layout)
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Save", (d, w) -> {
                        try {
                            o.put("situation", s.getText().toString().trim());
                            o.put("timestamp", t.getText().toString().trim());
                            o.put("location", l.getText().toString().trim());
                            o.put("childReaction", r.getText().toString().trim());
                            o.put("howHandled", h.getText().toString().trim());
                            o.put("questions", q.getText().toString().trim());
                            arr.put(index, o);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        getSharedPreferences(PREFS, MODE_PRIVATE)
                                .edit()
                                .putString(key, arr.toString())
                                .apply();

                        loadTable();
                    })
                    .show();

        } catch (Exception ignored) {
        }
    }

    private EditText input(String hint, String val) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setText(val);
        e.setInputType(InputType.TYPE_CLASS_TEXT);
        return e;
    }

    private void showFilterDialog() {
        String[] opts = {"Situation Name", "Location", "Timestamp", "Clear filters"};

        new AlertDialog.Builder(this)
                .setTitle("Filter by")
                .setItems(opts, (dialog, which) -> {
                    if (which == 0) {
                        textFilter(COL_SITUATION, "Situation");
                    } else if (which == 1) {
                        textFilter(COL_LOCATION, "Location");
                    } else if (which == 2) {
                        showTimestampFilterDialog();
                    } else if (which == 3) {
                        clearFilters();
                    }
                })
                .show();
    }

    private void clearFilters() {
        loadTable();
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
        final int TIMESTAMP_COL_INDEX = 0;

        java.util.List<android.widget.TableRow> rows = new java.util.ArrayList<>();

        for (int i = 1; i < table.getChildCount(); i++) {
            android.view.View v = table.getChildAt(i);
            if (v instanceof android.widget.TableRow) {
                rows.add((android.widget.TableRow) v);
            }
        }

        rows.sort((r1, r2) -> {
            android.view.View v1 = r1.getChildAt(TIMESTAMP_COL_INDEX);
            android.view.View v2 = r2.getChildAt(TIMESTAMP_COL_INDEX);

            if (!(v1 instanceof android.widget.TextView) || !(v2 instanceof android.widget.TextView))
                return 0;

            String t1 = ((android.widget.TextView) v1).getText().toString();
            String t2 = ((android.widget.TextView) v2).getText().toString();

            int cmp = t1.compareTo(t2);
            return newestFirst ? -cmp : cmp;
        });

        table.removeViews(1, table.getChildCount() - 1);

        for (android.widget.TableRow row : rows) {
            table.addView(row);
        }
    }

    private void textFilter(int col, String title) {
        EditText input = new EditText(this);
        new AlertDialog.Builder(this)
                .setTitle("Search by " + title)
                .setView(input)
                .setPositiveButton("Search", (d, w) -> {
                    String q = input.getText().toString().toLowerCase();
                    for (int i = 1; i < table.getChildCount(); i++) {
                        TableRow r = (TableRow) table.getChildAt(i);
                        String txt = ((TextView) r.getChildAt(col)).getText().toString().toLowerCase();
                        r.setVisibility(txt.contains(q) ? TableRow.VISIBLE : TableRow.GONE);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
