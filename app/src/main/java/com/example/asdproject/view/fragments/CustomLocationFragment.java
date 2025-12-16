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
import androidx.fragment.app.Fragment;

import com.example.asdproject.R;

/**
 * Custom location input for Step 2.
 * Limited, guided input with back navigation.
 */
public class CustomLocationFragment extends Fragment {

    public interface Listener {
        void onCustomLocationEntered(String location);
        void onBackToWhereStep();
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
                    "Parent must implement CustomLocationFragment.Listener"
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
        View view = inflater.inflate(R.layout.fragment_custom_location, container, false);

        EditText input = view.findViewById(R.id.editCustomLocation);
        TextView counter = view.findViewById(R.id.txtCharCounter);
        AppCompatButton btnContinue = view.findViewById(R.id.btnConfirmCustomLocation);
        View btnBack = view.findViewById(R.id.btnBackToWhere);

        btnContinue.setEnabled(false);
        btnContinue.setAlpha(0.4f);

        counter.setText("0 / " + MAX_LENGTH);

        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}

            @Override
            public void afterTextChanged(Editable s) {
                int length = s.length();
                counter.setText(length + " / " + MAX_LENGTH);

                boolean valid = !TextUtils.isEmpty(s.toString().trim());
                btnContinue.setEnabled(valid);
                btnContinue.setAlpha(valid ? 1f : 0.4f);
            }
        });

        btnContinue.setOnClickListener(v -> {
            String text = input.getText().toString().trim();
            if (!TextUtils.isEmpty(text)) {
                listener.onCustomLocationEntered(text);
            }
        });

        btnBack.setOnClickListener(v -> listener.onBackToWhereStep());

        return view;
    }
}
