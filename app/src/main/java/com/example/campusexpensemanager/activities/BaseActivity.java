package com.example.campusexpensemanager.activities;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.campusexpensemanager.utils.LocaleHelper;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;

import com.example.campusexpensemanager.R;
import com.example.campusexpensemanager.utils.SessionManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.Locale;

/**
 * BaseActivity - Sprint 6
 * Handles locale changes for multi-language support
 * All activities should extend this for language switching
 */
public abstract class BaseActivity extends AppCompatActivity {

    protected BottomNavigationView bottomNavigation;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Apply locale
        LocaleHelper.setLocale(this, LocaleHelper.getLanguage(this));

        // Apply dark mode
        sessionManager = new SessionManager(this);
        if (sessionManager.isDarkModeEnabled()) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        String storedLang = LocaleHelper.getLanguage(this);
        String currentLang = getResources().getConfiguration().locale.getLanguage();
        if (!currentLang.equals(storedLang)) {
            LocaleHelper.setLocale(this, storedLang);
            recreate();
            return;
        }

        if (bottomNavigation != null) {
            int currentId = getCurrentNavigationItem();
            if (currentId != -1 && bottomNavigation.getSelectedItemId() != currentId) {

                bottomNavigation.setSelectedItemId(currentId);
            }
        }
    }

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        setupBottomNavigation();
    }

    /**
     * ✅ Setup global Bottom Navigation
     * Override this method in activities that DON'T need bottom nav
     */
    protected void setupBottomNavigation() {
        bottomNavigation = findViewById(R.id.bottom_navigation);

        if (bottomNavigation != null) {
            // Set selected item based on current activity
            int currentItem = getCurrentNavigationItem();
            if (currentItem != -1) {
                bottomNavigation.setSelectedItemId(currentItem);
            }

            // Handle navigation item clicks
            bottomNavigation.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();

                // Don't navigate if already on current screen
                if (itemId == currentItem) {
                    return true;
                }

                if (itemId == R.id.nav_dashboard) {
                    navigateTo(MainActivity.class);
                    return true;
                } else if (itemId == R.id.nav_expenses) {
                    navigateTo(ExpenseListActivity.class);
                    return true;
                } else if (itemId == R.id.nav_budget) {
                    navigateTo(BudgetDashboardActivity.class);
                    return true;
                } else if (itemId == R.id.nav_report) {
                    navigateTo(ReportActivity.class);
                    return true;
                } else if (itemId == R.id.nav_profile) {
                    navigateTo(ProfileActivity.class);
                    return true;
                }

                return false;
            });
        }
    }

    /**
     * ✅ Get current navigation item ID based on activity
     */
    protected int getCurrentNavigationItem() {
        if (this instanceof MainActivity) {
            return R.id.nav_dashboard;
        } else if (this instanceof ExpenseListActivity) {
            return R.id.nav_expenses;
        } else if (this instanceof BudgetDashboardActivity) {
            return R.id.nav_budget;
        } else if (this instanceof ReportActivity) {
            return R.id.nav_report;
        } else if (this instanceof ProfileActivity) {
            return R.id.nav_profile;
        }
        return -1;
    }

    /**
     * ✅ Navigate to another activity without back stack
     */
    protected void navigateTo(Class<?> targetActivity) {
        if (this.getClass().equals(targetActivity)) {
            return; // Already on this screen
        }

        Intent intent = new Intent(this, targetActivity);
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);
    }

    /**
     * ✅ Hide bottom navigation (for child activities like Add/Edit)
     */
    protected void hideBottomNavigation() {
        if (bottomNavigation != null) {
            bottomNavigation.setVisibility(View.GONE);
        }
    }

    /**
     * ✅ Show bottom navigation
     */
    protected void showBottomNavigation() {
        if (bottomNavigation != null) {
            bottomNavigation.setVisibility(View.VISIBLE);
        }
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