package com.example.asdproject.view.activities;

import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.asdproject.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class TherapistNotesActivity extends AppCompatActivity {

    private String childId;
    private String childName;

    private RecyclerView recycler;
    private NotesAdapter adapter;
    private final List<NoteItem> notes = new ArrayList<>();

    private FirebaseFirestore db;
    private ListenerRegistration reg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_therapist_notes);

        childId = getIntent().getStringExtra("CHILD_ID");
        childName = getIntent().getStringExtra("CHILD_NAME");

        if (childName != null && !childName.isEmpty()) {
            setTitle("Therapist Notes – " + childName);
        }

        recycler = findViewById(R.id.recyclerTherapistNotes);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NotesAdapter(notes);
        recycler.setAdapter(adapter);

        Button btnBack = findViewById(R.id.btnBackFromNotes);
        Button btnFilter = findViewById(R.id.btnFilterNotes);

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btnFilter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg;
                if (childName != null && !childName.isEmpty()) {
                    msg = "Filter options for " + childName + " will be added later.";
                } else {
                    msg = "Filter options will be added later.";
                }
                Toast.makeText(TherapistNotesActivity.this,
                        msg,
                        Toast.LENGTH_SHORT).show();
            }
        });

        db = FirebaseFirestore.getInstance();

        if (childId == null || childId.trim().isEmpty()) {
            Toast.makeText(this, "Missing childId!", Toast.LENGTH_LONG).show();
            return;
        }

        listenForNotes();
    }

    private void listenForNotes() {
        Query q = db.collection("feedbacks")
                .whereEqualTo("childID", childId);

        reg = q.addSnapshotListener((snap, e) -> {
            if (e != null) {
                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                return;
            }
            if (snap == null) return;

            notes.clear();

            for (DocumentSnapshot d : snap.getDocuments()) {
                String title = d.getString("title");
                String desc = d.getString("description");
                String date = d.getString("date");
                String time = d.getString("time");

                if (title == null) title = "";
                if (desc == null) desc = "";
                if (date == null) date = "";
                if (time == null) time = "";

                notes.add(new NoteItem(title, desc, date, time));
            }

            adapter.notifyDataSetChanged();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (reg != null) reg.remove();
    }

    private static class NoteItem {
        final String title;
        final String description;
        final String date;
        final String time;

        NoteItem(String title, String description, String date, String time) {
            this.title = title;
            this.description = description;
            this.date = date;
            this.time = time;
        }
    }

    private class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.VH> {

        private final List<NoteItem> list;

        NotesAdapter(List<NoteItem> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            int pad18 = dp(18);
            int rad24 = dp(24);
            int mb14 = dp(14);

            CardView card = new CardView(parent.getContext());
            RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            lp.bottomMargin = mb14;
            card.setLayoutParams(lp);
            card.setRadius(rad24);
            card.setCardElevation(0f);
            card.setCardBackgroundColor(0xFFFBF6E9);

            LinearLayout root = new LinearLayout(parent.getContext());
            root.setLayoutParams(new CardView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));
            root.setOrientation(LinearLayout.VERTICAL);
            root.setPadding(pad18, pad18, pad18, pad18);

            LinearLayout titleRow = new LinearLayout(parent.getContext());
            titleRow.setOrientation(LinearLayout.HORIZONTAL);
            titleRow.setGravity(Gravity.CENTER_VERTICAL);
            titleRow.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));

            TextView bullet = new TextView(parent.getContext());
            bullet.setText("• ");
            bullet.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
            bullet.setTextColor(0xFF000000);
            bullet.setPadding(0, 0, dp(6), 0);

            TextView tvTitle = new TextView(parent.getContext());
            tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
            tvTitle.setTextColor(0xFF000000);
            tvTitle.setTypeface(tvTitle.getTypeface(), android.graphics.Typeface.BOLD);
            LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    1f
            );
            tvTitle.setLayoutParams(titleLp);

            titleRow.addView(bullet);
            titleRow.addView(tvTitle);

            LinearLayout dtRow = new LinearLayout(parent.getContext());
            dtRow.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams dtLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            dtLp.topMargin = dp(6);
            dtRow.setLayoutParams(dtLp);

            TextView tvDate = new TextView(parent.getContext());
            tvDate.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            tvDate.setTextColor(0xFF000000);

            Space sp = new Space(parent.getContext());
            sp.setLayoutParams(new LinearLayout.LayoutParams(dp(16), 1));

            TextView tvTime = new TextView(parent.getContext());
            tvTime.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            tvTime.setTextColor(0xFF000000);

            dtRow.addView(tvDate);
            dtRow.addView(sp);
            dtRow.addView(tvTime);

            TextView tvDesc = new TextView(parent.getContext());
            LinearLayout.LayoutParams descLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            descLp.topMargin = dp(10);
            tvDesc.setLayoutParams(descLp);
            tvDesc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
            tvDesc.setGravity(Gravity.CENTER);
            tvDesc.setTextColor(0xFF000000);

            root.addView(titleRow);
            root.addView(dtRow);
            root.addView(tvDesc);

            card.addView(root);

            return new VH(card, tvTitle, tvDate, tvTime, tvDesc);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            NoteItem n = list.get(position);
            h.tvTitle.setText(n.title == null ? "" : n.title);
            h.tvDate.setText("Date: " + (n.date == null ? "" : n.date));
            h.tvTime.setText("Time: " + (n.time == null ? "" : n.time));
            String desc = (n.description == null ? "" : n.description);
            h.tvDesc.setText("“" + desc + "”");
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class VH extends RecyclerView.ViewHolder {
            final TextView tvTitle;
            final TextView tvDate;
            final TextView tvTime;
            final TextView tvDesc;

            VH(@NonNull View itemView, TextView tvTitle, TextView tvDate, TextView tvTime, TextView tvDesc) {
                super(itemView);
                this.tvTitle = tvTitle;
                this.tvDate = tvDate;
                this.tvTime = tvTime;
                this.tvDesc = tvDesc;
            }
        }
    }

    private int dp(int v) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                v,
                getResources().getDisplayMetrics()
        );
    }
}
