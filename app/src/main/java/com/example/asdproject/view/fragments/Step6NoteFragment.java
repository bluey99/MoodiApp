package com.example.asdproject.view.fragments;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.asdproject.R;

public class Step6NoteFragment extends Fragment {

    public interface Listener {
        void onNoteEntered(String note);
    }

    private Listener listener;
    private EditText editNote;
    private Button btnContinue;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        if (context instanceof Listener) {
            listener = (Listener) context;
        } else {
            throw new IllegalStateException("Parent must implement Step6NoteFragment.Listener");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_step6_note, container, false);

        editNote = view.findViewById(R.id.editNote);
        btnContinue = view.findViewById(R.id.btnNoteContinue);

        // Default: Skip (no note)
        btnContinue.setText("Skip");

        editNote.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().trim().isEmpty()) {
                    btnContinue.setText("Skip");
                } else {
                    btnContinue.setText("Continue");
                }
            }
        });

        btnContinue.setOnClickListener(v -> {
            String note = editNote.getText().toString().trim();
            listener.onNoteEntered(note.isEmpty() ? null : note);
        });

        return view;
    }
}
