package com.example.asdproject.view.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.asdproject.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

/**
 * HistoryFilterBottomSheetFragment
 *
 * Bottom sheet dialog used to filter the child's emotion history.
 *
 * Supported filters:
 * - Emotion (single selection via chips)
 * - Intensity (single exact value, 1–5, via glass selector)
 *
 * Design decisions:
 * - Intensity is a single value (NOT a range)
 * - Intensity = -1 means "Any intensity"
 * - This fragment ONLY handles UI + user input
 * - Actual filtering logic is handled in ChildHistoryActivity
 */
public class HistoryFilterBottomSheetFragment extends BottomSheetDialogFragment {

    /**
     * Listener implemented by the hosting Activity.
     * Used to pass the selected filter values back.
     */
    public interface Listener {
        void onFiltersApplied(
                String emotion,
                int intensity,
                String timeFilter
        );
    }

    /* ===================== STATE ===================== */

    private Listener listener;

    /** Selected emotion (e.g., "HAPPY"), or null for any */
    private String selectedEmotion = null;

    /**
     * Selected intensity:
     *  1–5  → exact intensity filter
     * -1    → any intensity
     */
    private int selectedIntensity = -1;

    //Selected time filter
    private String selectedTime = "ALL";


    public void setListener(Listener listener) {
        this.listener = listener;
    }

    /* ===================== LIFECYCLE ===================== */

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_history_filter, container, false);

        /* ===================== EMOTION CHIPS ===================== */

        ChipGroup chipGroupEmotions = view.findViewById(R.id.chipGroupEmotions);

        // Play feedback animation on chip tap
        for (int i = 0; i < chipGroupEmotions.getChildCount(); i++) {
            View chip = chipGroupEmotions.getChildAt(i);
            chip.setOnClickListener(v ->
                    com.example.asdproject.util.ViewAnimationUtil
                            .playFeelingClick(requireContext(), v)
            );
        }

        // Handle emotion selection
        chipGroupEmotions.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds == null || checkedIds.isEmpty()) {
                selectedEmotion = null;
                return;
            }

            Chip chip = group.findViewById(checkedIds.get(0));
            if (chip == null || chip.getTag() == null) {
                selectedEmotion = null;
                return;
            }

            selectedEmotion = chip.getTag().toString().trim().toUpperCase();
        });
        /* ===================== TIME FILTER ===================== */

        ChipGroup chipGroupTime = view.findViewById(R.id.chipGroupTime);
        selectedTime = "ALL";

        if (chipGroupTime != null) {
            chipGroupTime.setOnCheckedStateChangeListener((group, checkedIds) -> {
                if (checkedIds == null || checkedIds.isEmpty()) {
                    selectedTime = "ALL";
                    return;
                }

                Chip chip = group.findViewById(checkedIds.get(0));
                selectedTime = (chip != null && chip.getTag() != null)
                        ? chip.getTag().toString()
                        : "ALL";
            });
        }


        /* ===================== INTENSITY (GLASS SELECTOR) ===================== */

        View fillView = view.findViewById(R.id.fillView);
        View fillContainer = view.findViewById(R.id.fillContainer);
        TextView txtIntensityLabel = view.findViewById(R.id.txtIntensityLabel);

        // Default state: no intensity filter
        selectedIntensity = -1;
        txtIntensityLabel.setText("Any intensity");

        // Each tap cycles intensity: Any → 1 → 2 → 3 → 4 → 5 → 1 ...
        fillContainer.setOnClickListener(v -> {

            if (selectedIntensity == -1) {
                selectedIntensity = 1;
            } else {
                selectedIntensity++;
                if (selectedIntensity > 5) {
                    selectedIntensity = 1;
                }
            }

            updateGlassUI(selectedIntensity, fillView, txtIntensityLabel);
        });

        /* ===================== ACTION BUTTONS ===================== */

        Button btnApply = view.findViewById(R.id.btnApplyFilters);
        Button btnClear = view.findViewById(R.id.btnClearFilters);

        // Apply filters and notify activity
        btnApply.setOnClickListener(v -> {
            if (listener != null) {
                listener.onFiltersApplied(
                        selectedEmotion,
                        selectedIntensity,
                        selectedTime
                );

            }
            dismiss();
        });

        // Clear all filters and reset state
        btnClear.setOnClickListener(v -> {
            chipGroupEmotions.clearCheck();
            chipGroupTime.clearCheck();

            selectedEmotion = null;
            selectedIntensity = -1;
            selectedTime = "ALL";

            txtIntensityLabel.setText("Any intensity");

            // 🔹 RESET GLASS VISUALLY (IMPORTANT)
            ViewGroup.LayoutParams params = fillView.getLayoutParams();
            params.height = (int) (
                    40 * fillView.getResources().getDisplayMetrics().density
            );
            fillView.setLayoutParams(params);
            fillView.setBackgroundResource(R.drawable.intensity_fill_level1);

            if (listener != null) {
                listener.onFiltersApplied(
                        null,
                        -1,
                        "ALL"
                );
            }
            dismiss();
        });


        return view;
    }

    /* ===================== UI HELPERS ===================== */

    /**
     * Updates the glass UI to visually represent the selected intensity.
     *
     * @param level     intensity level (1–5)
     * @param fillView  the colored fill inside the glass
     * @param label     descriptive text shown below the glass
     */
    private void updateGlassUI(
            int level,
            View fillView,
            TextView label
    ) {
        int heightDp;
        int backgroundRes;
        String text;

        switch (level) {
            case 1:
                heightDp = 40;
                backgroundRes = R.drawable.intensity_fill_level1;
                text = "Just a little";
                break;
            case 2:
                heightDp = 80;
                backgroundRes = R.drawable.intensity_fill_level2;
                text = "A little bit";
                break;
            case 3:
                heightDp = 120;
                backgroundRes = R.drawable.intensity_fill_level3;
                text = "Medium";
                break;
            case 4:
                heightDp = 160;
                backgroundRes = R.drawable.intensity_fill_level4;
                text = "A lot";
                break;
            default:
                heightDp = 200;
                backgroundRes = R.drawable.intensity_fill_level5;
                text = "Very much";
                break;
        }

        // Convert dp → px and update fill height
        ViewGroup.LayoutParams params = fillView.getLayoutParams();
        params.height = (int) (
                heightDp * fillView.getResources().getDisplayMetrics().density
        );
        fillView.setLayoutParams(params);

        fillView.setBackgroundResource(backgroundRes);
        label.setText(text);
    }
}
