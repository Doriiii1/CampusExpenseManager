package com.example.campusexpensemanager.activities;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.campusexpensemanager.R;
import com.example.campusexpensemanager.utils.DatabaseHelper;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * MainActivity serves as the main dashboard with bottom navigation
 * Manages navigation between Dashboard, Expenses, and Profile tabs
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private BottomNavigationView bottomNavigation;
    private TextView tvDashboardPlaceholder;
    private TextView tvExpensesPlaceholder;
    private TextView tvProfilePlaceholder;

    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize database
        dbHelper = DatabaseHelper.getInstance(this);

        // Initialize views
        initializeViews();

        // Setup bottom navigation
        setupBottomNavigation();

        // Show default tab (Dashboard)
        showDashboard();
    }

    /**
     * Initialize all view components
     */
    private void initializeViews() {
        bottomNavigation = findViewById(R.id.bottom_navigation);
        tvDashboardPlaceholder = findViewById(R.id.tv_dashboard_placeholder);
        tvExpensesPlaceholder = findViewById(R.id.tv_expenses_placeholder);
        tvProfilePlaceholder = findViewById(R.id.tv_profile_placeholder);
    }

    /**
     * Setup bottom navigation with item selection listener
     */
    private void setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_dashboard) {
                showDashboard();
                return true;
            } else if (itemId == R.id.nav_expenses) {
                showExpenses();
                return true;
            } else if (itemId == R.id.nav_profile) {
                showProfile();
                return true;
            }

            return false;
        });
    }

    /**
     * Show Dashboard tab
     */
    private void showDashboard() {
        tvDashboardPlaceholder.setVisibility(TextView.VISIBLE);
        tvExpensesPlaceholder.setVisibility(TextView.GONE);
        tvProfilePlaceholder.setVisibility(TextView.GONE);

        // TODO: Load dashboard data in Sprint 4
        tvDashboardPlaceholder.setText("Dashboard\n\nComing Soon:\n• Total Spent Summary\n• Budget Overview\n• Quick Actions");
    }

    /**
     * Show Expenses tab
     */
    private void showExpenses() {
        tvDashboardPlaceholder.setVisibility(TextView.GONE);
        tvExpensesPlaceholder.setVisibility(TextView.VISIBLE);
        tvProfilePlaceholder.setVisibility(TextView.GONE);

        // TODO: Load expenses list in Sprint 3
        tvExpensesPlaceholder.setText("Expenses\n\nComing Soon:\n• Add New Expense\n• View Expense List\n• Edit/Delete Expenses");
    }

    /**
     * Show Profile tab
     */
    private void showProfile() {
        tvDashboardPlaceholder.setVisibility(TextView.GONE);
        tvExpensesPlaceholder.setVisibility(TextView.GONE);
        tvProfilePlaceholder.setVisibility(TextView.VISIBLE);

        // TODO: Load profile data in Sprint 2
        tvProfilePlaceholder.setText("Profile\n\nComing Soon:\n• View Profile Info\n• Edit Profile\n• Dark Mode Toggle\n• Logout");
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh data when activity resumes
        refreshCurrentTab();
    }

    /**
     * Refresh the currently selected tab
     */
    private void refreshCurrentTab() {
        int selectedItemId = bottomNavigation.getSelectedItemId();

        if (selectedItemId == R.id.nav_dashboard) {
            showDashboard();
        } else if (selectedItemId == R.id.nav_expenses) {
            showExpenses();
        } else if (selectedItemId == R.id.nav_profile) {
            showProfile();
        }
    }
}