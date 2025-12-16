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
import com.example.asdproject.view.fragments.CustomLocationFragment;
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
 Each step is implemented as a fragment and collects one part of the log:
 *  1 → Situation
 *  2 → Location
 *  3 → Feeling
 *  4 → Intensity
 *  5 → Photo (optional)
 *  6 → Notes (optional)
 *  7 → Review + Save
 * All inputs are stored in an EmotionLogDraft object until the flow is complete.
 * Only when the child confirms in Step 7 do we create an EmotionLog and save it.
 */
public class EmotionLogActivity extends AppCompatActivity
        implements Step1SituationFragment.Listener,
        CustomSituationFragment.Listener,
        Step2WhereFragment.Listener,
        CustomLocationFragment.Listener,
        Step3FeelingFragment.Listener,
        Step4IntensityFragment.Listener,
        Step5PhotoFragment.Listener,
        Step6NoteFragment.Listener,
        Step7ReviewFragment.Listener {

    /** Temporary container for all user inputs */
    private final EmotionLogDraft draft = new EmotionLogDraft();

    /** Repository responsible for Firestore writes */
    private final EmotionRepository emotionRepository = new EmotionRepository();

    /** Firestore child document ID */
    private String childId;

    /** Current step index (1–7) */
    private int currentStep = 1;

    /** Shared UI elements */
    private TextView txtStepIndicator;
    private ImageView btnBack;
    private View stepProgressFill;

    private static final int TOTAL_STEPS = 7;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emotion_log);

        childId = getIntent().getStringExtra("childId");

        initViews();
        setupBackButton();

        showStep(1);
    }

    /** Binds shared views */
    private void initViews() {
        txtStepIndicator = findViewById(R.id.txtStepIndicator);
        btnBack = findViewById(R.id.btnBack);
        stepProgressFill = findViewById(R.id.stepProgressFill);
    }

    /** Handles back navigation */
    private void setupBackButton() {
        btnBack.setOnClickListener(v -> {
            if (currentStep == 1) {
                finish();
            } else {
                showStep(currentStep - 1);
            }
        });
    }

    /** Updates step label */
    private void updateStepIndicator() {
        txtStepIndicator.setText("Step " + currentStep + " of " + TOTAL_STEPS);
    }

    /** Updates progress bar width */
    private void updateProgressBar() {
        float fraction = (float) currentStep / TOTAL_STEPS;

        stepProgressFill.post(() -> {
            View bar = findViewById(R.id.stepProgressBar);
            int newWidth = (int) (bar.getWidth() * fraction);

            ViewGroup.LayoutParams params = stepProgressFill.getLayoutParams();
            params.width = newWidth;
            stepProgressFill.setLayoutParams(params);
        });
    }

    /** Central navigation method */
    public void showStep(int step) {
        currentStep = step;
        updateStepIndicator();
        updateProgressBar();

        switch (step) {
            case 1: replaceFragment(new Step1SituationFragment()); break;
            case 2: replaceFragment(new Step2WhereFragment()); break;
            case 3: replaceFragment(new Step3FeelingFragment()); break;
            case 4: replaceFragment(new Step4IntensityFragment()); break;
            case 5: replaceFragment(Step5PhotoFragment.newInstance(childId)); break;
            case 6: replaceFragment(new Step6NoteFragment()); break;
            case 7: replaceFragment(Step7ReviewFragment.newInstance(draft)); break;
        }
    }

    /** Fragment replacement helper */
    private void replaceFragment(androidx.fragment.app.Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }

    // -------------------------------------------------------------
    // Step callbacks
    // -------------------------------------------------------------

    @Override
    public void onSituationSelected(String situation) {
        draft.situation = situation;
        showStep(2);
    }

    @Override
    public void onRequestCustomSituation() {
        replaceFragment(new CustomSituationFragment());
    }

    @Override
    public void onCustomSituationEntered(String situation) {
        draft.situation = situation;
        showStep(2);
    }

    @Override
    public void onLocationSelected(String location) {
        draft.location = location;
        showStep(3);
    }

    @Override
    public void onRequestCustomLocation() {
        replaceFragment(new CustomLocationFragment());
    }

    @Override
    public void onCustomLocationEntered(String location) {
        draft.location = location;
        showStep(3);
    }

    @Override
    public void onBackToWhereStep() {
        showStep(2);
    }

    @Override
    public void onFeelingSelected(Feeling feeling) {
        draft.feeling = feeling.getLabel();
        showStep(4);
    }

    @Override
    public void onIntensitySelected(int intensityLevel) {
        draft.intensity = intensityLevel;
        showStep(5);
    }

    @Override
    public void onPhotoCaptured(String photoUrl) {
        draft.photoUri = photoUrl;
        showStep(6);
    }

    @Override
    public void onNoteEntered(String note) {
        draft.note = note;
        showStep(7);
    }

    @Override
    public void onReviewConfirmed() {
        saveFinalEmotionLog();
    }

    /** Converts draft → EmotionLog and saves it */
    private void saveFinalEmotionLog() {

        if (childId == null || childId.trim().isEmpty()) {
            Toast.makeText(this, "Error: Missing child information", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        EmotionLog finalLog = new EmotionLog(childId, draft);

        Toast.makeText(this, "Saving your feeling...", Toast.LENGTH_SHORT).show();
        emotionRepository.addEmotionLog(finalLog);

        finish();
    }
}
