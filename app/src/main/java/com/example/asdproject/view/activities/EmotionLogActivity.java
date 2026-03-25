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
import com.example.asdproject.view.fragments.CustomCompanionBottomSheet;
import com.example.asdproject.view.fragments.CustomLocationBottomSheet;
import com.example.asdproject.view.fragments.CustomSituationBottomSheet;
import com.example.asdproject.view.fragments.Step1SituationFragment;
import com.example.asdproject.view.fragments.Step2WhereFragment;
import com.example.asdproject.view.fragments.Step4FeelingFragment;
import com.example.asdproject.view.fragments.CustomFeelingBottomSheet;
import com.example.asdproject.view.fragments.Step3WhoWithFragment;
import com.example.asdproject.view.fragments.Step5IntensityFragment;
import com.example.asdproject.view.fragments.Step6PhotoFragment;
import com.example.asdproject.view.fragments.Step7NoteFragment;
import com.example.asdproject.view.fragments.Step8ReviewFragment;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * Hosts the multi-step emotion logging flow for the child.
 * Steps:
 *  1 → Situation
 *  2 → Location
 *  3 → Companion
 *  4 → Feeling
 *  5 → Intensity
 *  6 → Photo (optional)
 *  7 → Notes (optional) / Discussion Prompts (for tasks)
 *  8 → Review + Save
 */
