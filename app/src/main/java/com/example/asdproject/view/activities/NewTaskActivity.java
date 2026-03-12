package com.example.asdproject.view.activities;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.asdproject.R;
import com.example.asdproject.util.LocaleManager;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class NewTaskActivity extends BaseActivity {

    private EditText edtTaskName, edtDisplayWhen, edtDiscussionPrompts;
    private Button btnSaveTask, btnGoBack;

    private final Calendar selectedDateTime = Calendar.getInstance();

    private final SimpleDateFormat format =
            new SimpleDateFormat("d/M/yyyy, h:mma", Locale.getDefault());

    private FirebaseFirestore db;

    private String parentId;
    private String childId;   // FIELD childID value
    private String childName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_task);

        db = FirebaseFirestore.getInstance();

        parentId = getIntent().getStringExtra("PARENT_ID");
        childId  = getIntent().getStringExtra("CHILD_ID");
        childName = getIntent().getStringExtra("CHILD_NAME");

        // Title (localized)
        if (childName != null && !childName.isEmpty()) {
            setTitle(getString(R.string.new_task_title_with_child, childName));
        } else {
            setTitle(getString(R.string.new_task_title));
        }

        // 🌐 language button
        TextView btnLanguage = findViewById(R.id.btnLanguage);
        btnLanguage.setOnClickListener(v -> {
            LocaleManager.toggleLanguage(this);
            recreate();
        });

        edtTaskName = findViewById(R.id.edtTaskName);
        edtDisplayWhen = findViewById(R.id.edtDisplayWhen);
        edtDiscussionPrompts = findViewById(R.id.edtDiscussionPrompts);
        btnSaveTask = findViewById(R.id.btnSaveTask);
        btnGoBack = findViewById(R.id.btnGoBack);

        edtDisplayWhen.setFocusable(false);
        edtDisplayWhen.setClickable(true);
        edtDisplayWhen.setOnClickListener(v -> openDatePicker());

        btnGoBack.setOnClickListener(v -> finish());
        btnSaveTask.setOnClickListener(v -> validateAndSendTask());
    }

    private void validateAndSendTask() {

        String taskName = edtTaskName.getText().toString().trim();
        String displayWhen = edtDisplayWhen.getText().toString().trim();
        String discussionPrompts = edtDiscussionPrompts.getText().toString().trim();

        if (TextUtils.isEmpty(taskName) ||
                TextUtils.isEmpty(displayWhen) ||
                TextUtils.isEmpty(discussionPrompts)) {

            Toast.makeText(this, getString(R.string.new_task_error_fill_all), Toast.LENGTH_SHORT).show();
            return;
        }

        if (parentId == null || parentId.isEmpty()) {
            Toast.makeText(this, getString(R.string.new_task_error_parent_missing), Toast.LENGTH_SHORT).show();
            return;
        }

        if (childId == null || childId.isEmpty()) {
            Toast.makeText(this, getString(R.string.new_task_error_select_child), Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedDateTime.getTimeInMillis() < System.currentTimeMillis()) {
            Toast.makeText(this, getString(R.string.new_task_error_future_time), Toast.LENGTH_SHORT).show();
            return;
        }

        saveTaskToFirestore(taskName, displayWhen, discussionPrompts, parentId, childId);
    }

    private void saveTaskToFirestore(String taskName,
                                     String displayWhen,
                                     String discussionPrompts,
                                     String parentId,
                                     String childId) {

        Map<String, Object> task = new HashMap<>();
        task.put("taskName", taskName);
        task.put("displayWhen", displayWhen);
        task.put("discussionPrompts", discussionPrompts);

        // ✅ keep consistent with children field name
        task.put("childID", childId);

        task.put("creatorType", "PARENT");
        task.put("creatorId", parentId);
        task.put("status", "ASSIGNED");

        db.collection("tasks")
                .add(task)
                .addOnSuccessListener(docRef -> {
                    Toast.makeText(this, getString(R.string.new_task_sent_success), Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                getString(R.string.new_task_failed_prefix) + " " + e.getMessage(),
                                Toast.LENGTH_SHORT).show()
                );
    }

    private void openDatePicker() {
        Calendar now = Calendar.getInstance();

        DatePickerDialog dp = new DatePickerDialog(
                this,
                (view, year, month, day) -> {
                    selectedDateTime.set(Calendar.YEAR, year);
                    selectedDateTime.set(Calendar.MONTH, month);
                    selectedDateTime.set(Calendar.DAY_OF_MONTH, day);
                    openTimePicker();
                },
                now.get(Calendar.YEAR),
                now.get(Calendar.MONTH),
                now.get(Calendar.DAY_OF_MONTH)
        );

        dp.getDatePicker().setMinDate(now.getTimeInMillis());
        dp.show();
    }

    private void openTimePicker() {
        Calendar now = Calendar.getInstance();

        TimePickerDialog tp = new TimePickerDialog(
                this,
                (view, hour, minute) -> {

                    Calendar candidate = (Calendar) selectedDateTime.clone();
                    candidate.set(Calendar.HOUR_OF_DAY, hour);
                    candidate.set(Calendar.MINUTE, minute);
                    candidate.set(Calendar.SECOND, 0);
                    candidate.set(Calendar.MILLISECOND, 0);

                    Calendar today = Calendar.getInstance();
                    boolean sameDay =
                            candidate.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                                    candidate.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR);

                    if (sameDay && candidate.getTimeInMillis() < System.currentTimeMillis()) {
                        Toast.makeText(this, getString(R.string.new_task_error_choose_future_time), Toast.LENGTH_SHORT).show();
                        openTimePicker();
                        return;
                    }

                    selectedDateTime.setTimeInMillis(candidate.getTimeInMillis());
                    edtDisplayWhen.setText(format.format(selectedDateTime.getTime()));
                },
                now.get(Calendar.HOUR_OF_DAY),
                now.get(Calendar.MINUTE),
                false
        );

        tp.show();
    }
}