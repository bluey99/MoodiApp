package com.example.asdproject.view.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.asdproject.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

/**
 * VisualStimSettingsBottomSheetFragment
 *
 * Allows the child to gently control:
 * - animation speed
 * - circle size
 *
 * Changes are applied live.
 */
public class VisualStimSettingsBottomSheetFragment extends BottomSheetDialogFragment {

    public interface OnStimSettingsChanged {
        void onSpeedChanged(float speed);
        void onSizeChanged(float size);
    }

    private static final String ARG_SPEED = "arg_speed";
    private static final String ARG_SIZE  = "arg_size";

    private OnStimSettingsChanged listener;

    private float initialSpeed = 1f;
    private float initialSize  = 1f;

    // factory with current values
    public static VisualStimSettingsBottomSheetFragment newInstance(
            float speed,
            float size
    ) {
        Bundle args = new Bundle();
        args.putFloat(ARG_SPEED, speed);
        args.putFloat(ARG_SIZE, size);

        VisualStimSettingsBottomSheetFragment fragment =
                new VisualStimSettingsBottomSheetFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnStimSettingsChanged) {
            listener = (OnStimSettingsChanged) context;
        }
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_visual_stim_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        if (getArguments() != null) {
            initialSpeed = getArguments().getFloat(ARG_SPEED, 1f);
            initialSize  = getArguments().getFloat(ARG_SIZE, 1f);
        }

        SeekBar sliderSpeed = view.findViewById(R.id.sliderSpeed);
        SeekBar sliderSize  = view.findViewById(R.id.sliderSize);

        TextView txtSpeedLabel = view.findViewById(R.id.txtSpeedLabel);
        TextView txtSizeLabel  = view.findViewById(R.id.txtSizeLabel);

        // initialize slider positions
        sliderSpeed.setProgress(reverseMap(initialSpeed, 0.7f, 1.5f));
        sliderSize.setProgress(reverseMap(initialSize, 0.85f, 1.25f));

        // initialize label text from current values
        updateSpeedLabel(sliderSpeed.getProgress(), txtSpeedLabel);
        updateSizeLabel(sliderSize.getProgress(), txtSizeLabel);

        // SPEED
        sliderSpeed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float speed = map(progress, 0.7f, 1.5f);

                if (listener != null) listener.onSpeedChanged(speed);

                updateSpeedLabel(progress, txtSpeedLabel);
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // SIZE
        sliderSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float size = map(progress, 0.85f, 1.25f);

                if (listener != null) listener.onSizeChanged(size);

                updateSizeLabel(progress, txtSizeLabel);
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void updateSpeedLabel(int progress, TextView label) {
        if (progress < 33) {
            label.setText(getString(R.string.visual_stim_speed_slow));
        } else if (progress < 66) {
            label.setText(getString(R.string.visual_stim_speed_normal));
        } else {
            label.setText(getString(R.string.visual_stim_speed_fast));
        }
    }

    private void updateSizeLabel(int progress, TextView label) {
        if (progress < 33) {
            label.setText(getString(R.string.visual_stim_size_small));
        } else if (progress < 66) {
            label.setText(getString(R.string.visual_stim_size_medium));
        } else {
            label.setText(getString(R.string.visual_stim_size_large));
        }
    }

    private float map(int progress, float min, float max) {
        return min + (progress / 100f) * (max - min);
    }

    // map float back to slider position
    private int reverseMap(float value, float min, float max) {
        return Math.round(((value - min) / (max - min)) * 100f);
    }
}