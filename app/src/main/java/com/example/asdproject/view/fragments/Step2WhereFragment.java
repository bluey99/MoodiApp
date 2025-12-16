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
 * Step 2 of the emotion logging flow.
 * The child selects where the event happened.
 */
public class Step2WhereFragment extends Fragment {

    /** Callbacks handled by EmotionLogActivity */
    public interface Listener {
        void onLocationSelected(String location);
        void onRequestCustomLocation();
    }

    private Listener listener;
    private View[] allButtons;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof Listener) {
            listener = (Listener) context;
        } else {
            throw new IllegalStateException(
                    "Parent must implement Step2WhereFragment.Listener"
            );
        }
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        View view = inflater.inflate(R.layout.fragment_step2_where, container, false);

        View btnSchool = view.findViewById(R.id.btnWhere1);
        View btnHome   = view.findViewById(R.id.btnWhere2);
        View btnOther  = view.findViewById(R.id.btnWhere3);
        View btnCustom = view.findViewById(R.id.btnWhere4);

        allButtons = new View[]{ btnSchool, btnHome, btnOther, btnCustom };

        setupButtons(btnSchool, btnHome, btnOther, btnCustom);

        return view;
    }

    /** Registers all location buttons using the shared helper */
    private void setupButtons(
            View btnSchool,
            View btnHome,
            View btnOther,
            View btnCustom
    ) {
        ChildButtonHelper.setup(
                btnSchool, allButtons,
                () -> listener.onLocationSelected("At school")
        );

        ChildButtonHelper.setup(
                btnHome, allButtons,
                () -> listener.onLocationSelected("At home")
        );

        ChildButtonHelper.setup(
                btnOther, allButtons,
                () -> listener.onLocationSelected("Somewhere else")
        );

        ChildButtonHelper.setup(
                btnCustom, allButtons,
                () -> listener.onRequestCustomLocation()
        );
    }
}
