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
import com.example.asdproject.view.activities.EmotionDetailActivity;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * RecyclerView adapter responsible for displaying the child's emotion history.
 * Each list item represents a single emotion entry and opens the detail screen on click.
 */
public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryHolder> {

    private final List<EmotionLog> list;
    private final SimpleDateFormat sdf =
            new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());

    public HistoryAdapter(List<EmotionLog> list) {
        this.list = list;
    }

    @NonNull
    @Override
    public HistoryHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_emotion_history, parent, false);
        return new HistoryHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryHolder holder, int position) {
        EmotionLog log = list.get(position);
        Context context = holder.itemView.getContext();

        // Display emotion name and intensity
        holder.txtEmotion.setText(log.getEmotion());
        holder.txtIntensity.setText("Intensity: " + log.getIntensity());

        // Format timestamp or fallback if missing
        if (log.getTimestamp() != null) {
            holder.txtTimestamp.setText(sdf.format(log.getTimestamp()));
        } else {
            holder.txtTimestamp.setText("No time");
        }

        // Display note preview or fallback if empty
        if (log.getNote() != null && !log.getNote().trim().isEmpty()) {
            holder.txtNotePreview.setText(log.getNote());
        } else {
            holder.txtNotePreview.setText("No note");
        }

        // Placeholder icon until emotion-specific icons are available
        holder.imgEmotion.setImageResource(R.drawable.ic_child);

        // Open detail screen when an item is clicked
        holder.itemView.setOnClickListener(v -> openDetailScreen(context, log));
    }

    /**
     * Launches the EmotionDetailActivity with the selected emotion entry.
     *
     * @param context the context from the item view
     * @param log     the emotion record selected by the user
     */
    private void openDetailScreen(Context context, EmotionLog log) {
        Intent intent = new Intent(context, EmotionDetailActivity.class);

        intent.putExtra("emotion", log.getEmotion());
        intent.putExtra("intensity", log.getIntensity());
        intent.putExtra("note", log.getNote());

        long timestampValue = (log.getTimestamp() != null)
                ? log.getTimestamp().getTime()
                : 0L;

        intent.putExtra("timestamp", timestampValue);

        context.startActivity(intent);
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    /**
     * ViewHolder for each emotion history row.
     */
    static class HistoryHolder extends RecyclerView.ViewHolder {

        ImageView imgEmotion;
        TextView txtEmotion;
        TextView txtIntensity;
        TextView txtNotePreview;
        TextView txtTimestamp;

        public HistoryHolder(@NonNull View itemView) {
            super(itemView);

            imgEmotion = itemView.findViewById(R.id.imgEmotion);
            txtEmotion = itemView.findViewById(R.id.txtEmotionName);
            txtIntensity = itemView.findViewById(R.id.txtIntensity);
            txtNotePreview = itemView.findViewById(R.id.txtNotePreview);
            txtTimestamp = itemView.findViewById(R.id.txtTimestamp);
        }
    }
}
