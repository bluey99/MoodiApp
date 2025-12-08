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
import com.example.asdproject.model.Feeling;
import com.example.asdproject.util.ChildButtonHelper;

/**
 * Step 3 of the child emotion-logging flow.
 * The child selects which feeling they experienced.
 * Uses ChildButtonHelper for unified animation + selected-state visuals.
 */
public class Step3FeelingFragment extends Fragment {

    /** Callback interface implemented by the hosting Activity. */
    public interface Listener {
        void onFeelingSelected(Feeling feeling);
    }

    private Listener listener;
    private View[] allButtons;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        if (context instanceof Listener) {
            listener = (Listener) context;
        } else {
            throw new IllegalStateException("Parent must implement Step3FeelingFragment.Listener");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_step3_feelings, container, false);

        // Collect all feeling buttons
        View btnHappy     = view.findViewById(R.id.btnHappy);
        View btnSad       = view.findViewById(R.id.btnSad);
        View btnAngry     = view.findViewById(R.id.btnAngry);
        View btnSurprised = view.findViewById(R.id.btnSurprised);
        View btnScared    = view.findViewById(R.id.btnScared);
        View btnDisgust   = view.findViewById(R.id.btnDisgust);
        View btnUnsure    = view.findViewById(R.id.btnUnsure);
        View btnOther     = view.findViewById(R.id.btnOtherFeeling);

        allButtons = new View[]{
                btnHappy, btnSad, btnAngry, btnSurprised,
                btnScared, btnDisgust, btnUnsure, btnOther
        };

        // Setup click behavior for each feeling
        setupButtons(btnHappy, btnSad, btnAngry, btnSurprised,
                btnScared, btnDisgust, btnUnsure, btnOther);

        return view;
    }

    /**
     * Registers click handlers for all feeling options.
     * Uses ChildButtonHelper for consistent visual + behavioral handling.
     */
    private void setupButtons(View btnHappy, View btnSad, View btnAngry,
                              View btnSurprised, View btnScared, View btnDisgust,
                              View btnUnsure, View btnOther) {

        ChildButtonHelper.setup(btnHappy, allButtons,
                () -> listener.onFeelingSelected(Feeling.HAPPY));

        ChildButtonHelper.setup(btnSad, allButtons,
                () -> listener.onFeelingSelected(Feeling.SAD));

        ChildButtonHelper.setup(btnAngry, allButtons,
                () -> listener.onFeelingSelected(Feeling.ANGRY));

        ChildButtonHelper.setup(btnSurprised, allButtons,
                () -> listener.onFeelingSelected(Feeling.SURPRISED));

        ChildButtonHelper.setup(btnScared, allButtons,
                () -> listener.onFeelingSelected(Feeling.AFRAID));

        ChildButtonHelper.setup(btnDisgust, allButtons,
                () -> listener.onFeelingSelected(Feeling.DISGUST));

        ChildButtonHelper.setup(btnUnsure, allButtons,
                () -> listener.onFeelingSelected(Feeling.UNSURE));

        ChildButtonHelper.setup(btnOther, allButtons,
                () -> listener.onFeelingSelected(Feeling.OTHER));
    }
}
