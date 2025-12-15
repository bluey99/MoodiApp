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
    private TextView txtEmpty, txtLogCount;

    private String childId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_child_history);

        recyclerHistory = findViewById(R.id.recyclerHistory);
        txtEmpty = findViewById(R.id.txtEmpty);
        txtLogCount = findViewById(R.id.txtLogCount);

        recyclerHistory.setLayoutManager(new LinearLayoutManager(this));

        childId = getIntent().getStringExtra("childId");

        loadHistory();
    }

    private void loadHistory() {

        if (childId == null) {
            txtEmpty.setVisibility(View.VISIBLE);
            txtEmpty.setText("No child selected");
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
                    txtLogCount.setText("You've logged " + historyList.size() + " feelings");

                    if (historyList.isEmpty()) {
                        txtEmpty.setVisibility(View.VISIBLE);
                        recyclerHistory.setAdapter(null);
                        return;
                    }

                    txtEmpty.setVisibility(View.GONE);

                    // ---------- GROUPING ----------
                    List<Object> groupedList = new ArrayList<>();

                    groupedList.add("This Week");
                    for (EmotionLog log : historyList) {
                        if (log.getTimestamp() == null) continue;

                        long days = (System.currentTimeMillis() - log.getTimestamp().toDate().getTime()
                        )
                                / (1000 * 60 * 60 * 24);
                        if (days <= 7) groupedList.add(log);
                    }

                    groupedList.add("Older Entries");
                    for (EmotionLog log : historyList) {
                        if (log.getTimestamp() == null) continue;

                        long days = (System.currentTimeMillis() - log.getTimestamp().toDate().getTime()
                        )
                                / (1000 * 60 * 60 * 24);
                        if (days > 7) groupedList.add(log);
                    }


                    // Final adapter
                    HistoryAdapter adapter = new HistoryAdapter(groupedList);
                    recyclerHistory.setAdapter(adapter);
                });
    }
}
