package com.example.asdproject.view.activities;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.asdproject.R;
import com.example.asdproject.model.EmotionLogDraft;
import com.example.asdproject.view.fragments.Step1SituationFragment;

/**
 * Activity that manages the multi-step emotion logging flow.
 * Each step is represented by a fragment responsible for collecting
 * a single component of the final log (situation, location, feeling, etc.).
 *
 * User selections are stored in an EmotionLogDraft object until the
 * final confirmation step. The completed data will later be converted
 * into an EmotionLog and saved to Firestore.
 *
 * This version implements Step 1 only. Remaining fragments will be added gradually.
 */
public class EmotionLogActivity extends AppCompatActivity
        implements Step1SituationFragment.Listener {

    /** Draft object used to store all user inputs across steps before final submission. */
    private final EmotionLogDraft draft = new EmotionLogDraft();

    /** Firestore child document ID associated with the logged-in child. */
    private String childId;

    /** Current step number in the logging workflow (1–7). */
    private int currentStep = 1;

    /** UI label displaying the current step count (e.g., "Step 1 of 7"). */
    private TextView txtStepIndicator;

    /** Back button used for step navigation. */
    private ImageView btnBack;

    /** Visual progress bar fill used to indicate step progression. */
    private View stepProgressFill;

    /** Total number of steps in the logging flow. */
    private static final int TOTAL_STEPS = 7;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emotion_log);

        // Retrieve child identifier from the previous activity.
        childId = getIntent().getStringExtra("childId");

        // Initialize UI elements.
        txtStepIndicator = findViewById(R.id.txtStepIndicator);
        btnBack = findViewById(R.id.btnBack);
        stepProgressFill = findViewById(R.id.stepProgressFill);

        // Configure back button behavior.
        btnBack.setOnClickListener(v -> handleBack());

        // Display the first step.
        showStep(1);
    }

    /**
     * Handles backward navigation.
     * If the user is on the first step, the activity is closed.
     */
    private void handleBack() {
        if (currentStep == 1) {
            finish();
            return;
        }
        showStep(currentStep - 1);
    }

    /**
     * Updates the step indicator text.
     */
    private void updateStepIndicator() {
        txtStepIndicator.setText("Step " + currentStep + " of " + TOTAL_STEPS);
    }

    /**
     * Updates the progress bar width based on the current step.
     */
    private void updateProgressBar() {
        float fraction = (float) currentStep / TOTAL_STEPS;

        // The update is posted to ensure the parent width is known before computing the fill width.
        stepProgressFill.post(() -> {
            View parentBar = findViewById(R.id.stepProgressBar);
            int totalWidth = parentBar.getWidth();
            int newWidth = (int) (totalWidth * fraction);

            ViewGroup.LayoutParams params = stepProgressFill.getLayoutParams();
            params.width = newWidth;
            stepProgressFill.setLayoutParams(params);
        });
    }

    /**
     * Displays the fragment associated with the requested step number.
     *
     * @param step the step number to load
     */
    public void showStep(int step) {
        currentStep = step;
        updateStepIndicator();
        updateProgressBar();

        switch (step) {
            case 1:
                replaceFragment(new Step1SituationFragment());
                break;

            // Steps 2–7 will be implemented progressively.
        }
    }

    /**
     * Replaces the fragment displayed in the fragment container.
     *
     * @param fragment the fragment instance to display
     */
    private void replaceFragment(androidx.fragment.app.Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }

    /**
     * Callback from Step 1 fragment.
     * Saves the selected situation and moves to Step 2.
     *
     * @param situation the situation selected by the user
     */
    @Override
    public void onSituationSelected(String situation) {
        draft.situation = situation;
        showStep(2);
    }
}
