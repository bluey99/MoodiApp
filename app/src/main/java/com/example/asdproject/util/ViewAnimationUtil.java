package com.example.asdproject.util;

import android.content.Context;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import com.example.asdproject.R;

/**
 * Utility class for lightweight UI animations.
 */
public class ViewAnimationUtil {

    /**
     * Plays the standard "feeling click" animation on a view.
     */
    public static void playFeelingClick(Context context, View view) {
        Animation anim =
                AnimationUtils.loadAnimation(context, R.anim.feeling_click);
        view.startAnimation(anim);
    }
}
