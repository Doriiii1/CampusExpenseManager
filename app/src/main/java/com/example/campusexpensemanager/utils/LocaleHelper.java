package com.example.campusexpensemanager.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;

import java.util.Locale;

/**
 * LocaleHelper - Sprint 6
 * Manages app language (English, Vietnamese, Chinese)
 */
public class LocaleHelper {

    private static final String PREFS_NAME = "AppSettings";
    private static final String KEY_LANGUAGE = "language";
    private static final String DEFAULT_LANGUAGE = "en";

    /**
     * Set app locale
     * @param context Context
     * @param languageCode "en", "vi", or "zh"
     */
    public static void setLocale(Context context, String languageCode) {
        // Save preference
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_LANGUAGE, languageCode).apply();

        // Apply locale
        updateResources(context, languageCode);
    }

    /**
     * Set locale from saved preference
     */
    public static void setLocale(Context context) {
        String languageCode = getLanguage(context);
        updateResources(context, languageCode);
    }

    /**
     * Get saved language code
     * @return "en", "vi", or "zh"
     */
    public static String getLanguage(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_LANGUAGE, DEFAULT_LANGUAGE);
    }

    /**
     * Called in BaseActivity.attachBaseContext()
     */
    public static Context onAttach(Context context) {
        String lang = getLanguage(context);
        return updateResources(context, lang);
    }

    /**
     * Update app resources with new locale
     */
    private static Context updateResources(Context context, String language) {
        Locale locale = new Locale(language);
        Locale.setDefault(locale);
        Resources resources = context.getResources();
        Configuration configuration = new Configuration(resources.getConfiguration());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.setLocale(locale);
            context = context.createConfigurationContext(configuration);
        } else {
            configuration.locale = locale;
            resources.updateConfiguration(configuration, resources.getDisplayMetrics());
        }
        return context;
    }

    /**
     * Get display name for language
     * @param languageCode "en", "vi", or "zh"
     * @return Display name (e.g., "English", "Tiếng Việt", "中文")
     */
    public static String getLanguageDisplayName(String languageCode) {
        switch (languageCode) {
            case "vi":
                return "Tiếng Việt 🇻🇳";
            case "zh":
                return "中文 🇨🇳";
            case "en":
            default:
                return "English 🇺🇸";
        }
    }

    /**
     * Get all available languages
     */
    public static String[] getAvailableLanguages() {
        return new String[]{"en", "vi", "zh"};
    }

    /**
     * Get display names for all languages
     */
    public static String[] getLanguageDisplayNames() {
        return new String[]{
                getLanguageDisplayName("en"),
                getLanguageDisplayName("vi"),
                getLanguageDisplayName("zh")
        };
    }
}