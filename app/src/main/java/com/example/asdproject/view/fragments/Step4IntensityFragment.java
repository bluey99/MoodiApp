package com.example.asdproject.view.fragments;

import android.animation.ValueAnimator;
import android.content.Context;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;

import com.example.asdproject.R;

/**
 * Step 4 of the child logging flow.
 * Allows the child to select intensity (1–5) by tapping a vertical “glass”.
 * The fill height smoothly animates, and labels/colors change per level.
 */
public class Step4IntensityFragment extends Fragment {

    /** Callback to the hosting Activity. */
    public interface Listener {
        void onIntensitySelected(int intensityLevel);
    }

    private Listener listener;

    // UI elements
    private View fillContainer;
    private View fillView;
    private TextView txtLabel;
    private AppCompatButton btnContinue;

    // Interaction state
    private int currentLevel = 1;
    private boolean userHasChosen = false;

    // Computed heights
    private int maxFillHeightPx = 0;
    private int minFillHeightPx = 0;

    private static final int LEVELS = 5;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        if (context instanceof Listener) {
            listener = (Listener) context;
        } else {
            throw new IllegalStateException("Parent must implement Step4IntensityFragment.Listener");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_step4_intensity, container, false);

        // Link UI
        fillContainer = view.findViewById(R.id.fillContainer);
        fillView = view.findViewById(R.id.fillView);
        txtLabel = view.findViewById(R.id.txtIntensityLabel);
        btnContinue = view.findViewById(R.id.btnIntensityContinue);

        // Initially disabled until the child taps at least once
        disableContinueButton();

        // Compute fill height after layout pass
        fillContainer.post(() -> {
            maxFillHeightPx = fillContainer.getHeight();
            minFillHeightPx = dpToPx(30); // Minimum visible liquid
            applyFillHeight(currentLevel, false);
        });

        // Tap interaction
        setupTapListener();

        // Continue button
        btnContinue.setOnClickListener(v -> {
            if (!userHasChosen) return;
            if (listener != null) listener.onIntensitySelected(currentLevel);
        });

        return view;
    }

    /**
     * Enables Continue button visually and functionally.
     */
    private void enableContinueButton() {
        btnContinue.setEnabled(true);
        btnContinue.setAlpha(1f);
    }

    /**
     * Disables Continue button until a level is chosen.
     */
    private void disableContinueButton() {
        btnContinue.setEnabled(false);
        btnContinue.setAlpha(0.5f);
    }

    /**
     * Registers tap behavior on the fill container.
     * Computes intensity (1–5) based on vertical tap position.
     */
    private void setupTapListener() {
        fillContainer.setOnTouchListener((v, event) -> {

            if (event.getAction() == MotionEvent.ACTION_DOWN ||
                    event.getAction() == MotionEvent.ACTION_UP) {

                float containerHeight = v.getHeight();
                float tapY = event.getY();     // Distance from top
                float normalized = 1f - (tapY / containerHeight); // 0 bottom → 1 top

                int level = Math.round(normalized * LEVELS);
                level = Math.max(1, Math.min(LEVELS, level));  // Clamp 1–5

                updateIntensity(level, true);
                return true;
            }
            return false;
        });
    }

    /**
     * Updates the intensity level (label, color, animation).
     * @param level   Chosen intensity (1–5)
     * @param animate Whether to animate the fill height
     */
    private void updateIntensity(int level, boolean animate) {
        userHasChosen = true;
        enableContinueButton();

        int previous = currentLevel;
        currentLevel = level;

        updateLabelAndColor(level);

        // If layout not measured yet, apply after measurement
        if (maxFillHeightPx == 0) {
            fillContainer.post(() -> applyFillHeight(level, false));
            return;
        }

        applyFillHeightAnimated(previous, level, animate);
    }

    /**
     * Applies fill height immediately without animation.
     */
    private void applyFillHeight(int level, boolean animate) {
        applyFillHeightAnimated(level, level, animate);
    }

    /**
     * Computes and animates the fillView height between two levels.
     */
    private void applyFillHeightAnimated(int fromLevel, int toLevel, boolean animate) {

        float fromRatio = (float) fromLevel / LEVELS;
        float toRatio = (float) toLevel / LEVELS;

        int fromHeight = (int) (minFillHeightPx + (maxFillHeightPx - minFillHeightPx) * fromRatio);
        int toHeight = (int) (minFillHeightPx + (maxFillHeightPx - minFillHeightPx) * toRatio);

        if (!animate) {
            setFillHeight(toHeight);
            return;
        }

        ValueAnimator animator = ValueAnimator.ofInt(fromHeight, toHeight);
        animator.setDuration(260);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(a -> setFillHeight((int) a.getAnimatedValue()));
        animator.start();
    }

    /**
     * Updates the fillView height safely.
     */
    private void setFillHeight(int heightPx) {
        ViewGroup.LayoutParams lp = fillView.getLayoutParams();
        lp.height = heightPx;
        fillView.setLayoutParams(lp);
        fillView.requestLayout();
    }

    /**
     * Updates the label text + liquid color drawable.
     */
    private void updateLabelAndColor(int level) {
        switch (level) {
            case 1:
                txtLabel.setText("Just a little");
                fillView.setBackgroundResource(R.drawable.intensity_fill_level1);
                break;
            case 2:
                txtLabel.setText("A little bit");
                fillView.setBackgroundResource(R.drawable.intensity_fill_level2);
                break;
            case 3:
                txtLabel.setText("Medium");
                fillView.setBackgroundResource(R.drawable.intensity_fill_level3);
                break;
            case 4:
                txtLabel.setText("A lot");
                fillView.setBackgroundResource(R.drawable.intensity_fill_level4);
                break;
            case 5:
                txtLabel.setText("Very much");
                fillView.setBackgroundResource(R.drawable.intensity_fill_level5);
                break;
        }
    }

    /**
     * Converts dp → px.
     */
    private int dpToPx(int dp) {
        if (getContext() == null) return dp;
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                getResources().getDisplayMetrics()
        );
    }
}
