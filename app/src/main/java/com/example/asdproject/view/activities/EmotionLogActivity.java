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
import com.example.asdproject.view.fragments.CustomLocationBottomSheet;
import com.example.asdproject.view.fragments.CustomSituationBottomSheet;
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
 *  6 → Notes (optional) / Discussion Prompts (for tasks)
 *  7 → Review + Save
 * All inputs are stored in an EmotionLogDraft object until the flow is complete.
 * Only when the child confirms in Step 7 do we create an EmotionLog and save it.
 */
public class EmotionLogActivity extends AppCompatActivity
        implements Step1SituationFragment.Listener,
        CustomSituationBottomSheet.Listener,
        Step2WhereFragment.Listener,
        CustomLocationBottomSheet.Listener,
        Step3FeelingFragment.Listener,
        Step4IntensityFragment.Listener,
        Step5PhotoFragment.Listener,
        Step6NoteFragment.Listener,
        Step6ForTasksActivity.Listener,
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
    private View headerView;

    // ✅ controls whether we start from step 1 or step 3
    private int startStep = 1;
    private String logType = "SELF"; // default

    // ✅ discussion prompts (only for TASK)
    private String discussionPrompts;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emotion_log);

        childId = getIntent().getStringExtra("childId");

        // ✅ Determine mode (SELF default)
        logType = getIntent().getStringExtra("LOG_TYPE");
        if (logType == null) logType = "SELF";

        // ✅ Get prompts from task screen (may be null for SELF)
        discussionPrompts = getIntent().getStringExtra("discussionPrompts");

        // ✅ If TASK → start directly at Feeling page (Step 3)
        startStep = "TASK".equals(logType) ? 2 : 1;

        // ✅ (Optional) prefill for TASK so review isn't empty
        if ("TASK".equals(logType)) {
            String taskTitle = getIntent().getStringExtra("taskTitle");

            draft.situation = (taskTitle != null && !taskTitle.trim().isEmpty())
                    ? ("Task: " + taskTitle)
                    : "Task";
            draft.location = "From task";
        }

        initViews();
        setupBackButton();

        showStep(startStep);
    }

    /** Binds shared views */
    private void initViews() {

        // Header include
        headerView = findViewById(R.id.header);

        // Header views
        btnBack = headerView.findViewById(R.id.btnBack);
        TextView txtHeaderTitle = headerView.findViewById(R.id.txtHeaderTitle);
        txtHeaderTitle.setText("Log My Feelings");

        // Step UI
        txtStepIndicator = findViewById(R.id.txtStepIndicator);
        stepProgressFill = findViewById(R.id.stepProgressFill);
    }


    /** Handles back navigation */
    private void setupBackButton() {
        btnBack.setOnClickListener(v -> {

            // ✅ If we started at step 3 (task logging), back exits
            if (currentStep == startStep) {
                finish();
                return;
            }

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

            // ✅ Step 6: if TASK use Step6ForTasks, else normal Step6NoteFragment
            case 6:
                if ("TASK".equals(logType)) {
                    replaceFragment(Step6ForTasksActivity.newInstance(discussionPrompts));
                } else {
                    replaceFragment(new Step6NoteFragment());
                }
                break;

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
        CustomSituationBottomSheet sheet = new CustomSituationBottomSheet();
        sheet.show(
                getSupportFragmentManager(),
                "CustomSituationBottomSheet"
        );
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
        new CustomLocationBottomSheet()
                .show(getSupportFragmentManager(), "CustomLocationBottomSheet");
    }

    @Override
    public void onCustomLocationEntered(String location) {
        draft.location = location;
        showStep(3);
    }

    @Override
    public void onFeelingSelected(Feeling feeling) {
        draft.feeling = feeling.name();
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

    // SELF Step 6 callback
    @Override
    public void onNoteEntered(String note) {
        draft.note = note;
        showStep(7);
    }

    // ✅ TASK Step 6 callback (answers for prompts)
    @Override
    public void onTaskAnswerEntered(String answer) {
        draft.note = answer; // store answers inside note to keep everything else unchanged
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
