package com.example.asdproject.view.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.asdproject.R;
import com.example.asdproject.model.EmotionLog;
import com.example.asdproject.util.IntensityHelper;
import com.example.asdproject.view.activities.EmotionDetailActivity;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * Adapter that supports 2 view types:
 * - SECTION HEADER ("This Week", "Older Entries")
 * - EMOTION ROW
 */
public class HistoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;

    private final List<Object> list; // MIX of String headers + EmotionLog objects

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

        h.txtEmotion.setText(log.getFeeling());
        h.txtIntensity.setText("Intensity: " + log.getIntensity());

        if (log.getNote() != null && !log.getNote().trim().isEmpty()) {
            h.txtNotePreview.setVisibility(View.VISIBLE);
            h.txtNotePreview.setText(log.getNote());
        } else {
            h.txtNotePreview.setVisibility(View.GONE);
        }


        if (log.getTimestamp() != null) {
            h.txtTimestamp.setText(
                    sdf.format(log.getTimestamp().toDate())
            );

        }

        h.imgEmotion.setImageResource(R.drawable.ic_child);

        // Mini glass fill
        View glass = h.itemView.findViewById(R.id.historyGlassContainer);
        View fill = h.itemView.findViewById(R.id.historyFillView);

        fill.setBackgroundResource(IntensityHelper.getFillDrawable(log.getIntensity()));

        glass.post(() -> {
            int maxHeight = glass.getHeight();
            int minHeight = 6;
            float ratio = log.getIntensity() / 5f;
            int fillHeight = (int) (minHeight + (maxHeight - minHeight) * ratio);

            ViewGroup.LayoutParams lp = fill.getLayoutParams();
            lp.height = fillHeight;
            fill.setLayoutParams(lp);
        });

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

    // HEADER HOLDER
    static class HeaderHolder extends RecyclerView.ViewHolder {
        TextView txtHeader;
        public HeaderHolder(@NonNull View itemView) {
            super(itemView);
            txtHeader = itemView.findViewById(R.id.txtHeaderTitle);
        }
    }

    // EMOTION HOLDER
    static class EmotionHolder extends RecyclerView.ViewHolder {

        ImageView imgEmotion;
        TextView txtEmotion;
        TextView txtIntensity;
        TextView txtNotePreview;
        TextView txtTimestamp;

        public EmotionHolder(@NonNull View itemView) {
            super(itemView);

            imgEmotion = itemView.findViewById(R.id.imgEmotion);
            txtEmotion = itemView.findViewById(R.id.txtEmotionName);
            txtIntensity = itemView.findViewById(R.id.txtIntensity);
            txtNotePreview = itemView.findViewById(R.id.txtNotePreview);
            txtTimestamp = itemView.findViewById(R.id.txtTimestamp);
        }
    }
}
