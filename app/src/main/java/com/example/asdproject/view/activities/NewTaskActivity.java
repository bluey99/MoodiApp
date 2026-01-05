package com.example.asdproject.view.activities;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.asdproject.R;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class NewTaskActivity extends AppCompatActivity {

    private EditText edtTaskName, edtDisplayWhen, edtDiscussionPrompts;
    private Button btnSaveTask, btnGoBack;

    private final Calendar selectedDateTime = Calendar.getInstance();

    // Example: "9/12/2025, 4:30PM"
    private final SimpleDateFormat format =
            new SimpleDateFormat("d/M/yyyy, h:mma", Locale.getDefault());

    private FirebaseFirestore db;

    // 👇 from ParentHomeActivity
    private String parentId;
    private String childId;
    private String childName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_task);

        db = FirebaseFirestore.getInstance();

        // get IDs from intent
        parentId = getIntent().getStringExtra("PARENT_ID");
        childId  = getIntent().getStringExtra("CHILD_ID");
        childName = getIntent().getStringExtra("CHILD_NAME");

        if (childName != null && !childName.isEmpty()) {
            setTitle("New Task – " + childName);
        } else {
            setTitle("New Task");
        }

        edtTaskName = findViewById(R.id.edtTaskName);
        edtDisplayWhen = findViewById(R.id.edtDisplayWhen);
        edtDiscussionPrompts = findViewById(R.id.edtDiscussionPrompts);
        btnSaveTask = findViewById(R.id.btnSaveTask);
        btnGoBack = findViewById(R.id.btnGoBack);

        // date & time picker instead of typing
        edtDisplayWhen.setFocusable(false);
        edtDisplayWhen.setClickable(true);
        edtDisplayWhen.setOnClickListener(v -> openDatePicker());

        btnGoBack.setOnClickListener(v -> finish());

        btnSaveTask.setOnClickListener(v -> validateAndSendTask());
    }

    // ----------------------------------------------------
    // Validate input and send task to THIS child only
    // ----------------------------------------------------
    private void validateAndSendTask() {

        String taskName = edtTaskName.getText().toString().trim();
        String displayWhen = edtDisplayWhen.getText().toString().trim();
        String discussionPrompts = edtDiscussionPrompts.getText().toString().trim();

        if (TextUtils.isEmpty(taskName) ||
                TextUtils.isEmpty(displayWhen) ||
                TextUtils.isEmpty(discussionPrompts)) {

            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (parentId == null || parentId.isEmpty()) {
            Toast.makeText(this, "Parent not logged in (missing parent id)", Toast.LENGTH_SHORT).show();
            return;
        }

        if (childId == null || childId.isEmpty()) {
            Toast.makeText(this, "Please select a child in the parent home screen first", Toast.LENGTH_SHORT).show();
            return;
        }

        // block past date/time (extra protection)
        if (selectedDateTime.getTimeInMillis() < System.currentTimeMillis()) {
            Toast.makeText(this, "Please choose a future date/time", Toast.LENGTH_SHORT).show();
            return;
        }

        // directly save to Firestore with this childId
        saveTaskToFirestore(taskName, displayWhen, discussionPrompts, parentId, childId);
    }

    // ----------------------------------------------------
    // Save task (child-specific)
    // ----------------------------------------------------
    private void saveTaskToFirestore(String taskName,
                                     String displayWhen,
                                     String discussionPrompts,
                                     String parentId,
                                     String childId) {

        Map<String, Object> task = new HashMap<>();
        task.put("taskName", taskName);
        task.put("displayWhen", displayWhen);
        task.put("discussionPrompts", discussionPrompts);
        task.put("childId", childId);
        task.put("parentId", parentId);
        task.put("createdAt", System.currentTimeMillis());

        // keep this if your old DB already has the field
        task.put("displayWhere", "");

        db.collection("tasks")
                .add(task)
                .addOnSuccessListener(docRef -> {
                    Toast.makeText(this, "Task sent successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to send task: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    // -------------------------------------------
    // DATE PICKER (no past dates)
    // -------------------------------------------
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

    // ----------------------------------------------------
    // TIME PICKER (no past time if same day)
    // ----------------------------------------------------
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

                    // If chosen date is today, block past times
                    Calendar today = Calendar.getInstance();
                    boolean sameDay =
                            candidate.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                                    candidate.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR);

                    if (sameDay && candidate.getTimeInMillis() < System.currentTimeMillis()) {
                        Toast.makeText(this, "Choose a future time", Toast.LENGTH_SHORT).show();
                        // reopen time picker
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
