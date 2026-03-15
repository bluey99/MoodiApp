package com.example.asdproject.view.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.asdproject.R;
import com.example.asdproject.util.LocaleHelper;

/**
 * ChildCalmingToolsActivity
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
        LocaleHelper.applyLanguage(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calming_tools);

        View header = findViewById(R.id.header);

        // Hide filter button – not relevant for calming tools
        ImageView btnFilter = header.findViewById(R.id.btnFilter);
        if (btnFilter != null) {
            btnFilter.setVisibility(View.GONE);
        }

        TextView title = header.findViewById(R.id.txtHeaderTitle);
        title.setText(getString(R.string.calming_tools_header_title));

        header.findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Tool cards
        findViewById(R.id.cardBreathing).setOnClickListener(v -> {
            startActivity(
                    new android.content.Intent(
                            this,
                            BreathingExerciseActivity.class
                    )
            );
        });

        findViewById(R.id.cardVisual).setOnClickListener(v -> {
            startActivity(
                    new android.content.Intent(
                            this,
                            VisualCalmActivity.class
                    )
            );
        });

        findViewById(R.id.cardMusic).setOnClickListener(v -> {
            // TODO: open MusicActivity
        });

        findViewById(R.id.cardAskForHelp).setOnClickListener(v -> {
            // TODO: open AskForHelpActivity
        });
    }
}