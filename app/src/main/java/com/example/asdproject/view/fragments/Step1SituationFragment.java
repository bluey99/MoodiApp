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
import com.example.asdproject.util.ChildButtonHelper;

/**
 * Step 1 of the child emotion-logging flow.
 * The child selects a situation describing what happened before the feeling.
 * This fragment uses ChildButtonHelper to unify click animation + selection behavior.
 */
public class Step1SituationFragment extends Fragment {

    /** Callback interface implemented by the hosting Activity. */
    public interface Listener {
        void onSituationSelected(String situation);
    }

    private Listener listener;
    private View[] allButtons;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        if (context instanceof Listener) {
            listener = (Listener) context;
        } else {
            throw new IllegalStateException("Parent must implement Step1SituationFragment.Listener");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_step1_situation, container, false);

        // Collect all button references
        View btn1 = view.findViewById(R.id.btnSituation1);
        View btn2 = view.findViewById(R.id.btnSituation2);
        View btn3 = view.findViewById(R.id.btnSituation3);
        View btn4 = view.findViewById(R.id.btnSituation4);
        View btn5 = view.findViewById(R.id.btnSituation5);

        allButtons = new View[]{ btn1, btn2, btn3, btn4, btn5 };

        // Attach click behavior using shared helper
        setupButtons();

        return view;
    }

    /**
     * Registers each button with its corresponding situation text.
     * Uses ChildButtonHelper for unified animation + selected-state handling.
     */
    private void setupButtons() {

        ChildButtonHelper.setup(
                allButtons[0], allButtons,
                () -> listener.onSituationSelected("I went somewhere new")
        );

        ChildButtonHelper.setup(
                allButtons[1], allButtons,
                () -> listener.onSituationSelected("I got a gift")
        );

        ChildButtonHelper.setup(
                allButtons[2], allButtons,
                () -> listener.onSituationSelected("I fought with someone")
        );

        ChildButtonHelper.setup(
                allButtons[3], allButtons,
                () -> listener.onSituationSelected("I had a test")
        );

        ChildButtonHelper.setup(
                allButtons[4], allButtons,
                () -> listener.onSituationSelected("Something else")
        );
    }
}
