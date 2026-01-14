package com.example.asdproject.view.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.*;

import com.example.asdproject.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class ReportsHistoryActivity extends AppCompatActivity {

    private static final String TAG = "ReportsHistoryActivity";

    private TableLayout table;

    private static final int COL_SITUATION = 0;
    private static final int COL_TIMESTAMP = 1;
    private static final int COL_LOCATION  = 2;

    //  FIELD childID (not doc id)
    private String childIdField;
    private String childName;

    private FirebaseFirestore db;
    private ListenerRegistration reg;

    // Keep loaded reports in memory so we can sort/filter easily
    private final List<ReportItem> reports = new ArrayList<>();

    // Parse the timestamp you use in NewReportActivity: "dd/MM/yyyy HH:mm"
    private final SimpleDateFormat tsFormat =
            new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reports_history);

        db = FirebaseFirestore.getInstance();
        table = findViewById(R.id.tableReportsHistory);

        childIdField = getIntent().getStringExtra("CHILD_ID");
        if (childIdField == null) childIdField = getIntent().getStringExtra("childID");
        if (childIdField == null) childIdField = getIntent().getStringExtra("childId");

        childName = getIntent().getStringExtra("CHILD_NAME");
        if (childName == null) childName = getIntent().getStringExtra("childName");

        findViewById(R.id.btnGoBackReports).setOnClickListener(v -> finish());
        findViewById(R.id.btnFilterReports).setOnClickListener(v -> showFilterDialog());
    }

    @Override
    protected void onStart() {
        super.onStart();
        startListening();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (reg != null) {
            reg.remove();
            reg = null;
        }
    }

    // ---------------- FIRESTORE LISTENER ----------------

    private void startListening() {
        if (isEmpty(childIdField)) {
            Toast.makeText(this, "Missing childID", Toast.LENGTH_SHORT).show();
            clearTableRows();
            return;
        }

        // Optional parent filter:
        // If you want each parent to only see their own reports, keep this.
        // If you want all reports for the child regardless of parent account, remove parent filter part.
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String uid = (user != null) ? user.getUid() : null;

        Query q = db.collection("reports")
                .whereEqualTo("childID", childIdField);

        // If your reports have "parentID" field that equals child doc parentID (not auth uid),
        // then filtering by auth uid won't work.
        // So we only filter by parent if YOU stored auth uid in reports (you currently do NOT).
        // If you want strict parent filtering, store auth uid in report (e.g., report.put("parentAuthUid", uid)).
        // Example:
        // if (uid != null) q = q.whereEqualTo("parentAuthUid", uid);

        // Live updates (both you + partner will see same)
        reg = q.addSnapshotListener((snap, e) -> {
            if (e != null) {
                Log.e(TAG, "listen failed", e);
                Toast.makeText(this, "Failed to load reports: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                return;
            }

            reports.clear();

            if (snap != null) {
                for (DocumentSnapshot d : snap.getDocuments()) {
                    ReportItem item = ReportItem.fromDoc(d);
                    // defensive: enforce childID match
                    if (childIdField.equals(item.childID)) {
                        reports.add(item);
                    }
                }
            }

            // default view: just render in current order (or sort newest first)
            sortReportsByTimestamp(true);
            renderTable(reports);
        });
    }

    // ---------------- TABLE RENDER ----------------

    private void clearTableRows() {
        if (table.getChildCount() > 1) {
            table.removeViews(1, table.getChildCount() - 1);
        }
    }

    private void renderTable(List<ReportItem> list) {
        clearTableRows();

        for (ReportItem item : list) {
            addRow(item);
        }
    }

    private void addRow(ReportItem item) {
        TableRow row = new TableRow(this);

        row.addView(cell(item.situation, 160));
        row.addView(cell(item.timestamp, 150));
        row.addView(cell(item.location, 120));

        TextView edit = cell("✏️", 80);
        edit.setOnClickListener(v -> openEditDialog(item));

        TextView del = cell("🗑️", 90);
        del.setOnClickListener(v -> deleteReport(item));

        row.addView(edit);
        row.addView(del);

        // store doc id so filters/sorts can still work without losing association
        row.setTag(item.docId);

        table.addView(row);
    }

    private TextView cell(String text, int widthDp) {
        TextView tv = new TextView(this);
        tv.setText(text == null ? "" : text);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(10, 10, 10, 10);
        tv.setBackgroundResource(R.drawable.table_cell_bg);
        tv.setLayoutParams(new TableRow.LayoutParams(dp(widthDp), dp(44)));
        return tv;
    }

    private int dp(int d) {
        return (int) (d * getResources().getDisplayMetrics().density);
    }

    // ---------------- DELETE / EDIT (FIRESTORE) ----------------

    private void deleteReport(ReportItem item) {
        if (item == null || isEmpty(item.docId)) return;

        db.collection("reports")
                .document(item.docId)
                .delete()
                .addOnSuccessListener(v ->
                        Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show()
                )
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Delete failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void openEditDialog(ReportItem item) {
        if (item == null || isEmpty(item.docId)) return;

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(20, 10, 20, 10);

        EditText s = input("Situation", item.situation);
        EditText t = input("Timestamp", item.timestamp);
        EditText l = input("Location", item.location);
        EditText r = input("Child’s reaction", item.childReaction);
        EditText h = input("How handled", item.howHandled);
        EditText q = input("Questions for therapist (optional)", item.questions);

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
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("situation", s.getText().toString().trim());
                    updates.put("timestamp", t.getText().toString().trim());
                    updates.put("location", l.getText().toString().trim());
                    updates.put("childReaction", r.getText().toString().trim());
                    updates.put("howHandled", h.getText().toString().trim());
                    updates.put("questions", q.getText().toString().trim());

                    // keep these consistent
                    if (!isEmpty(childIdField)) updates.put("childID", childIdField);
                    if (!isEmpty(childName)) updates.put("childName", childName);

                    db.collection("reports")
                            .document(item.docId)
                            .update(updates)
                            .addOnSuccessListener(v ->
                                    Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show()
                            )
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Save failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                            );
                })
                .show();
    }

    private EditText input(String hint, String val) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setText(val == null ? "" : val);
        e.setInputType(InputType.TYPE_CLASS_TEXT);
        return e;
    }

    // ---------------- FILTERS / SORT (TABLE-BASED) ----------------

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
                    } else {
                        clearFilters();
                    }
                })
                .show();
    }

    private void clearFilters() {
        // show everything again
        for (int i = 1; i < table.getChildCount(); i++) {
            View v = table.getChildAt(i);
            if (v instanceof TableRow) v.setVisibility(TableRow.VISIBLE);
        }
    }

    private void showTimestampFilterDialog() {
        String[] options = {"Newest -> Oldest", "Oldest -> Newest"};

        new AlertDialog.Builder(this)
                .setTitle("Sort by time")
                .setItems(options, (dialog, which) -> {
                    boolean newestFirst = (which == 0);
                    sortReportsByTimestamp(newestFirst);
                    renderTable(reports);
                })
                .show();
    }

    private void sortReportsByTimestamp(boolean newestFirst) {
        reports.sort((a, b) -> {
            long ta = parseTimestampMillis(a.timestamp);
            long tb = parseTimestampMillis(b.timestamp);

            // fallback if parsing fails
            if (ta == Long.MIN_VALUE || tb == Long.MIN_VALUE) {
                int cmp = safeStr(a.timestamp).compareTo(safeStr(b.timestamp));
                return newestFirst ? -cmp : cmp;
            }

            int cmp = Long.compare(ta, tb);
            return newestFirst ? -cmp : cmp;
        });
    }

    private long parseTimestampMillis(String ts) {
        if (isEmpty(ts)) return Long.MIN_VALUE;
        try {
            Date d = tsFormat.parse(ts.trim());
            return (d == null) ? Long.MIN_VALUE : d.getTime();
        } catch (ParseException e) {
            return Long.MIN_VALUE;
        }
    }

    private void textFilter(int col, String title) {
        EditText input = new EditText(this);

        new AlertDialog.Builder(this)
                .setTitle("Search by " + title)
                .setView(input)
                .setPositiveButton("Search", (d, w) -> {
                    String q = input.getText().toString().toLowerCase(Locale.getDefault()).trim();

                    for (int i = 1; i < table.getChildCount(); i++) {
                        View v = table.getChildAt(i);
                        if (!(v instanceof TableRow)) continue;

                        TableRow r = (TableRow) v;
                        View cellView = r.getChildAt(col);
                        if (!(cellView instanceof TextView)) continue;

                        String txt = ((TextView) cellView).getText().toString()
                                .toLowerCase(Locale.getDefault());

                        r.setVisibility(txt.contains(q) ? TableRow.VISIBLE : TableRow.GONE);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ---------------- UTILS ----------------

    private boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }

    private String safeStr(String s) {
        return s == null ? "" : s;
    }

    // ---------------- MODEL ----------------

    private static class ReportItem {
        String docId;

        String childID;
        String childName;

        String situation;
        String timestamp;
        String location;

        String childReaction;
        String howHandled;
        String questions;

        static ReportItem fromDoc(DocumentSnapshot d) {
            ReportItem r = new ReportItem();
            r.docId = d.getId();

            r.childID = d.getString("childID");
            r.childName = d.getString("childName");

            r.situation = d.getString("situation");
            r.timestamp = d.getString("timestamp");
            r.location  = d.getString("location");

            r.childReaction = d.getString("childReaction");
            r.howHandled    = d.getString("howHandled");
            r.questions     = d.getString("questions");

            // Avoid nulls in table
            if (r.situation == null) r.situation = "";
            if (r.timestamp == null) r.timestamp = "";
            if (r.location  == null) r.location  = "";
            if (r.childReaction == null) r.childReaction = "";
            if (r.howHandled == null) r.howHandled = "";
            if (r.questions == null) r.questions = "";

            if (r.childID == null) r.childID = "";
            if (r.childName == null) r.childName = "";

            return r;
        }
    }
}
