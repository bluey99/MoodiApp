package com.example.asdproject.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

public class LanguageHelper {

    private static final String PREFS = "lang_prefs";
    private static final String KEY_LANG = "lang"; // "en" or "ar"

    public static void applySavedLanguage(Context context) {
        String lang = getSavedLanguage(context);
        setLanguage(lang);
    }

    public static void toggleLanguage(Context context) {
        String current = getSavedLanguage(context);
        String next = current.equals("ar") ? "en" : "ar";
        saveLanguage(context, next);
        setLanguage(next);
    }

    private static void setLanguage(String langCode) {
        LocaleListCompat locales = LocaleListCompat.forLanguageTags(langCode);
        AppCompatDelegate.setApplicationLocales(locales);
    }

    private static void saveLanguage(Context context, String lang) {
        SharedPreferences sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        sp.edit().putString(KEY_LANG, lang).apply();
    }

    public static String getSavedLanguage(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return sp.getString(KEY_LANG, "en"); // default English
    }
}