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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;

import com.example.asdproject.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class CustomSituationBottomSheet extends BottomSheetDialogFragment {

    public interface Listener {
        void onCustomSituationEntered(String situation);
    }

    private Listener listener;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof Listener) {
            listener = (Listener) context;
        } else {
            throw new IllegalStateException(
                    "Activity must implement CustomSituationBottomSheet.Listener"
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
                R.layout.fragment_custom_situation,
                container,
                false
        );

        EditText input = view.findViewById(R.id.editCustomSituation);
        AppCompatButton btnConfirm =
                view.findViewById(R.id.btnConfirmCustomSituation);
        AppCompatButton btnCancel =
                view.findViewById(R.id.btnCancelCustomSituation);


        disableButton(btnConfirm);

        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (TextUtils.isEmpty(s.toString().trim())) {
                    disableButton(btnConfirm);
                } else {
                    enableButton(btnConfirm);
                }
            }
        });

        btnConfirm.setOnClickListener(v -> {
            String text = input.getText().toString().trim();
            if (!TextUtils.isEmpty(text)) {
                listener.onCustomSituationEntered(text);
                dismiss(); // 👈 close the bottom sheet
            }
        });

        btnCancel.setOnClickListener(v -> dismiss());

        return view;
    }

    private void disableButton(AppCompatButton button) {
        button.setEnabled(false);
        button.setAlpha(0.4f);
    }

    private void enableButton(AppCompatButton button) {
        button.setEnabled(true);
        button.setAlpha(1f);
    }
}
