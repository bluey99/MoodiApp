package com.example.asdproject.view.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.asdproject.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

/**
 * Bottom sheet for filtering emotion history.
 * Allows filtering by emotion, intensity, and time range.
 * Returns selections to the caller via Listener.
 */
public class HistoryFilterBottomSheetFragment extends BottomSheetDialogFragment {

    public interface Listener {
        void onFiltersApplied(
                String emotion,
                int minIntensity,
                int maxIntensity,
                String timeFilter
        );
    }

    private Listener listener;

    private String selectedEmotion = null;

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_history_filter, container, false);

        ChipGroup chipGroupEmotions = view.findViewById(R.id.chipGroupEmotions);
        for (int i = 0; i < chipGroupEmotions.getChildCount(); i++) {
            View chip = chipGroupEmotions.getChildAt(i);

            chip.setOnClickListener(v -> {
                com.example.asdproject.util.ViewAnimationUtil
                        .playFeelingClick(requireContext(), v);
            });
        }


        Button btnApply = view.findViewById(R.id.btnApplyFilters);
        Button btnClear = view.findViewById(R.id.btnClearFilters);

        chipGroupEmotions.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds == null || checkedIds.isEmpty()) {
                selectedEmotion = null;
                return;
            }

            int checkedId = checkedIds.get(0);
            Chip chip = group.findViewById(checkedId);

            if (chip == null) {
                selectedEmotion = null;
                return;
            }

            Object tag = chip.getTag();
            if (tag == null) {
                selectedEmotion = null;
                return;
            }

            selectedEmotion = tag.toString().trim().toUpperCase();
        });

        btnApply.setOnClickListener(v -> {
            if (listener != null) {
                listener.onFiltersApplied(
                        selectedEmotion,
                        -1,
                        -1,
                        "ALL"
                );
            }
            dismiss();
        });

        btnClear.setOnClickListener(v -> {
            chipGroupEmotions.clearCheck();
            selectedEmotion = null;

            if (listener != null) {
                listener.onFiltersApplied(
                        null,
                        -1,
                        -1,
                        "ALL"
                );
            }
            dismiss();
        });

        return view;
    }
}
