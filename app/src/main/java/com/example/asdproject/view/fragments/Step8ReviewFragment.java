package com.example.asdproject.view.fragments;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.asdproject.R;
import com.example.asdproject.model.EmotionLogDraft;

/**
 * Step 7 – Review screen
 * Shows a complete summary plus a visual intensity glass.
 */
public class Step8ReviewFragment extends Fragment {

    public interface Listener {
        void onReviewConfirmed();
    }

    private Listener listener;
    private static final String ARG_DRAFT = "draft";
    private EmotionLogDraft draft;

    public static Step8ReviewFragment newInstance(EmotionLogDraft draft) {
        Step8ReviewFragment f = new Step8ReviewFragment();
        Bundle b = new Bundle();
        b.putSerializable(ARG_DRAFT, draft);
        f.setArguments(b);
        return f;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (!(context instanceof Listener)) {
            throw new IllegalStateException("Parent must implement Step8ReviewFragment.Listener");
        }
        listener = (Listener) context;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_step8_review, container, false);

        draft = (EmotionLogDraft) getArguments().getSerializable(ARG_DRAFT);

        LinearLayout summary = v.findViewById(R.id.summaryContainer);
        Button btnConfirm = v.findViewById(R.id.btnConfirmLog);

        addSummaryRow(summary, getString(R.string.review_label_situation), draft.situation);
        addSummaryRow(summary, getString(R.string.review_label_location), draft.location);
        addSummaryRow(summary, getString(R.string.review_label_companion), draft.companion);
        addSummaryRow(summary, getString(R.string.review_label_feeling), draft.feeling);
        if (draft.note != null && !draft.note.trim().isEmpty()) {
            addSummaryRow(summary, getString(R.string.review_label_note), draft.note);
        }

        TextView txtLabel = v.findViewById(R.id.txtIntensityLabelReview);
        TextView txtNumber = v.findViewById(R.id.txtIntensityNumberReview);
        View glass = v.findViewById(R.id.reviewGlassContainer);
        View fill = v.findViewById(R.id.reviewFillView);

        txtLabel.setText(getString(
                R.string.review_intensity_text,
                getIntensityLabel(draft.intensity)
        ));
        txtNumber.setText(getString(R.string.review_intensity_number, draft.intensity));

        fill.setBackgroundResource(getIntensityFillDrawable(draft.intensity));

        glass.post(() -> {
            int maxH = glass.getHeight();
            int minH = 20;
            float ratio = draft.intensity / 5f;
            int h = (int) (minH + (maxH - minH) * ratio);

            ViewGroup.LayoutParams p = fill.getLayoutParams();
            p.height = h;
            fill.setLayoutParams(p);
        });

        ImageView imgPhoto = v.findViewById(R.id.imgSummaryPhoto);
        if (draft.photoUri != null) {
            imgPhoto.setVisibility(View.VISIBLE);
            imgPhoto.setImageURI(Uri.parse(draft.photoUri));
        }

        btnConfirm.setOnClickListener(view -> {
            if (listener != null) listener.onReviewConfirmed();
        });

        return v;
    }

    private void addSummaryRow(LinearLayout container, String label, String value) {
        if (value == null || value.isEmpty()) return;

        View row = LayoutInflater.from(getContext())
                .inflate(R.layout.summary_row_item, container, false);

        ((TextView) row.findViewById(R.id.txtLabel)).setText(label);
        ((TextView) row.findViewById(R.id.txtValue)).setText(value);

        container.addView(row);
    }

    private String getIntensityLabel(int level) {
        switch (level) {
            case 1:
                return getString(R.string.step4_intensity_level_1);
            case 2:
                return getString(R.string.step4_intensity_level_2);
            case 3:
                return getString(R.string.step4_intensity_level_3);
            case 4:
                return getString(R.string.step4_intensity_level_4);
            case 5:
                return getString(R.string.step4_intensity_level_5);
            default:
                return getString(R.string.step4_intensity_level_1);
        }
    }

    private int getIntensityFillDrawable(int level) {
        switch (level) {
            case 1:
                return R.drawable.intensity_fill_level1;
            case 2:
                return R.drawable.intensity_fill_level2;
            case 3:
                return R.drawable.intensity_fill_level3;
            case 4:
                return R.drawable.intensity_fill_level4;
            case 5:
                return R.drawable.intensity_fill_level5;
            default:
                return R.drawable.intensity_fill_level1;
        }
    }
}