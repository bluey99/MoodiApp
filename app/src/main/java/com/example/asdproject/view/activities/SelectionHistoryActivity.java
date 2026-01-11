package com.example.asdproject.view.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.asdproject.R;

public class SelectionHistoryActivity extends AppCompatActivity {

    private String childId;
    private String childName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selection_history);

        // Get selected child info from ParentHomeActivity
        childId = getIntent().getStringExtra("CHILD_ID");
        childName = getIntent().getStringExtra("CHILD_NAME");

        // Buttons from XML
        androidx.appcompat.widget.AppCompatButton btnChildHistory = findViewById(R.id.btnChildHistory);
        androidx.appcompat.widget.AppCompatButton btnTaskHistory  = findViewById(R.id.btnTaskHistory);
        Button btnGoBack = findViewById(R.id.btnGoBack);

        // 1) Child logs history -> open ChildLogsHistoryActivity
        btnChildHistory.setOnClickListener(v -> {
            Intent i = new Intent(SelectionHistoryActivity.this, ChildLogsHistoryActivity.class);
            putChildExtras(i);
            startActivity(i);
        });

        // 2) Tasks & Therapist logs -> open existing EmotionHistoryActivity
        btnTaskHistory.setOnClickListener(v -> {
            Intent i = new Intent(SelectionHistoryActivity.this, EmotionHistoryActivity.class);
            putChildExtras(i);
            startActivity(i);
        });

        // Back button
        btnGoBack.setOnClickListener(v -> finish());
    }

    private void putChildExtras(Intent i) {
        if (childId != null && !childId.trim().isEmpty()) {
            i.putExtra("CHILD_ID", childId);
            i.putExtra("CHILD_NAME", childName);
        }
    }
}