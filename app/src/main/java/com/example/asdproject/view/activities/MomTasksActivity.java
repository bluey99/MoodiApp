package com.example.asdproject.view.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.asdproject.R;

/**
 * Shows a single "New Task From Mom" for the child.
 * Gets task data from Intent extras sent by ChildTasksActivity.
 */
public class MomTasksActivity extends AppCompatActivity {

    private TextView txtTaskName, txtWhen, txtPrompt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mom_tasks);

        // --------- find views ----------
        ImageButton btnBack = findViewById(R.id.btnBack);
        txtTaskName = findViewById(R.id.txtTaskName);
        txtWhen     = findViewById(R.id.txtWhen);
        txtPrompt   = findViewById(R.id.txtPrompt);
        Button btnLogNow = findViewById(R.id.btnLogNow);

        // Back arrow → return
        btnBack.setOnClickListener(v -> finish());

        // --------- data from clicked task ----------
        String taskName          = getIntent().getStringExtra("taskName");
        String displayWhen       = getIntent().getStringExtra("displayWhen");
        String discussionPrompts = getIntent().getStringExtra("discussionPrompts");

        // ✅ IMPORTANT: pass childId to EmotionLogActivity (needed to save)
        String childId = getIntent().getStringExtra("childId");

        if (taskName != null) {
            txtTaskName.setText(taskName);
        }

        if (displayWhen != null) {
            // Example value: "9/12/2025, 8:00" → show only "9/12/2025"
            String datePart = displayWhen.split(",")[0].trim();
            txtWhen.setText("When: " + datePart);
        }

        if (discussionPrompts != null) {
            txtPrompt.setText(discussionPrompts);
        }

        btnLogNow.setOnClickListener(v -> {
            Intent i = new Intent(MomTasksActivity.this, EmotionLogActivity.class);

            // ✅ Tell the flow: this is task logging
            i.putExtra("LOG_TYPE", "TASK");

            // ✅ Needed for saving
            i.putExtra("childId", childId);

            // ✅ Nice for the review text
            i.putExtra("taskTitle", taskName);

            // ✅ THIS WAS MISSING (this is why Step 6 is empty)
            i.putExtra("discussionPrompts", discussionPrompts);

            startActivity(i);
        });
    }
}
