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
 * Step 2 of the child emotion-logging flow.
 * The child selects where the event occurred.
 * Uses ChildButtonHelper to unify animation + selection behavior.
 */
public class Step2WhereFragment extends Fragment {

    /** Callback interface implemented by the hosting Activity. */
    public interface Listener {
        void onLocationSelected(String location);
    }

    private Listener listener;
    private View[] allButtons;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        if (context instanceof Listener) {
            listener = (Listener) context;
        } else {
            throw new IllegalStateException("Parent must implement Step2WhereFragment.Listener");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_step2_where, container, false);

        // Collect all button references
        View btnSchool = view.findViewById(R.id.btnWhere1);
        View btnHome = view.findViewById(R.id.btnWhere2);
        View btnUnknown = view.findViewById(R.id.btnWhere3);
        View btnElse = view.findViewById(R.id.btnWhere4);

        allButtons = new View[]{ btnSchool, btnHome, btnUnknown, btnElse };

        // Attach click behavior using shared helper
        setupButtons(btnSchool, btnHome, btnUnknown, btnElse);

        return view;
    }

    /**
     * Registers click handlers for each location option.
     * Uses ChildButtonHelper to ensure consistent animation + selection.
     */
    private void setupButtons(View btnSchool, View btnHome, View btnUnknown, View btnElse) {

        ChildButtonHelper.setup(
                btnSchool, allButtons,
                () -> listener.onLocationSelected("At School")
        );

        ChildButtonHelper.setup(
                btnHome, allButtons,
                () -> listener.onLocationSelected("At Home")
        );

        ChildButtonHelper.setup(
                btnUnknown, allButtons,
                () -> listener.onLocationSelected("I don't know")
        );

        ChildButtonHelper.setup(
                btnElse, allButtons,
                () -> listener.onLocationSelected("Somewhere else")
        );
    }
}
