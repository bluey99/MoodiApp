package com.example.asdproject.view.activities;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Space;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.asdproject.R;
import com.example.asdproject.util.LocaleManager;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReportsHistoryActivity extends BaseActivity {

    private RecyclerView recycler;
    private ReportsAdapter adapter;

    private FirebaseFirestore db;
    private ListenerRegistration reg;

    private String childIdField;
    private String childName;

    private final List<ReportItem> reports = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LocaleManager.setLocale(this);
        setContentView(R.layout.activity_reports_history);

        db = FirebaseFirestore.getInstance();

        recycler = findViewById(R.id.recyclerReportsHistory);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ReportsAdapter(reports);
        recycler.setAdapter(adapter);

        childIdField = firstNonEmpty(
                getIntent().getStringExtra("CHILD_ID"),
                getIntent().getStringExtra("childID"),
                getIntent().getStringExtra("childId")
        );

        childName = firstNonEmpty(
                getIntent().getStringExtra("CHILD_NAME"),
                getIntent().getStringExtra("childName")
        );

        TextView btnLanguage = findViewById(R.id.btnLanguage);
        Button back = findViewById(R.id.btnGoBackReports);

        back.setOnClickListener(v -> finish());

        btnLanguage.setOnClickListener(v -> {
            LocaleManager.toggleLanguage(this);
            recreate();
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        startListening();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (reg != null) reg.remove();
    }

    private void startListening() {

        if (isEmpty(childIdField)) {
            Toast.makeText(this, getString(R.string.missing_child_id), Toast.LENGTH_SHORT).show();
            return;
        }

        reg = db.collection("reports")
                .whereEqualTo("childID", childIdField)
                .addSnapshotListener((snap, e) -> {

                    if (e != null) {
                        Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    reports.clear();

                    if (snap != null) {
                        for (DocumentSnapshot d : snap.getDocuments()) {
                            reports.add(ReportItem.fromDoc(d));
                        }
                    }

                    Collections.sort(reports,
                            (a, b) -> safeStr(b.timestamp).compareTo(safeStr(a.timestamp)));

                    adapter.notifyDataSetChanged();
                });
    }

    private void confirmDeleteReport(ReportItem item) {

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.report_delete_title))
                .setMessage(getString(R.string.report_delete_message))
                .setNegativeButton(getString(R.string.cancel), null)
                .setPositiveButton(getString(R.string.delete),
                        (d, w) -> deleteReport(item))
                .show();
    }

    private void deleteReport(ReportItem item) {

        db.collection("reports")
                .document(item.docId)
                .delete()
                .addOnSuccessListener(v ->
                        Toast.makeText(this,
                                getString(R.string.report_deleted),
                                Toast.LENGTH_SHORT).show()
                )
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                getString(R.string.report_delete_failed),
                                Toast.LENGTH_SHORT).show()
                );
    }

    private void openEditDialog(ReportItem item) {

        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(14), dp(18), dp(6));
        scroll.addView(root);

        EditText s = niceInput(getString(R.string.report_label_what_happened), item.situation, true);
        EditText t = niceInput(getString(R.string.report_label_when), item.timestamp, false);
        EditText l = niceInput(getString(R.string.report_label_where), item.location, false);
        EditText r = niceInput(getString(R.string.report_label_child_reaction), item.childReaction, true);
        EditText h = niceInput(getString(R.string.report_label_how_handled), item.howHandled, true);
        EditText q = niceInput(getString(R.string.report_label_questions), item.questions, true);

        root.addView(label(getString(R.string.report_label_what_happened)));
        root.addView(s);
        root.addView(space(10));

        root.addView(label(getString(R.string.report_label_when)));
        root.addView(t);
        root.addView(space(10));

        root.addView(label(getString(R.string.report_label_where)));
        root.addView(l);
        root.addView(space(10));

        root.addView(label(getString(R.string.report_label_child_reaction)));
        root.addView(r);
        root.addView(space(10));

        root.addView(label(getString(R.string.report_label_how_handled)));
        root.addView(h);
        root.addView(space(10));

        root.addView(label(getString(R.string.report_label_questions)));
        root.addView(q);

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.report_edit_title))
                .setView(scroll)
                .setNegativeButton(getString(R.string.cancel), null)
                .setPositiveButton(getString(R.string.save), (d, w) -> {

                    Map<String, Object> updates = new HashMap<>();

                    updates.put("situation", safeStr(s.getText().toString()));
                    updates.put("timestamp", safeStr(t.getText().toString()));
                    updates.put("location", safeStr(l.getText().toString()));
                    updates.put("childReaction", safeStr(r.getText().toString()));
                    updates.put("howHandled", safeStr(h.getText().toString()));
                    updates.put("questions", safeStr(q.getText().toString()));

                    db.collection("reports")
                            .document(item.docId)
                            .update(updates)
                            .addOnSuccessListener(v ->
                                    Toast.makeText(this,
                                            getString(R.string.report_saved),
                                            Toast.LENGTH_SHORT).show());
                })
                .show();
    }

    private static class ReportItem {

        String docId;
        String situation;
        String timestamp;
        String location;
        String childReaction;
        String howHandled;
        String questions;
        String childID;

        static ReportItem fromDoc(DocumentSnapshot d) {

            ReportItem r = new ReportItem();

            r.docId = d.getId();
            r.situation = safeStr(d.getString("situation"));
            r.timestamp = safeStr(d.getString("timestamp"));
            r.location = safeStr(d.getString("location"));
            r.childReaction = safeStr(d.getString("childReaction"));
            r.howHandled = safeStr(d.getString("howHandled"));
            r.questions = safeStr(d.getString("questions"));
            r.childID = safeStr(d.getString("childID"));

            return r;
        }
    }

    private class ReportsAdapter extends RecyclerView.Adapter<ReportsAdapter.VH> {

        List<ReportItem> list;

        ReportsAdapter(List<ReportItem> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

            CardView card = new CardView(parent.getContext());

            RecyclerView.LayoutParams lp =
                    new RecyclerView.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT);

            lp.setMargins(dp(16), dp(8), dp(16), dp(8));
            card.setLayoutParams(lp);

            card.setRadius(dp(24));
            card.setCardBackgroundColor(0xFFFBF6E9);

            LinearLayout root = new LinearLayout(parent.getContext());
            root.setOrientation(LinearLayout.VERTICAL);
            root.setPadding(dp(18), dp(18), dp(18), dp(18));
            root.setLayoutParams(new CardView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));

            TextView title = new TextView(parent.getContext());
            title.setTextSize(20);
            title.setTypeface(null, android.graphics.Typeface.BOLD);

            TextView meta = new TextView(parent.getContext());
            meta.setTextSize(14);

            TextView body = new TextView(parent.getContext());
            body.setTextSize(16);

            LinearLayout actions = new LinearLayout(parent.getContext());
            actions.setGravity(Gravity.END);

            TextView edit = actionChip(getString(R.string.edit));
            TextView delete = actionChip(getString(R.string.delete));

            actions.addView(edit);
            actions.addView(space(10));
            actions.addView(delete);

            root.addView(title);
            root.addView(meta);
            root.addView(body);
            root.addView(actions);

            card.addView(root);

            return new VH(card, title, meta, body, edit, delete);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {

            ReportItem it = list.get(position);

            h.title.setText(it.situation);

            h.meta.setText(
                    getString(R.string.label_date) + " " + it.timestamp +
                            "   •   " +
                            getString(R.string.label_location) + " " + it.location
            );

            String body =
                    getString(R.string.report_label_child_reaction) +
                            "\n" + it.childReaction +
                            "\n\n" +
                            getString(R.string.report_label_how_handled) +
                            "\n" + it.howHandled;

            if (!isEmpty(it.questions)) {
                body += "\n\n" +
                        getString(R.string.report_label_questions) +
                        "\n" + it.questions;
            }

            h.body.setText(body);

            h.edit.setOnClickListener(v -> openEditDialog(it));
            h.delete.setOnClickListener(v -> confirmDeleteReport(it));
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class VH extends RecyclerView.ViewHolder {

            TextView title, meta, body, edit, delete;

            VH(@NonNull android.view.View itemView,
               TextView title,
               TextView meta,
               TextView body,
               TextView edit,
               TextView delete) {

                super(itemView);

                this.title = title;
                this.meta = meta;
                this.body = body;
                this.edit = edit;
                this.delete = delete;
            }
        }
    }

    private TextView actionChip(String txt) {

        TextView tv = new TextView(this);
        tv.setText(txt);
        tv.setPadding(dp(16), dp(10), dp(16), dp(10));
        tv.setBackgroundResource(R.drawable.report_action_chip_bg);
        tv.setClickable(true);

        return tv;
    }

    private TextView label(String txt) {

        TextView tv = new TextView(this);
        tv.setText(txt);
        tv.setTypeface(null, android.graphics.Typeface.BOLD);

        return tv;
    }

    private EditText niceInput(String hint, String val, boolean multiline) {

        EditText e = new EditText(this);

        e.setHint(hint);
        e.setText(val);

        if (multiline) {
            e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
            e.setMinLines(3);
            e.setGravity(Gravity.TOP | Gravity.START);
        }

        return e;
    }

    private Space space(int dp) {

        Space s = new Space(this);
        s.setLayoutParams(new LinearLayout.LayoutParams(dp(dp), 1));

        return s;
    }

    private int dp(int d) {
        return (int) (d * getResources().getDisplayMetrics().density);
    }

    private static String safeStr(String s) {
        return s == null ? "" : s.trim();
    }

    private static boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }

    private String firstNonEmpty(String... vals) {
        for (String v : vals) {
            if (v != null && !v.trim().isEmpty()) {
                return v.trim();
            }
        }
        return "";
    }
}