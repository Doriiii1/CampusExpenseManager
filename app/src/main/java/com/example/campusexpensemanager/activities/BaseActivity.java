package com.example.campusexpensemanager.activities;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.campusexpensemanager.utils.LocaleHelper;

import java.util.Locale;

/**
 * BaseActivity - Sprint 6
 * Handles locale changes for multi-language support
 * All activities should extend this for language switching
 */
public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Apply saved locale
        LocaleHelper.setLocale(this);
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        // Apply locale to context
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    /**
     * Change app language and recreate activity
     * @param languageCode "en", "vi", or "zh"
     */
    protected void changeLanguage(String languageCode) {
        LocaleHelper.setLocale(this, languageCode);
        recreate(); // Recreate activity to apply new locale
    }

    /**
     * Get current language code
     * @return "en", "vi", or "zh"
     */
    protected String getCurrentLanguage() {
        return LocaleHelper.getLanguage(this);
    }
}