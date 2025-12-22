package com.example.asdproject.view.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.asdproject.R;
import com.example.asdproject.controller.FirebaseManager;
import com.example.asdproject.model.EmotionLog;
import com.example.asdproject.view.adapters.HistoryAdapter;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class ChildHistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerHistory;
    private TextView txtLogCount;
    private String childId;
    private View emptyContainer;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_child_history);
        TextView headerTitle = findViewById(R.id.txtHeaderTitle);
        headerTitle.setText("My History");

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());


        recyclerHistory = findViewById(R.id.recyclerHistory);
        txtLogCount = findViewById(R.id.txtLogCount);
        recyclerHistory.setLayoutManager(new LinearLayoutManager(this));
        childId = getIntent().getStringExtra("childId");
        emptyContainer = findViewById(R.id.emptyContainer);

        loadHistory();
    }

    private void loadHistory() {

        if (childId == null) {
            emptyContainer.setVisibility(View.VISIBLE);
            recyclerHistory.setAdapter(null);
            txtLogCount.setVisibility(View.GONE);
            return;
        }


        FirebaseFirestore db = FirebaseManager.getDb();

        db.collection("children")
                .document(childId)
                .collection("history")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snap -> {

                    List<EmotionLog> historyList = new ArrayList<>();
                    snap.forEach(doc -> {
                        EmotionLog log = doc.toObject(EmotionLog.class);
                        log.setId(doc.getId());
                        historyList.add(log);
                    });

                    // UPDATE "You've logged X feelings"
                    txtLogCount.setText("Saved feelings: " + historyList.size());


                    if (historyList.isEmpty()) {
                        emptyContainer.setVisibility(View.VISIBLE);
                        recyclerHistory.setAdapter(null);
                        txtLogCount.setVisibility(View.GONE);
                        return;
                    }

                    emptyContainer.setVisibility(View.GONE);
                    txtLogCount.setVisibility(View.VISIBLE);

                    // ---------- GROUPING ----------
                    List<Object> groupedList = new ArrayList<>();

                    List<EmotionLog> thisWeek = new ArrayList<>();
                    List<EmotionLog> older = new ArrayList<>();

                    for (EmotionLog log : historyList) {
                        if (log.getTimestamp() == null) continue;

                        long days =
                                (System.currentTimeMillis() - log.getTimestamp().toDate().getTime())
                                        / (1000 * 60 * 60 * 24);

                        if (days <= 7) {
                            thisWeek.add(log);
                        } else {
                            older.add(log);
                        }
                    }

                    // Add sections ONLY if they have items
                    if (!thisWeek.isEmpty()) {
                        groupedList.add("This Week");
                        groupedList.addAll(thisWeek);
                    }

                    if (!older.isEmpty()) {
                        groupedList.add("Earlier");
                        groupedList.addAll(older);
                    }





                    // Final adapter
                    HistoryAdapter adapter = new HistoryAdapter(groupedList);
                    recyclerHistory.setAdapter(adapter);
                });
    }
}
