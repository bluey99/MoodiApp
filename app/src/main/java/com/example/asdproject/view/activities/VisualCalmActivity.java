package com.example.asdproject.view.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.asdproject.R;
import com.example.asdproject.util.LocaleHelper;
import com.example.asdproject.view.fragments.VisualStimSettingsBottomSheetFragment;
import com.example.asdproject.view.views.VisualStimmingView;

/**
 * VisualCalmActivity
 *
 * Screen that provides visual stimming for self-regulation.
 * The child can:
 * - Switch between visual modes (Soft / Gentle / Active)
 * - Adjust speed and size via a bottom sheet popup
 *
 * No logging, no persistence, no Firebase.
 */
public class VisualCalmActivity extends AppCompatActivity
        implements VisualStimSettingsBottomSheetFragment.OnStimSettingsChanged {

    private TextView txtVisualNote;
    private VisualStimmingView stimView;

    // bayan added here - persist current settings while screen is open
    private float currentSpeed = 1.0f;
    private float currentSize  = 1.0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LocaleHelper.applyLanguage(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_visual_calm);

        View header = findViewById(R.id.header);

        View filter = header.findViewById(R.id.btnFilter);
        if (filter != null) filter.setVisibility(View.GONE);

        header.findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        TextView title = header.findViewById(R.id.txtHeaderTitle);
        title.setText(getString(R.string.visual_calm_header_title));

        txtVisualNote = findViewById(R.id.txtVisualNote);
        txtVisualNote.postDelayed(() ->
                        txtVisualNote.animate().alpha(1f).setDuration(600).start(),
                400
        );

        stimView = findViewById(R.id.visualStimmingView);

        // apply initial values once
        stimView.setSpeedMultiplier(currentSpeed);
        stimView.setSizeMultiplier(currentSize);

        findViewById(R.id.btnVisualOptions).setOnClickListener(v -> {
            VisualStimSettingsBottomSheetFragment fragment =
                    VisualStimSettingsBottomSheetFragment.newInstance(
                            currentSpeed,
                            currentSize
                    );

            fragment.show(getSupportFragmentManager(), "stimSettings");
        });
    }

    @Override
    public void onSpeedChanged(float speed) {
        currentSpeed = speed;
        stimView.setSpeedMultiplier(speed);
    }

    @Override
    public void onSizeChanged(float size) {
        currentSize = size;
        stimView.setSizeMultiplier(size);
    }
}