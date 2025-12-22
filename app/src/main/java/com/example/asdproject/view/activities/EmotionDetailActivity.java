package com.example.asdproject.view.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.asdproject.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import android.view.ViewGroup;
import com.example.asdproject.util.IntensityHelper;


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
        TextView headerTitle = findViewById(R.id.txtHeaderTitle);
        headerTitle.setText("My Feeling");

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());


        // Link UI elements to their layout components
        imgEmotion = findViewById(R.id.imgDetailEmotion);
        txtEmotionName = findViewById(R.id.txtDetailEmotionName);
        txtIntensity = findViewById(R.id.txtDetailIntensity);
        txtTimestamp = findViewById(R.id.txtDetailTimestamp);
        txtNote = findViewById(R.id.txtDetailNote);

        // Extract emotion entry data passed from the history list
        String emotion = getIntent().getStringExtra("feeling");
        int intensity = getIntent().getIntExtra("intensity", 0);
        String note = getIntent().getStringExtra("note");
        long timestamp = getIntent().getLongExtra("timestamp", 0);
        // --- INTENSITY GLASS (same logic as history) ---
        View glass = findViewById(R.id.detailGlassContainer);
        View fill = findViewById(R.id.detailFillView);

// Set fill color based on intensity
        fill.setBackgroundResource(IntensityHelper.getFillDrawable(intensity));

// Calculate fill height (same formula as history)
        glass.post(() -> {
            int maxHeight = glass.getHeight();
            int minHeight = 6;
            float ratio = intensity / 5f;
            int fillHeight = (int) (minHeight + (maxHeight - minHeight) * ratio);

            ViewGroup.LayoutParams lp = fill.getLayoutParams();
            lp.height = fillHeight;
            fill.setLayoutParams(lp);
        });


        // Display emotion name and intensity
        txtEmotionName.setText(emotion);
        txtIntensity.setText("How strong it felt: " + intensity + " / 5");

        // Format and display the timestamp
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());
        if (timestamp > 0) {
            txtTimestamp.setText(sdf.format(new Date(timestamp)));
        } else {
            txtTimestamp.setVisibility(View.GONE);
        }


        // Show the note or a fallback message if none exists
        if (note == null || note.trim().isEmpty()) {
            txtNote.setVisibility(View.GONE);
        } else {
            txtNote.setText(note);
            txtNote.setVisibility(View.VISIBLE);
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

        switch (emotion.toLowerCase(Locale.ROOT)) {
            case "happy":
                imgEmotion.setImageResource(R.drawable.ic_child);
                break;

            case "sad":
                imgEmotion.setImageResource(R.drawable.ic_child);
                break;

            default:
                imgEmotion.setImageResource(R.drawable.ic_child);
                break;
        }
    }

}
