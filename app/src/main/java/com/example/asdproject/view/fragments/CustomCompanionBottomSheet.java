package com.example.asdproject.view.fragments;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;

import com.example.asdproject.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

/**
 * Bottom sheet for entering who was with the child.
 * This is a temporary input UI and does not count as a flow step.
 */
public class CustomCompanionBottomSheet extends BottomSheetDialogFragment {

    public interface Listener {
        void onCustomCompanionEntered(String companion);
    }

    private static final int MAX_LENGTH = 25;
    private Listener listener;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof Listener) {
            listener = (Listener) context;
        } else {
            throw new IllegalStateException(
                    "Activity must implement CustomCompanionBottomSheet.Listener"
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
        View view = inflater.inflate(
                R.layout.fragment_custom_companion,
                container,
                false
        );

        EditText input = view.findViewById(R.id.editCustomCompanion);
        TextView counter = view.findViewById(R.id.txtCharCounterCompanion);
        AppCompatButton btnContinue = view.findViewById(R.id.btnConfirmCustomCompanion);
        AppCompatButton btnCancel = view.findViewById(R.id.btnBackToWhoWith);

        btnContinue.setEnabled(false);
        btnContinue.setAlpha(0.4f);
        counter.setText(getString(R.string.custom_companion_counter, 0, MAX_LENGTH));

        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}

            @Override
            public void afterTextChanged(Editable s) {
                int length = s.length();
                counter.setText(getString(R.string.custom_companion_counter, length, MAX_LENGTH));

                boolean valid = !TextUtils.isEmpty(s.toString().trim());
                btnContinue.setEnabled(valid);
                btnContinue.setAlpha(valid ? 1f : 0.4f);
            }
        });

        btnContinue.setOnClickListener(v -> {
            String text = input.getText().toString().trim();
            if (!TextUtils.isEmpty(text)) {
                listener.onCustomCompanionEntered(text);
                dismiss();
            }
        });

        btnCancel.setOnClickListener(v -> dismiss());

        return view;
    }
}