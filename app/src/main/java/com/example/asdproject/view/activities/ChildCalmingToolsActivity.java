package com.example.asdproject.view.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.asdproject.R;

/**
 * CalmingToolsActivity
 *
 * Entry screen for child calming tools.
 * Provides gentle, pressure-free options to help the child regulate.
 *
 * This screen ONLY handles navigation.
 * No logging, no Firebase, no persistence.
 */
public class ChildCalmingToolsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calming_tools);

        View header = findViewById(R.id.header);
        // Hide filter button – not relevant for calming tools
        ImageView btnFilter = header.findViewById(R.id.btnFilter);
        if (btnFilter != null) {
            btnFilter.setVisibility(View.GONE);
        }
        TextView title = header.findViewById(R.id.txtHeaderTitle);
        title.setText("My tools");

        header.findViewById(R.id.btnBack).setOnClickListener(v -> finish());


        // Back button
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Tool cards (navigation only for now)
        findViewById(R.id.cardBreathing).setOnClickListener(v -> {
            // TODO: open BreathingExerciseActivity
        });

        findViewById(R.id.cardMusic).setOnClickListener(v -> {
            // TODO: open MusicActivity
        });

        findViewById(R.id.cardAskForHelp).setOnClickListener(v -> {
            // TODO: open AskForHelpActivity
        });
    }
}
