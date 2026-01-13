package com.example.asdproject.view.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.asdproject.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class ChildTaskFilterBottomSheetFragment extends BottomSheetDialogFragment {

    public interface OnFilterSelectedListener {
        void onFilterSelected(String creatorType);
    }

    private OnFilterSelectedListener listener;

    public ChildTaskFilterBottomSheetFragment(OnFilterSelectedListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.sheet_task_filter, container, false);

        Button btnAll = view.findViewById(R.id.btnAll);
        ImageView btnMom = view.findViewById(R.id.btnMom);
        ImageView btnTherapist = view.findViewById(R.id.btnTherapist);


        btnAll.setOnClickListener(v -> {
            listener.onFilterSelected(null);
            dismiss();
        });

        btnMom.setOnClickListener(v -> {
            listener.onFilterSelected("PARENT");
            dismiss();
        });

        btnTherapist.setOnClickListener(v -> {
            listener.onFilterSelected("THERAPIST");
            dismiss();
        });

        return view;
    }
}
