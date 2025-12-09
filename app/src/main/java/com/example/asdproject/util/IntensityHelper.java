package com.example.asdproject.util;

import com.example.asdproject.R;

public class IntensityHelper {

    /**
     * Returns the text label for a given intensity level (1–5).
     */
    public static String getLabel(int level) {
        switch (level) {
            case 1: return "Just a little";
            case 2: return "A little bit";
            case 3: return "Medium";
            case 4: return "A lot";
            case 5: return "Very much";
            default: return "";
        }
    }

    /**
     * Returns the drawable resource for the fill color used in Step 4.
     */
    public static int getFillDrawable(int level) {
        switch (level) {
            case 1: return R.drawable.intensity_fill_level1;
            case 2: return R.drawable.intensity_fill_level2;
            case 3: return R.drawable.intensity_fill_level3;
            case 4: return R.drawable.intensity_fill_level4;
            case 5: return R.drawable.intensity_fill_level5;
            default: return R.drawable.intensity_fill_level1;
        }
    }

    /**
     * Converts intensity (1–5) to a percentage for UI if needed.
     * Example: 3 → 60%
     */
    public static int getPercentage(int level) {
        return (int) ((level / 5f) * 100);
    }
}
