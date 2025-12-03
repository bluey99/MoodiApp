package com.example.asdproject.view.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.asdproject.R;

/**
 * Displays the child's home screen after login.
 * Provides navigation to emotion logging, history, tasks, and calming tools.
 */
public class ChildHomeActivity extends AppCompatActivity {

    private TextView txtGreeting;
    private LinearLayout btnLogEmotion;
    private LinearLayout btnTasks;
    private LinearLayout btnHistory;
    private LinearLayout btnCalmingTools;

    // Firestore document ID representing the logged-in child
    private String childId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_child_home);

        // Retrieve child name and ID passed from LoginActivity
        String childName = getIntent().getStringExtra("childName");
        childId = getIntent().getStringExtra("childId");

        // Connect UI elements
        txtGreeting = findViewById(R.id.txtGreeting);
        btnLogEmotion = findViewById(R.id.btnLogEmotion);
        btnTasks = findViewById(R.id.btnTasks);
        btnHistory = findViewById(R.id.btnHistory);
        btnCalmingTools = findViewById(R.id.btnCalmingTools);

        // Display a simple greeting message for the child
        txtGreeting.setText("Good morning, " + childName + "!");

        // Navigate to the emotion logging screen
        btnLogEmotion.setOnClickListener(v -> {
            Intent intent = new Intent(this, EmotionLogActivity.class);
            intent.putExtra("childId", childId);
            startActivity(intent);
        });

        // Navigate to the emotion history screen
        btnHistory.setOnClickListener(v -> {
            Intent intent = new Intent(this, ChildHistoryActivity.class);
            intent.putExtra("childId", childId);
            startActivity(intent);
        });

        // Navigation for tasks and calming tools will be implemented later
    }
}
