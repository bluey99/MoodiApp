package com.example.asdproject.view.adapters;

import android.content.Context;
import android.content.Intent;
import android.text.BidiFormatter;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.asdproject.R;
import com.example.asdproject.model.EmotionLog;
import com.example.asdproject.model.Feeling;
import com.example.asdproject.util.FeelingUiMapper;
import com.example.asdproject.util.IntensityHelper;
import com.example.asdproject.view.activities.EmotionDetailActivity;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * Adapter that supports 2 view types:
 * - SECTION HEADER
 * - EMOTION ROW
 */
public class HistoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;

    private List<Object> list;

    private final SimpleDateFormat sdf =
            new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());

    public HistoryAdapter(List<Object> mixedList) {
        this.list = mixedList;
    }

    @Override
    public int getItemViewType(int position) {
        return (list.get(position) instanceof String) ? TYPE_HEADER : TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        if (viewType == TYPE_HEADER) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_history_header, parent, false);
            return new HeaderHolder(view);
        }

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_emotion_history, parent, false);
        return new EmotionHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {

        if (holder instanceof HeaderHolder) {
            ((HeaderHolder) holder).txtHeader.setText((String) list.get(position));
            return;
        }

        EmotionHolder h = (EmotionHolder) holder;
        EmotionLog log = (EmotionLog) list.get(position);
        Context context = h.itemView.getContext();

        if (isTaskLog(log)) {
            h.txtTaskBadge.setVisibility(View.VISIBLE);

            if (log.getTaskPrompt() != null && !log.getTaskPrompt().trim().isEmpty()) {
                h.txtTaskPrompt.setVisibility(View.VISIBLE);
                applyContentDirection(
                        h.txtTaskPrompt,
                        context.getString(R.string.history_task_prompt_prefix) + log.getTaskPrompt()
                );
            } else {
                h.txtTaskPrompt.setVisibility(View.GONE);
            }

        } else {
            h.txtTaskBadge.setVisibility(View.GONE);
            h.txtTaskPrompt.setVisibility(View.GONE);
        }

        // ---------------- Emotion ----------------
        String rawFeeling = (log.getFeeling() == null) ? "" : log.getFeeling().trim();

        Feeling feelingEnum;
        String displayText;

        try {
            feelingEnum = Feeling.valueOf(rawFeeling);
            displayText = getLocalizedFeelingLabel(context, feelingEnum);
        } catch (Exception e) {
            feelingEnum = Feeling.OTHER;
            displayText = rawFeeling; // custom typed feeling stays as entered
        }

        applyContentDirection(h.txtEmotion, displayText);
        h.imgEmotion.setImageResource(FeelingUiMapper.getEmojiRes(feelingEnum));

        // ---------------- Intensity ----------------
        h.txtIntensity.setText(
                context.getString(R.string.history_intensity_text, log.getIntensity())
        );

        // ---------------- Note preview ----------------
        if (log.getNote() != null && !log.getNote().trim().isEmpty()) {
            h.txtNotePreview.setVisibility(View.VISIBLE);
            applyContentDirection(h.txtNotePreview, log.getNote());
        } else {
            h.txtNotePreview.setVisibility(View.GONE);
        }

        // ---------------- Timestamp ----------------
        if (log.getTimestamp() != null) {
            h.txtTimestamp.setText(sdf.format(log.getTimestamp().toDate()));
        } else {
            h.txtTimestamp.setText("");
        }

        // ---------------- Mini glass intensity fill ----------------
        View glass = h.itemView.findViewById(R.id.historyGlassContainer);
        View fill = h.itemView.findViewById(R.id.historyFillView);

        fill.setBackgroundResource(
                IntensityHelper.getFillDrawable(log.getIntensity())
        );

        glass.post(() -> {
            int maxHeight = glass.getHeight();
            int minHeight = 6;
            float ratio = log.getIntensity() / 5f;
            int fillHeight = (int) (minHeight + (maxHeight - minHeight) * ratio);

            ViewGroup.LayoutParams lp = fill.getLayoutParams();
            lp.height = fillHeight;
            fill.setLayoutParams(lp);
        });

        // ---------------- Click → detail screen ----------------
        h.itemView.setOnClickListener(v -> openDetailScreen(context, log));
    }

    private void openDetailScreen(Context context, EmotionLog log) {
        Intent intent = new Intent(context, EmotionDetailActivity.class);
        intent.putExtra("feeling", log.getFeeling());
        intent.putExtra("intensity", log.getIntensity());
        intent.putExtra("note", log.getNote());
        intent.putExtra("timestamp", log.getTimestamp() != null
                ? log.getTimestamp().toDate().getTime()
                : 0L);
        context.startActivity(intent);
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class HeaderHolder extends RecyclerView.ViewHolder {
        TextView txtHeader;

        public HeaderHolder(@NonNull View itemView) {
            super(itemView);
            txtHeader = itemView.findViewById(R.id.txtHeaderTitle);
        }
    }

    static class EmotionHolder extends RecyclerView.ViewHolder {

        ImageView imgEmotion;
        TextView txtEmotion;
        TextView txtIntensity;
        TextView txtNotePreview;
        TextView txtTimestamp;
        TextView txtTaskBadge;
        TextView txtTaskPrompt;

        public EmotionHolder(@NonNull View itemView) {
            super(itemView);

            txtTaskBadge = itemView.findViewById(R.id.txtTaskBadge);
            imgEmotion = itemView.findViewById(R.id.imgEmotion);
            txtEmotion = itemView.findViewById(R.id.txtEmotionName);
            txtIntensity = itemView.findViewById(R.id.txtIntensity);
            txtNotePreview = itemView.findViewById(R.id.txtNotePreview);
            txtTimestamp = itemView.findViewById(R.id.txtTimestamp);
            txtTaskPrompt = itemView.findViewById(R.id.txtTaskPrompt);
        }
    }

    /**
     * Replaces the adapter data and refreshes the RecyclerView.
     */
    public void updateData(List<Object> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }

    private boolean isTaskLog(EmotionLog log) {
        return "TASK".equals(log.getLogType());
    }

    // display custom/user-entered content in its original language direction
    private void applyContentDirection(TextView textView, String text) {
        if (text == null) {
            textView.setText("");
            return;
        }

        boolean isRtl = isRtlText(text);
        BidiFormatter bidi = BidiFormatter.getInstance(isRtl);
        textView.setText(bidi.unicodeWrap(text));

        if (isRtl) {
            textView.setTextDirection(View.TEXT_DIRECTION_RTL);
            textView.setGravity(Gravity.RIGHT);
            textView.setTextAlignment(View.TEXT_ALIGNMENT_GRAVITY);
        } else {
            textView.setTextDirection(View.TEXT_DIRECTION_LTR);
            textView.setGravity(Gravity.LEFT);
            textView.setTextAlignment(View.TEXT_ALIGNMENT_GRAVITY);
        }
    }

    // detect first strong directional character
    private boolean isRtlText(String text) {
        if (text == null) return false;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            byte dir = Character.getDirectionality(c);

            if (dir == Character.DIRECTIONALITY_RIGHT_TO_LEFT ||
                    dir == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC) {
                return true;
            }

            if (dir == Character.DIRECTIONALITY_LEFT_TO_RIGHT) {
                return false;
            }
        }

        return false;
    }
    // Returns the localized label for a Feeling enum using string resources
    private String getLocalizedFeelingLabel(Context context, Feeling feeling) {
        switch (feeling) {
            case HAPPY:
                return context.getString(R.string.step3_feeling_happy);
            case SAD:
                return context.getString(R.string.step3_feeling_sad);
            case ANGRY:
                return context.getString(R.string.step3_feeling_angry);
            case SURPRISED:
                return context.getString(R.string.step3_feeling_surprised);
            case AFRAID:
                return context.getString(R.string.step3_feeling_afraid);
            case DISGUST:
                return context.getString(R.string.step3_feeling_disgust);
            case UNSURE:
                return context.getString(R.string.step3_feeling_unsure);
            case OTHER:
            default:
                return context.getString(R.string.step3_feeling_other);
        }
    }
}