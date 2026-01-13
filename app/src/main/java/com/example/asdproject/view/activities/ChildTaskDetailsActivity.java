package com.example.asdproject.view.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.asdproject.R;

/**
 * Shows a single "New Task From Mom" for the child.
 * Gets task data from Intent extras sent by ChildTasksActivity.
 */
public class ChildTaskDetailsActivity extends AppCompatActivity {

    private TextView txtTaskName, txtWhen, txtPrompt;
    private TextView txtTitle, txtCreatorName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mom_tasks);

        // --------- get intent data FIRST ----------
        String taskId            = getIntent().getStringExtra("taskId");
        String taskName          = getIntent().getStringExtra("taskName");
        String displayWhen       = getIntent().getStringExtra("displayWhen");
        String discussionPrompts = getIntent().getStringExtra("discussionPrompts");
        String creatorType       = getIntent().getStringExtra("creatorType");
        String childId           = getIntent().getStringExtra("childId");

        // --------- HEADER ----------
        View header = findViewById(R.id.header);

        ImageView btnBack = header.findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        TextView headerTitle = header.findViewById(R.id.txtHeaderTitle);
        headerTitle.setText(
                "THERAPIST".equals(creatorType)
                        ? "Task from Therapist"
                        : "Task from Mom"
        );

        ImageView btnFilter = header.findViewById(R.id.btnFilter);
        if (btnFilter != null) {
            btnFilter.setVisibility(View.GONE);
        }

        // --------- CONTENT VIEWS ----------
        txtTaskName = findViewById(R.id.txtTaskName);
        txtWhen     = findViewById(R.id.txtWhen);
        txtPrompt   = findViewById(R.id.txtPrompt);
        txtCreatorName = findViewById(R.id.txtMomName);
        ImageView imgCreatorAvatar = findViewById(R.id.imgMomAvatar);
        Button btnLogNow = findViewById(R.id.btnLogNow);
        txtTitle = findViewById(R.id.txtTitle);


        // --------- populate content ----------
        boolean isTherapist =
                creatorType != null &&
                        creatorType.equalsIgnoreCase("THERAPIST");

        if (isTherapist) {
            txtTitle.setText("Task by Therapist");
            txtCreatorName.setText("Therapist");
            imgCreatorAvatar.setImageResource(R.drawable.ic_therapist);
        } else {
            txtTitle.setText("Task by Mom");
            txtCreatorName.setText("Mom");
            imgCreatorAvatar.setImageResource(R.drawable.ic_parent);
        }


        if (taskName != null) {
            txtTaskName.setText(taskName);
        }

        if (displayWhen != null) {
            txtWhen.setText("When: " + displayWhen.split(",")[0].trim());
        }

        if (discussionPrompts != null) {
            txtPrompt.setText(discussionPrompts);
        }

        // --------- Log Now ----------
        btnLogNow.setOnClickListener(v -> {

            Intent i = new Intent(ChildTaskDetailsActivity.this, EmotionLogActivity.class);

            i.putExtra("LOG_TYPE", "TASK");
            i.putExtra("childId", childId);
            i.putExtra("taskId", taskId);
            i.putExtra("taskTitle", taskName);
            i.putExtra("discussionPrompts", discussionPrompts);

            startActivity(i);
            finish();
        });

    }

}
