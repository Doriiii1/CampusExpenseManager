package com.example.campusexpensemanager;

import android.app.Application;

import androidx.work.Configuration;
import androidx.work.WorkManager;

/**
 * Application class for manual WorkManager initialization
 * ONLY USE IF YOU NEED CUSTOM CONFIGURATION
 */
public class CampusExpenseApp extends Application implements Configuration.Provider {

    @Override
    public void onCreate() {
        super.onCreate();

        // Manual WorkManager initialization
        WorkManager.initialize(
                this,
                getWorkManagerConfiguration()
        );
    }

    @Override
    public Configuration getWorkManagerConfiguration() {
        return new Configuration.Builder()
                .setMinimumLoggingLevel(android.util.Log.INFO)
                .build();
    }
}