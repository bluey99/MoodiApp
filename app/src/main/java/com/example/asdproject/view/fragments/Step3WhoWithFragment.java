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
 * Step 3 of the emotion logging flow.
 * The child selects who was with them during the event.
 */
public class Step3WhoWithFragment extends Fragment {

    /** Callbacks handled by EmotionLogActivity */
    public interface Listener {
        void onCompanionSelected(String companion);
        void onRequestCustomCompanion();
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
                    "Parent must implement Step3WhoWithFragment.Listener"
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
        View view = inflater.inflate(R.layout.fragment_step3_who_with, container, false);

        View btnAlone = view.findViewById(R.id.btnWhoWith1);
        View btnFamily = view.findViewById(R.id.btnWhoWith2);
        View btnFriends = view.findViewById(R.id.btnWhoWith3);
        View btnCustom = view.findViewById(R.id.btnWhoWith4);

        allButtons = new View[]{btnAlone, btnFamily, btnFriends, btnCustom};

        setupButtons(btnAlone, btnFamily, btnFriends, btnCustom);

        return view;
    }

    /** Registers all companion buttons using the shared helper */
    private void setupButtons(
            View btnAlone,
            View btnFamily,
            View btnFriends,
            View btnCustom
    ) {
        ChildButtonHelper.setup(
                btnAlone,
                allButtons,
                () -> listener.onCompanionSelected(getString(R.string.step3_who_with_option_1))
        );

        ChildButtonHelper.setup(
                btnFamily,
                allButtons,
                () -> listener.onCompanionSelected(getString(R.string.step3_who_with_option_2))
        );

        ChildButtonHelper.setup(
                btnFriends,
                allButtons,
                () -> listener.onCompanionSelected(getString(R.string.step3_who_with_option_3))
        );

        ChildButtonHelper.setup(
                btnCustom,
                allButtons,
                () -> listener.onRequestCustomCompanion()
        );
    }
}