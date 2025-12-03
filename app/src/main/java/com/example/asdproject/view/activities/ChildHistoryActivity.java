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

/**
 * Displays the history of emotion entries recorded for a specific child.
 * Loads the logs from Firestore and presents them in a RecyclerView.
 */
public class ChildHistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerHistory;
    private TextView txtEmpty;
    private HistoryAdapter adapter;

    // In-memory list used to populate the RecyclerView
    private final List<EmotionLog> historyList = new ArrayList<>();

    // Firestore document ID for the current child
    private String childId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_child_history);

        // Connect UI components
        recyclerHistory = findViewById(R.id.recyclerHistory);
        txtEmpty = findViewById(R.id.txtEmpty);

        // Configure RecyclerView for vertical scrolling
        recyclerHistory.setLayoutManager(new LinearLayoutManager(this));
        adapter = new HistoryAdapter(historyList);
        recyclerHistory.setAdapter(adapter);

        // Retrieve the child ID passed from ChildHomeActivity
        childId = getIntent().getStringExtra("childId");

        // Load emotion history from Firestore
        loadHistory();
    }

    /**
     * Retrieves emotion logs from Firestore for the specified child.
     * Uses descending timestamp order so the newest entries appear first.
     */
    private void loadHistory() {
        FirebaseFirestore db = FirebaseManager.getDb();

        // Validate that a child ID was received
        if (childId == null) {
            txtEmpty.setText("No child selected");
            txtEmpty.setVisibility(View.VISIBLE);
            return;
        }

        // Query Firestore: children/{childId}/history ordered by timestamp (newest first)
        db.collection("children")
                .document(childId)
                .collection("history")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snap -> {
                    historyList.clear();

                    // Show empty message if there are no logs
                    if (snap.isEmpty()) {
                        txtEmpty.setVisibility(View.VISIBLE);
                        return;
                    }

                    txtEmpty.setVisibility(View.GONE);

                    // Convert each Firestore document into an EmotionLog object
                    snap.forEach(doc -> {
                        EmotionLog log = doc.toObject(EmotionLog.class);
                        log.setId(doc.getId()); // Assign Firestore document ID
                        historyList.add(log);
                    });

                    // Refresh RecyclerView with the updated data
                    adapter.notifyDataSetChanged();
                });
    }
}
