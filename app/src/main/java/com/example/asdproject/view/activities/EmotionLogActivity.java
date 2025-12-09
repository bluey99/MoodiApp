package com.example.asdproject.view.activities;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.asdproject.R;
import com.example.asdproject.model.EmotionLogDraft;
import com.example.asdproject.model.Feeling;
import com.example.asdproject.view.fragments.Step1SituationFragment;
import com.example.asdproject.view.fragments.Step2WhereFragment;
import com.example.asdproject.view.fragments.Step3FeelingFragment;
import com.example.asdproject.view.fragments.Step4IntensityFragment;
import com.example.asdproject.view.fragments.Step5PhotoFragment;
import com.example.asdproject.view.fragments.Step6NoteFragment;

/**
 * Hosts the multi-step emotion logging flow for the child.
 * Each step is implemented as a fragment and collects one part of the log:
 *
 * 1 → Situation
 * 2 → Location
 * 3 → Feeling
 * 4 → Intensity
 * 5 → Photo
 * 6 → Notes (coming next)
 * 7 → Review + Save (coming later)
 *
 * All inputs are stored in an EmotionLogDraft object until the flow is complete.
 */
public class EmotionLogActivity extends AppCompatActivity
        implements Step1SituationFragment.Listener,
        Step2WhereFragment.Listener,
        Step3FeelingFragment.Listener,
        Step4IntensityFragment.Listener,
        Step5PhotoFragment.Listener,
        Step6NoteFragment.Listener {

    /** Temporary container for in-progress user inputs. */
    private final EmotionLogDraft draft = new EmotionLogDraft();

    /** Firestore ID of the current child. */
    private String childId;

    /** Current step index. */
    private int currentStep = 1;

    /** UI elements. */
    private TextView txtStepIndicator;
    private ImageView btnBack;
    private View stepProgressFill;

    /** Total number of steps in the wizard. */
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

    /** Connect views from layout. */
    private void initViews() {
        txtStepIndicator = findViewById(R.id.txtStepIndicator);
        btnBack = findViewById(R.id.btnBack);
        stepProgressFill = findViewById(R.id.stepProgressFill);
    }

    /** Handles back navigation between steps. */
    private void setupBackButton() {
        btnBack.setOnClickListener(v -> {
            if (currentStep == 1) {
                finish();
            } else {
                showStep(currentStep - 1);
            }
        });
    }

    /** Updates the step label text. */
    private void updateStepIndicator() {
        txtStepIndicator.setText("Step " + currentStep + " of " + TOTAL_STEPS);
    }

    /** Updates the horizontal progress bar. */
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
     * All step navigation routes through here.
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

            // Step 7 → Review (coming later)
        }
    }

    /** Replaces the visible fragment. */
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
    public void onLocationSelected(String location) {
        draft.location = location;
        showStep(3);
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
        draft.note = note;    // may be null
        showStep(7);          // next step: review screen (coming next)
    }

}
