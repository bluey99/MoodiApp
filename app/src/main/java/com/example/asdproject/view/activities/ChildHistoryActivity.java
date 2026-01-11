package com.example.asdproject.view.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.asdproject.R;
import com.example.asdproject.controller.FirebaseManager;
import com.example.asdproject.model.EmotionLog;
import com.example.asdproject.view.adapters.HistoryAdapter;
import com.example.asdproject.view.fragments.HistoryFilterBottomSheetFragment;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

/**
 * ChildHistoryActivity
 *
 * Displays the child's emotion history and supports filtering by:
 * - Emotion
 * - Intensity range
 * - Time range
 *
 * Data is fetched once from Firestore and cached locally.
 * All filters are applied locally to ensure fast UI updates.
 *
 * UX note:
 * Filter feedback is shown ONLY in the log count pill
 * (e.g. "Filtered feelings (Surprised): 1")
 */
public class ChildHistoryActivity extends AppCompatActivity {

    /* ===================== UI ===================== */

    private RecyclerView recyclerHistory;
    private HistoryAdapter historyAdapter;

    private TextView txtLogCount;
    private TextView txtLoading;

    private View emptyContainer;
    private View loadingContainer;

    private ImageView btnFilter;

    /* ===================== DATA ===================== */

    private String childId;
    private final List<EmotionLog> cachedLogs = new ArrayList<>();

    /* ===================== FILTER STATE ===================== */

    private String selectedEmotion = null;
    private int selectedIntensity = -1;
    private TimeFilter selectedTime = TimeFilter.ALL;

    private enum TimeFilter {
        ALL,
        LAST_7_DAYS,
        LAST_30_DAYS
    }

