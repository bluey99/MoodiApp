package com.example.asdproject.view.activities;



import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.asdproject.R;

public class TherapistNotesActivity extends AppCompatActivity {

    private String childId;
    private String childName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_therapist_notes);

        // child context from parent home
        childId = getIntent().getStringExtra("CHILD_ID");
        childName = getIntent().getStringExtra("CHILD_NAME");

        if (childName != null && !childName.isEmpty()) {
            setTitle("Therapist Notes – " + childName);
        }

        Button btnBack = findViewById(R.id.btnBackFromNotes);
        Button btnFilter = findViewById(R.id.btnFilterNotes);

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btnFilter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg;
                if (childName != null && !childName.isEmpty()) {
                    msg = "Filter options for " + childName + " will be added later.";
                } else {
                    msg = "Filter options will be added later.";
                }
                Toast.makeText(TherapistNotesActivity.this,
                        msg,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
}