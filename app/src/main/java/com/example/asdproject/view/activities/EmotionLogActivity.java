package com.example.asdproject.view.activities;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.asdproject.R;
import com.example.asdproject.controller.EmotionRepository;
import com.example.asdproject.model.EmotionLog;
import com.example.asdproject.model.EmotionLogDraft;
import com.example.asdproject.model.Feeling;
import com.example.asdproject.view.fragments.CustomSituationFragment;
import com.example.asdproject.view.fragments.Step1SituationFragment;
import com.example.asdproject.view.fragments.Step2WhereFragment;
import com.example.asdproject.view.fragments.Step3FeelingFragment;
import com.example.asdproject.view.fragments.Step4IntensityFragment;
import com.example.asdproject.view.fragments.Step5PhotoFragment;
import com.example.asdproject.view.fragments.Step6NoteFragment;
import com.example.asdproject.view.fragments.Step7ReviewFragment;

/**
 * Hosts the multi-step emotion logging flow for the child.
 * Each step is implemented as a fragment and collects one part of the log:
 *
 *  1 → Situation
 *  2 → Location
 *  3 → Feeling
 *  4 → Intensity
 *  5 → Photo (optional)
 *  6 → Notes (optional)
 *  7 → Review + Save
 *
 * All inputs are stored in an EmotionLogDraft object until the flow is complete.
 * Only when the child confirms in Step 7 do we create an EmotionLog and save it.
 */
public class EmotionLogActivity extends AppCompatActivity
        implements Step1SituationFragment.Listener,
        CustomSituationFragment.Listener,
        Step2WhereFragment.Listener,
        Step3FeelingFragment.Listener,
        Step4IntensityFragment.Listener,
        Step5PhotoFragment.Listener,
        Step6NoteFragment.Listener,
        Step7ReviewFragment.Listener {

    /** Temporary container for in-progress user inputs across steps. */
    private final EmotionLogDraft draft = new EmotionLogDraft();

    /** Repository that knows how to save EmotionLog objects to Firestore. */
    private final EmotionRepository emotionRepository = new EmotionRepository();

    /** Firestore ID of the current child, passed from previous screen. */
    private String childId;

    /** Current step index (1–7). */
    private int currentStep = 1;

    /** UI elements shared across steps. */
    private TextView txtStepIndicator;
    private ImageView btnBack;
    private View stepProgressFill;

    /** Total number of steps in the wizard. */
    private static final int TOTAL_STEPS = 7;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emotion_log);

        // Child ID is required so we can store under children/{childId}/history
        childId = getIntent().getStringExtra("childId");

        initViews();
        setupBackButton();

        // Start at Step 1
        showStep(1);
    }

    /** Connect shared UI elements from the layout. */
    private void initViews() {
        txtStepIndicator = findViewById(R.id.txtStepIndicator);
        btnBack = findViewById(R.id.btnBack);
        stepProgressFill = findViewById(R.id.stepProgressFill);
    }

    /** Handles back navigation between steps or closes the flow on Step 1. */
    private void setupBackButton() {
        btnBack.setOnClickListener(v -> {
            if (currentStep == 1) {
                // If we are at the first step → exit the flow
                finish();
            } else {
                // Otherwise go back one step
                showStep(currentStep - 1);
            }
        });
    }

    /** Updates the "Step X of Y" label. */
    private void updateStepIndicator() {
        txtStepIndicator.setText("Step " + currentStep + " of " + TOTAL_STEPS);
    }

    /** Updates the horizontal progress bar based on the current step. */
    private void updateProgressBar() {
        float fraction = (float) currentStep / TOTAL_STEPS;

        stepProgressFill.post(() -> {
            View bar = findViewById(R.id.stepProgressBar);
            int totalWidth = bar.getWidth();
            int newWidth = (int) (totalWidth * fraction);

            ViewGroup.LayoutParams params = stepProgressFill.getLayoutParams();
            params.width = newWidth;
            stepProgressFill.setLayoutParams(params);
        });
    }

    /**
     * Displays the fragment for the selected step.
     * All navigation (Next / Back) routes through here.
     */
    public void showStep(int step) {
        currentStep = step;
        updateStepIndicator();
        updateProgressBar();

        switch (step) {
            case 1:
                replaceFragment(new Step1SituationFragment());
                break;
            case 2:
                replaceFragment(new Step2WhereFragment());
                break;
            case 3:
                replaceFragment(new Step3FeelingFragment());
                break;
            case 4:
                replaceFragment(new Step4IntensityFragment());
                break;
            case 5:
                replaceFragment(Step5PhotoFragment.newInstance(childId));
                break;
            case 6:
                replaceFragment(new Step6NoteFragment());
                break;
            case 7:
                // Pass the full draft to the review screen
                replaceFragment(Step7ReviewFragment.newInstance(draft));
                break;
        }
    }

    /** Replaces the visible fragment inside the container. */
    private void replaceFragment(androidx.fragment.app.Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }

    // -------------------------------------------------------------
    // Step callbacks (called by each Fragment)
    // -------------------------------------------------------------

    @Override
    public void onSituationSelected(String situation) {
        draft.situation = situation;
        showStep(2);
    }

    @Override
    public void onLocationSelected(String location) {
        draft.location = location;
        showStep(3);
    }

    @Override
    public void onFeelingSelected(Feeling feeling) {
        // Store label only (e.g., "Angry", "Happy")
        draft.feeling = feeling.getLabel();
        showStep(4);
    }

    @Override
    public void onIntensitySelected(int intensityLevel) {
        draft.intensity = intensityLevel;   // 1–5
        showStep(5);
    }

    @Override
    public void onPhotoCaptured(String photoUrl) {
        // May be null if child chose "Skip" in Step 5
        draft.photoUri = photoUrl;
        showStep(6);
    }

    @Override
    public void onNoteEntered(String note) {
        // May be empty or null → Step 6 is optional
        draft.note = note;
        showStep(7);
    }

    /**
     * Final callback from Step 7 when the child presses "Submit".
     * Here we:
     *  - Convert the draft into a final EmotionLog
     *  - Ask EmotionRepository to save it to Firestore
     *  - Close the flow
     */
    @Override
    public void onReviewConfirmed() {
        saveFinalEmotionLog();
    }

    @Override
    public void onRequestCustomSituation() {
        replaceFragment(new CustomSituationFragment());
    }

    @Override
    public void onCustomSituationEntered(String situation) {
        draft.situation = situation;
        showStep(2);  // Continue to Step 2
    }

    /**
     * Converts the current EmotionLogDraft into a final EmotionLog
     * and saves it to Firestore via EmotionRepository.
     */
    private void saveFinalEmotionLog() {

        if (childId == null || childId.trim().isEmpty()) {
            Toast.makeText(this, "Error: Missing child information", Toast.LENGTH_SHORT).show();
            // We cannot save without a child ID
            finish();
            return;
        }

        // Build final Firestore-ready model from the draft
        EmotionLog finalLog = new EmotionLog(childId, draft);

        // Very simple UX for now; can be improved with a "success" screen
        Toast.makeText(this, "Saving your feeling...", Toast.LENGTH_SHORT).show();

        // Fire-and-forget save. EmotionRepository logs success/failure.
        emotionRepository.addEmotionLog(finalLog);

        // Close the flow and return to previous screen
        finish();
    }
}
