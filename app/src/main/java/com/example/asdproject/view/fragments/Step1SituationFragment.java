package com.example.asdproject.view.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.asdproject.R;

/**
 * Fragment representing Step 1 of the emotion logging workflow.
 * The user selects a situation describing what happened before the emotion.
 *
 * This fragment communicates user selections back to EmotionLogActivity
 * through the Listener interface. The activity hosting this fragment
 * must implement Step1SituationFragment.Listener.
 */
public class Step1SituationFragment extends Fragment {

    /**
     * Callback interface for delivering the user’s selection
     * to the hosting activity.
     */
    public interface Listener {
        void onSituationSelected(String situation);
    }

    private Listener listener;

    /**
     * Ensures that the hosting activity implements the required listener.
     */
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        if (context instanceof Listener) {
            listener = (Listener) context;
        } else {
            throw new IllegalStateException(
                    "Parent activity must implement Step1SituationFragment.Listener");
        }
    }

    /**
     * Inflates the layout and registers click listeners for all
     * situation selection options.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(
                R.layout.fragment_step1_situation,
                container,
                false
        );

        // Register button interactions
        view.findViewById(R.id.btnSituation1).setOnClickListener(v -> send("I went somewhere new"));
        view.findViewById(R.id.btnSituation2).setOnClickListener(v -> send("I got a gift"));
        view.findViewById(R.id.btnSituation3).setOnClickListener(v -> send("I fought with someone"));
        view.findViewById(R.id.btnSituation4).setOnClickListener(v -> send("I had a test"));
        view.findViewById(R.id.btnSituation5).setOnClickListener(v -> send("Something else"));


        return view;
    }

    /**
     * Sends the selected situation back to the hosting activity.
     */
    private void send(String situation) {
        if (listener != null) {
            listener.onSituationSelected(situation);
        }
    }
}
