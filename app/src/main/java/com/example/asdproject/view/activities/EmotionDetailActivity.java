package com.example.asdproject.view.activities;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.asdproject.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Displays the detailed information for a single recorded emotion entry.
 * Data is received via Intent extras from HistoryAdapter.
 */
public class EmotionDetailActivity extends AppCompatActivity {

    private ImageView imgEmotion;
    private TextView txtEmotionName;
    private TextView txtIntensity;
    private TextView txtTimestamp;
    private TextView txtNote;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emotion_detail);

        // Link UI elements to their layout components
        imgEmotion = findViewById(R.id.imgDetailEmotion);
        txtEmotionName = findViewById(R.id.txtDetailEmotionName);
        txtIntensity = findViewById(R.id.txtDetailIntensity);
        txtTimestamp = findViewById(R.id.txtDetailTimestamp);
        txtNote = findViewById(R.id.txtDetailNote);

        // Extract emotion entry data passed from the history list
        String emotion = getIntent().getStringExtra("emotion");
        int intensity = getIntent().getIntExtra("intensity", 0);
        String note = getIntent().getStringExtra("note");
        long timestamp = getIntent().getLongExtra("timestamp", 0);

        // Display emotion name and intensity
        txtEmotionName.setText(emotion);
        txtIntensity.setText("Intensity: " + intensity);

        // Format and display the timestamp
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());
        txtTimestamp.setText(sdf.format(new Date(timestamp)));

        // Show the note or a fallback message if none exists
        if (note == null || note.trim().isEmpty()) {
            txtNote.setText("No notes for this feeling.");
        } else {
            txtNote.setText(note);
        }

        // Assign an icon based on the emotion type
        setEmotionIcon(emotion);
    }

    /**
     * Sets the displayed icon based on the emotion's name.
     * This uses placeholder icons until emotion-specific resources are added.
     *
     * @param emotion The emotion string provided from the history entry.
     */
    private void setEmotionIcon(String emotion) {
        if (emotion == null) {
            imgEmotion.setImageResource(R.drawable.ic_child);
            return;
        }

        switch (emotion.toLowerCase()) {
            case "happy":
                imgEmotion.setImageResource(R.drawable.ic_child);  // Replace with ic_emoji_happy when available
                break;

            case "sad":
                imgEmotion.setImageResource(R.drawable.ic_child);  // Replace with ic_emoji_sad when available
                break;

            default:
                imgEmotion.setImageResource(R.drawable.ic_child);  // Replace with placeholder when available
                break;
        }
    }
}
