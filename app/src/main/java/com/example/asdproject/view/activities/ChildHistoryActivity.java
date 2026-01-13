package com.example.asdproject.view.activities;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
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

    private View filterPill;
    private TextView txtPillLabel;
    private TextView txtPillCount;
    private TextView txtPillTime;
    private ImageView imgPillEmotion;
    private View miniGlass;
    private View miniGlassFill;

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
        filterPill = findViewById(R.id.filterPill);
        txtPillCount = findViewById(R.id.txtPillCount);
        txtPillTime = findViewById(R.id.txtPillTime);
        imgPillEmotion = findViewById(R.id.imgPillEmotion);
        miniGlass = findViewById(R.id.miniGlass);
        miniGlassFill = findViewById(R.id.miniGlassFill);

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

// 1) Resolve Firestore document ID using REAL childID
        db.collection("children")
                .whereEqualTo("childID", childId)
                .limit(1)
                .get()
                .addOnSuccessListener(query -> {

                    if (query.isEmpty()) {
                        showEmptyState();
                        return;
                    }

                    String resolvedDocId = query.getDocuments().get(0).getId();

                    // 2) Load history using resolved document ID
                    db.collection("children")
                            .document(resolvedDocId)
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

        updateFilterPill(logs.size());

        if (logs.isEmpty()) {
            recyclerHistory.setVisibility(View.GONE);
            emptyContainer.setVisibility(View.VISIBLE);
            historyAdapter.updateData(new ArrayList<>());
            return;
        }

        emptyContainer.setVisibility(View.GONE);
        recyclerHistory.setVisibility(View.VISIBLE);

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

    /* ===================== UI STATES ===================== */

    private void showLoadingState() {
        loadingContainer.setVisibility(View.VISIBLE);
        recyclerHistory.setVisibility(View.INVISIBLE);
        emptyContainer.setVisibility(View.GONE);
    }

    private void hideLoadingState() {
        loadingContainer.setVisibility(View.GONE);
        recyclerHistory.setVisibility(View.VISIBLE);
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
    private int emotionToDrawable(String emotion) {
        if (emotion == null) return 0;

        switch (emotion) {
            case "HAPPY":
                return R.drawable.emoji_happy;
            case "SAD":
                return R.drawable.emoji_sad;
            case "ANGRY":
                return R.drawable.emoji_angry;
            case "AFRAID":
                return R.drawable.emoji_afraid;
            case "SURPRISED":
                return R.drawable.emoji_surprised;
            default:
                return 0;
        }
    }
    private void updateFilterPill(int count) {

        boolean filtersActive =
                selectedEmotion != null ||
                        selectedIntensity != -1 ||
                        selectedTime != TimeFilter.ALL;

        if (!filtersActive) {
            filterPill.setVisibility(View.GONE);
            return;
        }

        filterPill.setVisibility(View.VISIBLE);

        txtPillCount.setText(String.valueOf(count));

        if (selectedEmotion != null) {
            imgPillEmotion.setVisibility(View.VISIBLE);
            imgPillEmotion.setImageResource(emotionToDrawable(selectedEmotion));
        } else {
            imgPillEmotion.setVisibility(View.GONE);
        }

        if (selectedIntensity != -1) {
            miniGlass.setVisibility(View.VISIBLE);
            updateMiniGlass(selectedIntensity);
        } else {
            miniGlass.setVisibility(View.GONE);
        }

        if (selectedTime == TimeFilter.LAST_7_DAYS) {
            txtPillTime.setVisibility(View.VISIBLE);
            txtPillTime.setText("7d");
        } else if (selectedTime == TimeFilter.LAST_30_DAYS) {
            txtPillTime.setVisibility(View.VISIBLE);
            txtPillTime.setText("30d");
        } else {
            txtPillTime.setVisibility(View.GONE);
        }
    }

    private void updateMiniGlass(int intensity) {

        // miniGlass is 36dp tall → get actual pixel height
        int glassHeightPx = miniGlass.getHeight();

        if (glassHeightPx == 0) {
            // Layout not measured yet → retry after layout pass
            miniGlass.post(() -> updateMiniGlass(intensity));
            return;
        }

        float percent;

        switch (intensity) {
            case 1: percent = 0.2f; break;
            case 2: percent = 0.4f; break;
            case 3: percent = 0.6f; break;
            case 4: percent = 0.8f; break;
            default: percent = 1.0f; break; // level 5 = FULL
        }

        int fillHeight = Math.round(glassHeightPx * percent);

        ViewGroup.LayoutParams params = miniGlassFill.getLayoutParams();
        params.height = fillHeight;
        miniGlassFill.setLayoutParams(params);

        // Keep your existing color logic
        int bgRes;
        switch (intensity) {
            case 1: bgRes = R.drawable.intensity_fill_level1; break;
            case 2: bgRes = R.drawable.intensity_fill_level2; break;
            case 3: bgRes = R.drawable.intensity_fill_level3; break;
            case 4: bgRes = R.drawable.intensity_fill_level4; break;
            default: bgRes = R.drawable.intensity_fill_level5; break;
        }

        miniGlassFill.setBackgroundResource(bgRes);
    }




}
