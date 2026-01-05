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
 * Displays the child's emotion history.
 * Supports filtering by emotion, intensity, and time range.
 *
 * Responsibilities:
 * - Fetch emotion logs from Firestore
 * - Apply selected filters
 * - Group results by time sections
 * - Update UI and empty state accordingly
 */
public class ChildHistoryActivity extends AppCompatActivity {

    /* ===================== UI ===================== */

    private RecyclerView recyclerHistory;
    private HistoryAdapter historyAdapter;
    private TextView txtLogCount;
    private View emptyContainer;
    private ImageView btnFilter;

    // Cached full history from Firestore
    private List<EmotionLog> cachedLogs = new ArrayList<>();
    private TextView txtLoading;
    private View loadingContainer;




    /* ===================== DATA ===================== */

    private String childId;

    /* ===================== FILTER STATE ===================== */

    private String selectedEmotion = null;   // null = no emotion filter
    private int minIntensity = -1;            // -1 = no minimum
    private int maxIntensity = -1;            // -1 = no maximum
    private TimeFilter selectedTime = TimeFilter.ALL;

    // Time filter options for history filtering.
    private enum TimeFilter {
        ALL,
        LAST_7_DAYS,
        LAST_30_DAYS
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_child_history);


        /* ---------- Header ---------- */

        TextView headerTitle = findViewById(R.id.txtHeaderTitle);
        headerTitle.setText("My History");

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        btnFilter = findViewById(R.id.btnFilter);

        /* ---------- Main UI ---------- */

        recyclerHistory = findViewById(R.id.recyclerHistory);
        txtLogCount = findViewById(R.id.txtLogCount);
        emptyContainer = findViewById(R.id.emptyContainer);
        txtLoading = findViewById(R.id.txtLoading);
        loadingContainer = findViewById(R.id.loadingContainer);



        recyclerHistory.setLayoutManager(new LinearLayoutManager(this));

        /*
         * Adapter is created once and reused.
         * This prevents UI lag and RecyclerView warnings.
         */
        historyAdapter = new HistoryAdapter(new ArrayList<>());
        recyclerHistory.setAdapter(historyAdapter);

        childId = getIntent().getStringExtra("childId");

        /* ---------- Filter Button ---------- */

        btnFilter.setOnClickListener(v -> openFilterBottomSheet());

        loadHistoryWithFilters();
    }

    /**
     * Opens the bottom sheet used to select history filters.
     */
    private void openFilterBottomSheet() {
        HistoryFilterBottomSheetFragment sheet =
                new HistoryFilterBottomSheetFragment();

        sheet.setListener((emotion, minI, maxI, time) -> {

            selectedEmotion = emotion;
            minIntensity = minI;
            maxIntensity = maxI;

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

    /**
     * Fetches history logs from Firestore and applies active filters.
     */
    private void loadHistoryWithFilters() {

        if (childId == null) {
            showEmptyState();
            return;
        }

        // If we already have data → filter locally
        if (!cachedLogs.isEmpty()) {
            hideLoadingState();
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
                .addOnFailureListener(e -> {
                    showEmptyState();
                });
    }


    /**
     * Applies emotion, intensity, and time filters to the full log list.
     *
     * @param logs Full list of emotion logs
     * @return Filtered list
     */
    private List<EmotionLog> applyFilters(List<EmotionLog> logs) {

        List<EmotionLog> result = new ArrayList<>();
        long now = System.currentTimeMillis();

        for (EmotionLog log : logs) {

            // Emotion filter
            if (selectedEmotion != null) {
                String logFeeling = (log.getFeeling() == null) ? "" : log.getFeeling().trim().toUpperCase();
                if (!selectedEmotion.equals(logFeeling)) {
                    continue;
                }
            }


            // Intensity filters
            if (minIntensity != -1 && log.getIntensity() < minIntensity) continue;
            if (maxIntensity != -1 && log.getIntensity() > maxIntensity) continue;

            // Time filter
            if (selectedTime != TimeFilter.ALL && log.getTimestamp() != null) {

                long diffDays =
                        (now - log.getTimestamp().toDate().getTime())
                                / (1000 * 60 * 60 * 24);

                if (selectedTime == TimeFilter.LAST_7_DAYS && diffDays > 7) continue;
                if (selectedTime == TimeFilter.LAST_30_DAYS && diffDays > 30) continue;
            }

            result.add(log);
        }

        return result;
    }
    private void applyFiltersAndUpdateUI() {
        List<EmotionLog> filtered = applyFilters(cachedLogs);
        updateHistoryUI(filtered);
    }


    /**
     * Updates the RecyclerView and empty state based on filtered results.
     *
     * @param logs Filtered emotion logs
     */
    private void updateHistoryUI(List<EmotionLog> logs) {

        hideLoadingState();

        if (logs.isEmpty()) {
            showEmptyState();
            historyAdapter.updateData(new ArrayList<>());
            return;
        }

        hideEmptyState();
        txtLogCount.setText("Saved feelings: " + logs.size());

        boolean filtersActive =
                selectedEmotion != null ||
                        minIntensity != -1 ||
                        maxIntensity != -1 ||
                        selectedTime != TimeFilter.ALL;

        if (filtersActive) {
            txtLogCount.setText("Filtered feelings: " + logs.size());
        } else {
            txtLogCount.setText("Saved feelings: " + logs.size());
        }


        List<Object> groupedList = new ArrayList<>();
        List<EmotionLog> thisWeek = new ArrayList<>();
        List<EmotionLog> older = new ArrayList<>();

        long now = System.currentTimeMillis();

        for (EmotionLog log : logs) {
            if (log.getTimestamp() == null) continue;

            long days =
                    (now - log.getTimestamp().toDate().getTime())
                            / (1000 * 60 * 60 * 24);

            if (days <= 7) {
                thisWeek.add(log);
            } else {
                older.add(log);
            }
        }

        if (!thisWeek.isEmpty()) {
            groupedList.add("This Week");
            groupedList.addAll(thisWeek);
        }

        if (!older.isEmpty()) {
            groupedList.add("Earlier");
            groupedList.addAll(older);
        }

        historyAdapter.updateData(groupedList);
    }

    /**
     * Displays the empty state UI.
     */
    private void showEmptyState() {
        emptyContainer.setVisibility(View.VISIBLE);
        txtLogCount.setVisibility(View.GONE);
    }

    /**
     * Hides the empty state UI.
     */
    private void hideEmptyState() {
        emptyContainer.setVisibility(View.GONE);
        txtLogCount.setVisibility(View.VISIBLE);
    }
    private void showLoadingState() {
        loadingContainer.setVisibility(View.VISIBLE);
        recyclerHistory.setVisibility(View.INVISIBLE);
        emptyContainer.setVisibility(View.GONE);
        txtLogCount.setVisibility(View.GONE);
    }

    private void hideLoadingState() {
        loadingContainer.setVisibility(View.GONE);
        recyclerHistory.setVisibility(View.VISIBLE);
    }


}
