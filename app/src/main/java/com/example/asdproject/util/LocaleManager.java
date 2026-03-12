package com.example.asdproject.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;

import java.util.Locale;

public class LocaleManager {

    private static final String PREFS = "lang_prefs";
    private static final String KEY = "lang";

    public static Context setLocale(Context context) {
        String lang = getLang(context);
        return updateResources(context, lang);
    }

    public static void toggleLanguage(Context context) {
        String current = getLang(context);
        String next = current.equals("ar") ? "en" : "ar";
        saveLang(context, next);
    }

    public static String getLang(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return sp.getString(KEY, "en");
    }

    private static void saveLang(Context context, String lang) {
        SharedPreferences sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        sp.edit().putString(KEY, lang).apply();
    }

    private static Context updateResources(Context context, String lang) {
        Locale locale = new Locale(lang);
        Locale.setDefault(locale);

        Configuration config = new Configuration(context.getResources().getConfiguration());
        config.setLocale(locale);

        return context.createConfigurationContext(config);
    }
}