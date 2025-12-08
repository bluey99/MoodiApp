package com.example.asdproject.util;

import android.view.View;
import android.view.animation.AnimationUtils;

import com.example.asdproject.R;

/**
 * Shared button behavior for all child-step screens.
 * Applies animation, deselects other buttons, selects the clicked one,
 * and executes a callback provided by the fragment.
 */
public class ChildButtonHelper {

    /**
     * Applies consistent behavior to selection buttons across all steps.
     *
     * @param button     The button the child clicks
     * @param allButtons Array of all buttons in the fragment
     * @param callback   The action to run after selecting
     */
    public static void setup(View button, View[] allButtons, Runnable callback) {

        button.setOnClickListener(v -> {

            // 1. Play click animation
            v.startAnimation(AnimationUtils.loadAnimation(v.getContext(), R.anim.feeling_click));

            // 2. Remove "selected" state from all other buttons
            for (View b : allButtons) {
                b.setSelected(false);
            }

            // 3. Highlight the selected button
            v.setSelected(true);

            // 4. Execute callback (send value to activity)
            callback.run();
        });
    }
}