    /* ===================== LIFECYCLE ===================== */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_child_history);

        setupHeader();
        setupViews();
        setupRecycler();

        childId = getIntent().getStringExtra("childId");

        btnFilter.setOnClickListener(v -> openFilterBottomSheet());

        loadHistoryWithFilters();
    }

    /* ===================== SETUP ===================== */

    private void setupHeader() {
        View header = findViewById(R.id.header);

        TextView headerTitle = header.findViewById(R.id.txtHeaderTitle);
        headerTitle.setText("My History");

        header.findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        btnFilter = header.findViewById(R.id.btnFilter);
    }

    private void setupViews() {
        recyclerHistory = findViewById(R.id.recyclerHistory);
        txtLogCount = findViewById(R.id.txtLogCount);
        emptyContainer = findViewById(R.id.emptyContainer);
        loadingContainer = findViewById(R.id.loadingContainer);
        txtLoading = findViewById(R.id.txtLoading);
    }

    private void setupRecycler() {
        recyclerHistory.setLayoutManager(new LinearLayoutManager(this));
        historyAdapter = new HistoryAdapter(new ArrayList<>());
        recyclerHistory.setAdapter(historyAdapter);
    }

    /* ===================== FILTER UI ===================== */

    private void openFilterBottomSheet() {
        HistoryFilterBottomSheetFragment sheet =
                new HistoryFilterBottomSheetFragment();

        sheet.setListener((emotion, intensity, time) -> {

            selectedEmotion = emotion;
            selectedIntensity = intensity;

            if ("LAST_7_DAYS".equals(time)) {
                selectedTime = TimeFilter.LAST_7_DAYS;
            } else if ("LAST_30_DAYS".equals(time)) {
                selectedTime = TimeFilter.LAST_30_DAYS;
            } else {
                selectedTime = TimeFilter.ALL;
            }

            loadHistoryWithFilters();
        });


        sheet.show(getSupportFragmentManager(), "HistoryFilter");
    }

    /* ===================== DATA LOADING ===================== */

    private void loadHistoryWithFilters() {

        if (childId == null) {
            showEmptyState();
            return;
        }

        // Already fetched → filter locally
        if (!cachedLogs.isEmpty()) {
            applyFiltersAndUpdateUI();
            return;
        }

        showLoadingState();

        FirebaseFirestore db = FirebaseManager.getDb();

        db.collection("children")
                .document(childId)
                .collection("history")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snapshot -> {

                    cachedLogs.clear();

                    snapshot.forEach(doc -> {
                        EmotionLog log = doc.toObject(EmotionLog.class);
                        log.setId(doc.getId());
                        cachedLogs.add(log);
                    });

                    applyFiltersAndUpdateUI();
                })
                .addOnFailureListener(e -> showEmptyState());
    }

    /* ===================== FILTERING ===================== */

    private List<EmotionLog> applyFilters(List<EmotionLog> logs) {

        List<EmotionLog> result = new ArrayList<>();
        long now = System.currentTimeMillis();

        for (EmotionLog log : logs) {

            if (selectedEmotion != null) {
                String feeling = log.getFeeling() == null
                        ? ""
                        : log.getFeeling().trim().toUpperCase();
                if (!selectedEmotion.equals(feeling)) continue;
            }

            if (selectedIntensity != -1 && log.getIntensity() != selectedIntensity) {
                continue;
            }


            if (selectedTime != TimeFilter.ALL && log.getTimestamp() != null) {
                long days =
                        (now - log.getTimestamp().toDate().getTime())
                                / (1000 * 60 * 60 * 24);

                if (selectedTime == TimeFilter.LAST_7_DAYS && days > 7) continue;
                if (selectedTime == TimeFilter.LAST_30_DAYS && days > 30) continue;
            }

            result.add(log);
        }

        return result;
    }

    private void applyFiltersAndUpdateUI() {
        updateHistoryUI(applyFilters(cachedLogs));
    }

    /* ===================== UI UPDATE ===================== */

    private void updateHistoryUI(List<EmotionLog> logs) {

        hideLoadingState();

        boolean filtersActive =
                selectedEmotion != null ||
                        selectedIntensity != -1 ||
                        selectedTime != TimeFilter.ALL;

        // --- EMPTY RESULT CASE ---
        if (logs.isEmpty()) {

            txtLogCount.setVisibility(View.VISIBLE);

            if (filtersActive) {
                txtLogCount.setText(
                        "Filtered feelings" + buildFilterSuffix() + ": 0"
                );
            } else {
                txtLogCount.setText("Saved feelings: 0");
            }

            recyclerHistory.setVisibility(View.GONE);
            emptyContainer.setVisibility(View.VISIBLE);

            historyAdapter.updateData(new ArrayList<>());
            return;
        }

        // --- NON-EMPTY CASE ---
        emptyContainer.setVisibility(View.GONE);
        recyclerHistory.setVisibility(View.VISIBLE);
        txtLogCount.setVisibility(View.VISIBLE);

        if (filtersActive) {
            txtLogCount.setText(
                    "Filtered feelings" + buildFilterSuffix() + ": " + logs.size()
            );
        } else {
            txtLogCount.setText("Saved feelings: " + logs.size());
        }

        // --- GROUP LOGS ---
        List<Object> grouped = new ArrayList<>();
        List<EmotionLog> thisWeek = new ArrayList<>();
        List<EmotionLog> older = new ArrayList<>();

        long now = System.currentTimeMillis();

        for (EmotionLog log : logs) {
            if (log.getTimestamp() == null) continue;

            long days =
                    (now - log.getTimestamp().toDate().getTime())
                            / (1000 * 60 * 60 * 24);

            if (days <= 7) thisWeek.add(log);
            else older.add(log);
        }

        if (!thisWeek.isEmpty()) {
            grouped.add("This Week");
            grouped.addAll(thisWeek);
        }

        if (!older.isEmpty()) {
            grouped.add("Earlier");
            grouped.addAll(older);
        }

        historyAdapter.updateData(grouped);
    }

    /* ===================== FILTER SUMMARY ===================== */

    /**
     * Builds a short filter description for the log count pill.
     * Example: " (Surprised, Last 7 days)"
     */
    private String buildFilterSuffix() {

        List<String> parts = new ArrayList<>();

        if (selectedEmotion != null) {
            parts.add(capitalize(selectedEmotion.toLowerCase()));
        }

        if (selectedTime == TimeFilter.LAST_7_DAYS) {
            parts.add("Last 7 days");
        } else if (selectedTime == TimeFilter.LAST_30_DAYS) {
            parts.add("Last 30 days");
        }

        if (parts.isEmpty()) return "";

        return " (" + String.join(", ", parts) + ")";
    }

    /* ===================== UI STATES ===================== */

    private void showLoadingState() {
        loadingContainer.setVisibility(View.VISIBLE);
        recyclerHistory.setVisibility(View.INVISIBLE);
        emptyContainer.setVisibility(View.GONE);
        txtLogCount.setVisibility(View.GONE);
    }

    private void hideLoadingState() {
        loadingContainer.setVisibility(View.GONE);
        recyclerHistory.setVisibility(View.VISIBLE);
        txtLogCount.setVisibility(View.VISIBLE);
    }

    private void showEmptyState() {
        loadingContainer.setVisibility(View.GONE);
        recyclerHistory.setVisibility(View.GONE);
        emptyContainer.setVisibility(View.VISIBLE);

        // IMPORTANT:
        // txtLogCount visibility is controlled by updateHistoryUI()
    }


    /* ===================== UTIL ===================== */

    private String capitalize(String text) {
        if (text == null || text.isEmpty()) return text;
        return text.substring(0, 1).toUpperCase() + text.substring(1);
    }
}
