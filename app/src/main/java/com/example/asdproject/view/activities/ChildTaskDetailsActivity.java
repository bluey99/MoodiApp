package com.example.asdproject.view.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.BidiFormatter;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.asdproject.R;

/**
 * Shows a single task for the child.
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
        String taskId = getIntent().getStringExtra("taskId");
        String taskName = getIntent().getStringExtra("taskName");
        String displayWhen = getIntent().getStringExtra("displayWhen");
        String discussionPrompts = getIntent().getStringExtra("discussionPrompts");
        String creatorType = getIntent().getStringExtra("creatorType");
        String childId = getIntent().getStringExtra("childId");

        // --------- HEADER ----------
        View header = findViewById(R.id.header);

        ImageView btnBack = header.findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        TextView headerTitle = header.findViewById(R.id.txtHeaderTitle);
        headerTitle.setText(
                "THERAPIST".equals(creatorType)
                        ? getString(R.string.task_details_header_therapist)
                        : getString(R.string.task_details_header_parent)
        );

        ImageView btnFilter = header.findViewById(R.id.btnFilter);
        if (btnFilter != null) {
            btnFilter.setVisibility(View.GONE);
        }

        // --------- CONTENT VIEWS ----------
        txtTaskName = findViewById(R.id.txtTaskName);
        txtWhen = findViewById(R.id.txtWhen);
        txtPrompt = findViewById(R.id.txtPrompt);
        txtCreatorName = findViewById(R.id.txtMomName);
        ImageView imgCreatorAvatar = findViewById(R.id.imgMomAvatar);
        Button btnLogNow = findViewById(R.id.btnLogNow);
        txtTitle = findViewById(R.id.txtTitle);

        // --------- populate content ----------
        boolean isTherapist =
                creatorType != null &&
                        creatorType.equalsIgnoreCase("THERAPIST");

        if (isTherapist) {
            txtTitle.setText(getString(R.string.task_details_title_therapist));
            txtCreatorName.setText(getString(R.string.task_details_creator_therapist));
            imgCreatorAvatar.setImageResource(R.drawable.ic_therapist);
        } else {
            txtTitle.setText(getString(R.string.task_details_title_parent));
            txtCreatorName.setText(getString(R.string.task_details_creator_parent));
            imgCreatorAvatar.setImageResource(R.drawable.ic_parent);
        }

        if (taskName != null) {
            applyContentDirection(txtTaskName, taskName);
        }

        if (displayWhen != null) {
            String dateOnly = displayWhen.split(",")[0].trim();
            txtWhen.setText(getString(R.string.task_details_when, dateOnly));
        }

        if (discussionPrompts != null) {
            applyContentDirection(txtPrompt, discussionPrompts);
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

    // bayan added here - display task content according to the language it was written in
    private void applyContentDirection(TextView textView, String text) {
        if (text == null) {
            textView.setText("");
            return;
        }

        boolean isRtl = isRtlText(text);

        // force paragraph direction so Arabic behaves RTL and English behaves LTR
        BidiFormatter bidi = BidiFormatter.getInstance(isRtl);
        textView.setText(bidi.unicodeWrap(text));

        if (isRtl) {
            textView.setTextDirection(View.TEXT_DIRECTION_RTL);
            textView.setGravity(Gravity.RIGHT);
            textView.setTextAlignment(View.TEXT_ALIGNMENT_GRAVITY);
        } else {
            textView.setTextDirection(View.TEXT_DIRECTION_LTR);
            textView.setGravity(Gravity.LEFT);
            textView.setTextAlignment(View.TEXT_ALIGNMENT_GRAVITY);
        }
    }

    // bayan added here - detect direction from the first strong character
    private boolean isRtlText(String text) {
        if (text == null) return false;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            byte dir = Character.getDirectionality(c);

            if (dir == Character.DIRECTIONALITY_RIGHT_TO_LEFT ||
                    dir == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC) {
                return true;
            }

            if (dir == Character.DIRECTIONALITY_LEFT_TO_RIGHT) {
                return false;
            }
        }

        return false;
    }
}