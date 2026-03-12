package com.example.asdproject.util;

import android.animation.Animator;

public class SimpleAnimatorListener implements Animator.AnimatorListener {

    private final Runnable onStart;

    public SimpleAnimatorListener(Runnable onStart) {
        this.onStart = onStart;
    }

    @Override
    public void onAnimationStart(Animator animation) {
        if (onStart != null) onStart.run();
    }

    @Override public void onAnimationEnd(Animator animation) {}
    @Override public void onAnimationCancel(Animator animation) {}
    @Override public void onAnimationRepeat(Animator animation) {}
}
