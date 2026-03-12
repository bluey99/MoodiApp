package com.example.asdproject.view.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.asdproject.R;
import com.example.asdproject.util.SimpleAnimatorListener;

public class BreathingExerciseActivity extends AppCompatActivity {

    private View circle;
    private View glow;
    private TextView txtInstruction;

    private AnimatorSet breathingLoop;
    private boolean shouldLoop = true;
    private TextView txtBreathNote;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_breathing_exercise);


        View header = findViewById(R.id.header);
        // hide filter icon on breathing screen
        View filter = header.findViewById(R.id.btnFilter);
        if (filter != null) {
            filter.setVisibility(View.GONE);
        }

        header.findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        TextView title = header.findViewById(R.id.txtHeaderTitle);
        title.setText("Breathing Exercise");
        txtBreathNote = findViewById(R.id.txtBreathNote);
        // fade companion note in once on screen open
        txtBreathNote.postDelayed(() ->
                        txtBreathNote.animate()
                                .alpha(1f)
                                .setDuration(600)
                                .start(),
                400
        );

        circle = findViewById(R.id.breathCircle);
        glow = findViewById(R.id.breathGlow);
        txtInstruction = findViewById(R.id.txtBreathInstruction);
    }

    @Override
    protected void onResume() {
        super.onResume();
        shouldLoop = true;
        startBreathingAnimation();
    }

    @Override
    protected void onPause() {
        super.onPause();
        shouldLoop = false;
        stopBreathingAnimation();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        shouldLoop = false;
        stopBreathingAnimation();
    }

    private void stopBreathingAnimation() {
        if (breathingLoop != null) {
            breathingLoop.cancel();
            breathingLoop = null;
        }
    }

    private void startBreathingAnimation() {
        stopBreathingAnimation();
        breathingLoop = createBreathingCycle();
        breathingLoop.start();
    }

    // recreate a fresh breathing cycle each time for safe looping
    private AnimatorSet createBreathingCycle() {

        long inhaleDuration = 4000;
        long holdDuration = 2000;
        long exhaleDuration = 4000;

        AccelerateDecelerateInterpolator interpolator =
                new AccelerateDecelerateInterpolator();

        // ---------- INHALE ----------
        ObjectAnimator inhaleX =
                ObjectAnimator.ofFloat(circle, View.SCALE_X, 1f, 1.4f);
        ObjectAnimator inhaleY =
                ObjectAnimator.ofFloat(circle, View.SCALE_Y, 1f, 1.4f);
        ObjectAnimator glowIn =
                ObjectAnimator.ofFloat(glow, View.ALPHA, 0.3f, 0.6f);

        inhaleX.setDuration(inhaleDuration);
        inhaleY.setDuration(inhaleDuration);
        glowIn.setDuration(inhaleDuration);

        inhaleX.setInterpolator(interpolator);
        inhaleY.setInterpolator(interpolator);
        glowIn.setInterpolator(interpolator);

        inhaleX.addListener(new SimpleAnimatorListener(() ->
                updateInstructionText("breathe in")
        ));


        AnimatorSet inhale = new AnimatorSet();
        inhale.playTogether(inhaleX, inhaleY, glowIn);

        // ---------- HOLD ----------
        ValueAnimator holdIn = ValueAnimator.ofFloat(0f, 1f);
        holdIn.setDuration(holdDuration);
        holdIn.addListener(new SimpleAnimatorListener(() ->
                updateInstructionText("hold")
        ));


        // ---------- EXHALE ----------
        ObjectAnimator exhaleX =
                ObjectAnimator.ofFloat(circle, View.SCALE_X, 1.4f, 1f);
        ObjectAnimator exhaleY =
                ObjectAnimator.ofFloat(circle, View.SCALE_Y, 1.4f, 1f);
        ObjectAnimator glowOut =
                ObjectAnimator.ofFloat(glow, View.ALPHA, 0.6f, 0.3f);

        exhaleX.setDuration(exhaleDuration);
        exhaleY.setDuration(exhaleDuration);
        glowOut.setDuration(exhaleDuration);

        exhaleX.setInterpolator(interpolator);
        exhaleY.setInterpolator(interpolator);
        glowOut.setInterpolator(interpolator);

        exhaleX.addListener(new SimpleAnimatorListener(() ->
                updateInstructionText("breathe out")
        ));


        AnimatorSet exhale = new AnimatorSet();
        exhale.playTogether(exhaleX, exhaleY, glowOut);

        // ---------- HOLD ----------
        ValueAnimator holdOut = ValueAnimator.ofFloat(0f, 1f);
        holdOut.setDuration(holdDuration);

        // ---------- FULL CYCLE ----------
        AnimatorSet cycle = new AnimatorSet();
        cycle.playSequentially(inhale, holdIn, exhale, holdOut);

        //  loop safely by creating a NEW cycle
        cycle.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (shouldLoop) {
                    breathingLoop = createBreathingCycle();
                    breathingLoop.start();
                }
            }
        });

        return cycle;
    }

    //  gently fade instruction text instead of snapping
    private void updateInstructionText(String newText) {
        txtInstruction.animate()
                .alpha(0f)
                .setDuration(150)
                .withEndAction(() -> {
                    txtInstruction.setText(newText);
                    txtInstruction.animate()
                            .alpha(1f)
                            .setDuration(150)
                            .start();
                })
                .start();
    }

}
