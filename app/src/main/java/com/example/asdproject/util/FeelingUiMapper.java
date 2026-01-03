package com.example.asdproject.util;

import com.example.asdproject.R;
import com.example.asdproject.model.Feeling;

public class FeelingUiMapper {

    public static int getEmojiRes(Feeling feeling) {
        if (feeling == null) {
            return R.drawable.emoji_unsure;
        }

        switch (feeling) {
            case HAPPY:
                return R.drawable.emoji_happy;
            case SAD:
                return R.drawable.emoji_sad;
            case ANGRY:
                return R.drawable.emoji_angry;
            case SURPRISED:
                return R.drawable.emoji_surprised;
            case AFRAID:
                return R.drawable.emoji_afraid;
            case DISGUST:
                return R.drawable.emoji_disgusted;
            case UNSURE:
            default:
                return R.drawable.emoji_unsure;
        }
    }
    public static String getLabel(Feeling feeling) {
        if (feeling == null) {
            return "Unsure";
        }

        switch (feeling) {
            case HAPPY:
                return "Happy";
            case SAD:
                return "Sad";
            case ANGRY:
                return "Angry";
            case SURPRISED:
                return "Surprised";
            case AFRAID:
                return "Afraid";
            case DISGUST:
                return "Disgust";
            case UNSURE:
                return "Unsure";
            case OTHER:
                return "Other";
            default:
                return "Unsure";
        }
    }


}