public class EmotionLogActivity extends AppCompatActivity
        implements Step1SituationFragment.Listener,
        CustomSituationBottomSheet.Listener,
        Step2WhereFragment.Listener,
        CustomLocationBottomSheet.Listener,
        Step3WhoWithFragment.Listener,
        CustomCompanionBottomSheet.Listener,
        Step4FeelingFragment.Listener,
        CustomFeelingBottomSheet.Listener,
        Step5IntensityFragment.Listener,
        Step6PhotoFragment.Listener,
        Step7NoteFragment.Listener,
        Step6ForTasksActivity.Listener,
        Step8ReviewFragment.Listener {

    /** Temporary container for all user inputs */
    private final EmotionLogDraft draft = new EmotionLogDraft();

    /** Repository responsible for Firestore writes */
    private final EmotionRepository emotionRepository = new EmotionRepository();

    /** childID FIELD value (not necessarily Firestore doc id) */
    private String childId;

    /** Current step index (1–8) */
    private int currentStep = 1;

    /** Shared UI elements */
    private TextView txtStepIndicator;
    private ImageView btnBack;
    private View stepProgressFill;
    private static final int TOTAL_STEPS = 8;
    private View headerView;

    private int startStep = 1;
    private String logType = "SELF"; // default

    private String discussionPrompts;

    // needed for task completion notification
    private String taskId;
    private String taskTitle;
    //  stores typed feeling when the child chooses OTHER
    private String customFeelingText = null;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emotion_log);

        db = FirebaseFirestore.getInstance();

        // childId comes from intent (your flow currently uses "childId")
        childId = getIntent().getStringExtra("childId");

        logType = getIntent().getStringExtra("LOG_TYPE");
        if (logType == null) logType = "SELF";

        discussionPrompts = getIntent().getStringExtra("discussionPrompts");

        // task extras (only for TASK)
        taskId = getIntent().getStringExtra("taskId");
        taskTitle = getIntent().getStringExtra("taskTitle");

        startStep = "TASK".equals(logType) ? 2 : 1;

        if ("TASK".equals(logType)) {
            draft.situation = null; // tasks don't use situation
            draft.location = "From task";
        }

        initViews();
        setupBackButton();
        showStep(startStep);
    }

    private void initViews() {
        headerView = findViewById(R.id.header);

        btnBack = headerView.findViewById(R.id.btnBack);
        TextView txtHeaderTitle = headerView.findViewById(R.id.txtHeaderTitle);
        txtHeaderTitle.setText(getString(R.string.emotion_log_header_title));

        txtStepIndicator = findViewById(R.id.txtStepIndicator);
        stepProgressFill = findViewById(R.id.stepProgressFill);
    }

    private void setupBackButton() {
        btnBack.setOnClickListener(v -> {

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

    private void updateStepIndicator() {
        txtStepIndicator.setText(
                getString(R.string.emotion_log_step_indicator, currentStep, TOTAL_STEPS)
        );
    }

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

    public void showStep(int step) {
        currentStep = step;
        updateStepIndicator();
        updateProgressBar();

        switch (step) {
            case 1: replaceFragment(new Step1SituationFragment()); break;
            case 2: replaceFragment(new Step2WhereFragment()); break;

// Step 3 (who was with you)
            case 3: replaceFragment(new Step3WhoWithFragment()); break;

// feeling becomes step 4
            case 4: replaceFragment(new Step4FeelingFragment()); break;

// intensity becomes step 5
            case 5: replaceFragment(new Step5IntensityFragment()); break;

// photo becomes step 6
            case 6: replaceFragment(Step6PhotoFragment.newInstance(childId)); break;

// note / task becomes step 7
            case 7:
                if ("TASK".equals(logType)) {
                    replaceFragment(Step6ForTasksActivity.newInstance(discussionPrompts));
                } else {
                    replaceFragment(new Step7NoteFragment());
                }
                break;

// review becomes step 8
            case 8: replaceFragment(Step8ReviewFragment.newInstance(draft)); break;
        }
    }

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
        sheet.show(getSupportFragmentManager(), "CustomSituationBottomSheet");
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
    public void onCompanionSelected(String companion) {
        draft.companion = companion;
        showStep(4);
    }

    @Override
    public void onRequestCustomCompanion() {
        new CustomCompanionBottomSheet()
                .show(getSupportFragmentManager(), "CustomCompanionBottomSheet");
    }

    @Override
    public void onCustomCompanionEntered(String companion) {
        draft.companion = companion.trim();
        showStep(4);
    }

    @Override
    public void onFeelingSelected(Feeling feeling) {
        draft.feeling = feeling.name();
        showStep(5);
    }

    @Override
    public void onRequestCustomFeeling() {
        // bayan added here - open custom feeling bottom sheet
        new CustomFeelingBottomSheet()
                .show(getSupportFragmentManager(), "CustomFeelingBottomSheet");
    }

    @Override
    public void onCustomFeelingEntered(String feelingText) {
        // save typed feeling directly (same pattern as situation/location)
        draft.feeling = feelingText.trim();
        showStep(5);
    }

    @Override
    public void onIntensitySelected(int intensityLevel) {
        draft.intensity = intensityLevel;
        showStep(6);
    }

    @Override
    public void onPhotoCaptured(String photoUrl) {
        draft.photoUri = photoUrl;
        showStep(7);
    }

    @Override
    public void onNoteEntered(String note) {
        draft.note = note;
        showStep(8);
    }

    @Override
    public void onTaskAnswerEntered(String answer) {
        draft.note = answer;
        showStep(8);
    }

    @Override
    public void onReviewConfirmed() {
        saveFinalEmotionLog();
    }

    private void saveFinalEmotionLog() {

        if (childId == null || childId.trim().isEmpty()) {
            Toast.makeText(this, "Error: Missing child information", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        EmotionLog finalLog = new EmotionLog(childId, draft);
        if ("TASK".equals(logType)) {
            finalLog.setLogType("TASK");
            finalLog.setTaskPrompt(discussionPrompts);
        } else {
            finalLog.setLogType("SELF");
        }

        //  mark emotion log as task-based if created from a task
        if ("TASK".equals(logType)) {
            finalLog.setId("TASK");
        }


        Toast.makeText(this, "Saving your feeling...", Toast.LENGTH_SHORT).show();
        emotionRepository.addEmotionLog(
                finalLog,

                // SUCCESS
                () -> {
                    if ("TASK".equals(logType) && taskId != null && !taskId.trim().isEmpty()) {
                        markTaskCompletedAndNotify();
                    }
                    finish();
                },

                // FAILURE
                () -> Toast.makeText(
                        this,
                        "Failed to save your feeling. Task not completed.",
                        Toast.LENGTH_LONG
                ).show()
        );
    }

    // -------------------------------------------------------------
    // Mark task completed + create notification
    // message uses REAL child name (Ali), not "Child"
    // -------------------------------------------------------------
    private void markTaskCompletedAndNotify() {

        db.collection("tasks")
                .document(taskId)
                .get()
                .addOnSuccessListener(taskDoc -> {

                    if (taskDoc == null || !taskDoc.exists()) return;

                    // who created it
                    String creatorTypeTmp = taskDoc.getString("creatorType");
                    String creatorIdTmp = taskDoc.getString("creatorId");

                    // fallback for old tasks
                    if (creatorTypeTmp == null || creatorTypeTmp.trim().isEmpty()) creatorTypeTmp = "PARENT";
                    if (creatorIdTmp == null || creatorIdTmp.trim().isEmpty()) creatorIdTmp = taskDoc.getString("parentId");

                    // task name
                    String taskNameTmp = taskDoc.getString("taskName");
                    if (taskNameTmp == null || taskNameTmp.trim().isEmpty()) {
                        taskNameTmp = (taskTitle != null && !taskTitle.trim().isEmpty()) ? taskTitle : "Task";
                    }

                    final String creatorType = creatorTypeTmp;
                    final String creatorId = creatorIdTmp;
                    final String taskName = taskNameTmp;

                    // 1) update status -> COMPLETED
                    Map<String, Object> update = new HashMap<>();
                    update.put("status", "COMPLETED");
                    db.collection("tasks").document(taskId).update(update);

                    // 2) get child name by FIELD childID (not document(childId))
                    db.collection("children")
                            .whereEqualTo("childID", childId)
                            .limit(1)
                            .get()
                            .addOnSuccessListener(qs -> {

                                String childName = "Child";

                                if (qs != null && !qs.isEmpty()) {
                                    DocumentSnapshot childDoc = qs.getDocuments().get(0);

                                    String n = childDoc.getString("name");
                                    if (n == null || n.trim().isEmpty()) {
                                        n = childDoc.getString("childName"); // fallback if your DB uses childName
                                    }
                                    if (n != null && !n.trim().isEmpty()) {
                                        childName = n.trim();
                                    }
                                }

                                // 3) write notification document
                                Map<String, Object> notif = new HashMap<>();
                                notif.put("receiverType", creatorType);
                                notif.put("receiverId", creatorId);
                                notif.put("read", false);
                                notif.put("message", childName + " finished the task: " + taskName);
                                notif.put("createdAt", System.currentTimeMillis());

                                db.collection("notifications").add(notif);
                            });

                })
                .addOnFailureListener(e -> {
                    // don't crash if notification fails
                });
    }




}
