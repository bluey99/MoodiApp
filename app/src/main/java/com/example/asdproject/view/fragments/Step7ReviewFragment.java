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
import com.example.asdproject.util.IntensityHelper;

/**
 * Step 7 – Review screen
 * Shows a complete summary plus a visual intensity glass.
 */
public class Step7ReviewFragment extends Fragment {

    public interface Listener {
        void onReviewConfirmed();
    }

    private Listener listener;
    private static final String ARG_DRAFT = "draft";
    private EmotionLogDraft draft;

    public static Step7ReviewFragment newInstance(EmotionLogDraft draft) {
        Step7ReviewFragment f = new Step7ReviewFragment();
        Bundle b = new Bundle();
        b.putSerializable(ARG_DRAFT, draft);
        f.setArguments(b);
        return f;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (!(context instanceof Listener)) {
            throw new IllegalStateException("Parent must implement Step7ReviewFragment.Listener");
        }
        listener = (Listener) context;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_step7_review, container, false);

        // Get data
        draft = (EmotionLogDraft) getArguments().getSerializable(ARG_DRAFT);

        LinearLayout summary = v.findViewById(R.id.summaryContainer);
        Button btnConfirm = v.findViewById(R.id.btnConfirmLog);

        // Summary text items
        addSummaryRow(summary, "Situation", draft.situation);
        addSummaryRow(summary, "Location", draft.location);
        addSummaryRow(summary, "Feeling", draft.feeling);
        if (draft.note != null && !draft.note.trim().isEmpty()) {
            addSummaryRow(summary, "Note", draft.note);
        }

        // ----------------------------
        // INTENSITY VISUAL REVIEW
        // ----------------------------
        TextView txtLabel = v.findViewById(R.id.txtIntensityLabelReview);
        TextView txtNumber = v.findViewById(R.id.txtIntensityNumberReview);
        View glass = v.findViewById(R.id.reviewGlassContainer);
        View fill = v.findViewById(R.id.reviewFillView);

        txtLabel.setText("Intensity: " + IntensityHelper.getLabel(draft.intensity));
        txtNumber.setText(draft.intensity + " / 5");

        fill.setBackgroundResource(IntensityHelper.getFillDrawable(draft.intensity));

        glass.post(() -> {
            int maxH = glass.getHeight();
            int minH = 20;
            float ratio = draft.intensity / 5f;
            int h = (int) (minH + (maxH - minH) * ratio);

            ViewGroup.LayoutParams p = fill.getLayoutParams();
            p.height = h;
            fill.setLayoutParams(p);
        });

        // ----------------------------
        // PHOTO (optional)
        // ----------------------------
        ImageView imgPhoto = v.findViewById(R.id.imgSummaryPhoto);
        if (draft.photoUri != null) {
            imgPhoto.setVisibility(View.VISIBLE);
            imgPhoto.setImageURI(Uri.parse(draft.photoUri));
        }

        // Confirm
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
}
