package com.example.asdproject.view.activities;

import android.os.Bundle;
import android.text.BidiFormatter;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.asdproject.R;
import com.example.asdproject.model.Feeling;
import com.example.asdproject.util.FeelingUiMapper;
import com.example.asdproject.util.IntensityHelper;
import com.example.asdproject.util.LocaleHelper;

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
    private TextView txtSituation;
    private TextView txtLocation;
    private TextView txtCompanion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LocaleHelper.applyLanguage(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emotion_detail);

        TextView headerTitle = findViewById(R.id.txtHeaderTitle);
        headerTitle.setText(getString(R.string.emotion_detail_header_title));

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Link UI elements
        imgEmotion = findViewById(R.id.imgDetailEmotion);
        txtEmotionName = findViewById(R.id.txtDetailEmotionName);
        txtIntensity = findViewById(R.id.txtDetailIntensity);
        txtTimestamp = findViewById(R.id.txtDetailTimestamp);
        txtNote = findViewById(R.id.txtDetailNote);
        txtSituation = findViewById(R.id.txtDetailSituation);
        txtLocation = findViewById(R.id.txtDetailLocation);
        txtCompanion = findViewById(R.id.txtDetailCompanion);

        // Extract emotion entry data
        String emotionStr = getIntent().getStringExtra("feeling");
        String rawFeeling = (emotionStr == null) ? "" : emotionStr.trim();

        Feeling feelingEnum;
        String displayText;

        try {
            feelingEnum = Feeling.valueOf(rawFeeling);
            displayText = getLocalizedFeelingLabel(feelingEnum);
        } catch (Exception e) {
            // bayan added here - typed custom feeling: display as-is and use OTHER icon
            feelingEnum = Feeling.OTHER;
            displayText = rawFeeling;
        }

        int intensity = getIntent().getIntExtra("intensity", 0);
        String note = getIntent().getStringExtra("note");
        long timestamp = getIntent().getLongExtra("timestamp", 0);
        String situation = getIntent().getStringExtra("situation");
        String location = getIntent().getStringExtra("location");
        String companion = getIntent().getStringExtra("companion");

        // --- INTENSITY GLASS ---
        View glass = findViewById(R.id.detailGlassContainer);
        View fill = findViewById(R.id.detailFillView);

        fill.setBackgroundResource(IntensityHelper.getFillDrawable(intensity));

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
        applyContentDirection(txtEmotionName, displayText);
        txtIntensity.setText(getString(R.string.emotion_detail_intensity_text, intensity));

        // Format and display timestamp
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());
        if (timestamp > 0) {
            txtTimestamp.setText(sdf.format(new Date(timestamp)));
        } else {
            txtTimestamp.setVisibility(View.GONE);
        }

        // show situation if available
        if (situation == null || situation.trim().isEmpty()) {
            txtSituation.setVisibility(View.GONE);
        } else {
            applyContentDirection(
                    txtSituation,
                    getString(R.string.emotion_detail_situation_text, situation)
            );
            txtSituation.setVisibility(View.VISIBLE);
        }

        // show location if available
        if (location == null || location.trim().isEmpty()) {
            txtLocation.setVisibility(View.GONE);
        } else {
            applyContentDirection(
                    txtLocation,
                    getString(R.string.emotion_detail_location_text, location)
            );
            txtLocation.setVisibility(View.VISIBLE);
        }

        //  show companion if available
        if (companion == null || companion.trim().isEmpty()) {
            txtCompanion.setVisibility(View.GONE);
        } else {
            applyContentDirection(
                    txtCompanion,
                    getString(R.string.emotion_detail_companion_text, companion)
            );
            txtCompanion.setVisibility(View.VISIBLE);
        }

        // Show note if exists
        if (note == null || note.trim().isEmpty()) {
            txtNote.setVisibility(View.GONE);
        } else {
            applyContentDirection(txtNote, note);
            txtNote.setVisibility(View.VISIBLE);
        }

        // Assign icon
        imgEmotion.setImageResource(FeelingUiMapper.getEmojiRes(feelingEnum));
    }

    // Returns the localized label for a Feeling enum using string resources
    private String getLocalizedFeelingLabel(Feeling feeling) {
        switch (feeling) {
            case HAPPY:
                return getString(R.string.step4_feeling_happy);
            case SAD:
                return getString(R.string.step4_feeling_sad);
            case ANGRY:
                return getString(R.string.step4_feeling_angry);
            case SURPRISED:
                return getString(R.string.step4_feeling_surprised);
            case AFRAID:
                return getString(R.string.step4_feeling_afraid);
            case DISGUST:
                return getString(R.string.step4_feeling_disgust);
            case UNSURE:
                return getString(R.string.step4_feeling_unsure);
            case OTHER:
            default:
                return getString(R.string.step4_feeling_other);
        }
    }

    // display content according to the language it was written in
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

    // detect direction from the first strong character
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
}