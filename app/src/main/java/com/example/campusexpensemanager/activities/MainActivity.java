package com.example.campusexpensemanager.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.campusexpensemanager.R;
import com.example.campusexpensemanager.models.Budget;
import com.example.campusexpensemanager.models.Category;
import com.example.campusexpensemanager.models.Expense;
import com.example.campusexpensemanager.utils.DatabaseHelper;
import com.example.campusexpensemanager.utils.SessionManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.Calendar;
import java.util.List;

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

    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize session manager
        sessionManager = new SessionManager(this);

        // Check if user is logged in
        if (!sessionManager.isLoggedIn()) {
            navigateToLogin();
            return;
        }

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
     * Navigate to LoginActivity if not authenticated
     */
    private void navigateToLogin() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
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

        // Load dashboard data
        loadDashboardData();
    }

    /**
     * Load dashboard summary data
     */
    private void loadDashboardData() {
        int userId = sessionManager.getUserId();

        // Get user name for greeting
        String userName = sessionManager.getUserName();
        if (userName == null) {
            userName = "User";
        }

        // Get current month expenses
        List<Expense> expenses = dbHelper.getExpensesByUser(userId);

        // Calculate this month's total
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        long monthStart = calendar.getTimeInMillis();

        double monthlyTotal = 0;
        java.util.Map<Integer, Double> categoryTotals = new java.util.HashMap<>();

        for (Expense expense : expenses) {
            if (expense.getDate() >= monthStart) {
                monthlyTotal += expense.getAmount();

                int categoryId = expense.getCategoryId();
                categoryTotals.put(categoryId,
                        categoryTotals.getOrDefault(categoryId, 0.0) + expense.getAmount());
            }
        }

        // Get budgets
        List<Budget> budgets = dbHelper.getBudgetsByUser(userId);
        double totalBudget = 0;
        for (Budget budget : budgets) {
            if (budget.getPeriodEnd() >= System.currentTimeMillis()) {
                totalBudget += budget.getAmount();
            }
        }

        double budgetRemaining = totalBudget - monthlyTotal;

        // Find top category
        String topCategory = "None";
        double topAmount = 0;
        for (java.util.Map.Entry<Integer, Double> entry : categoryTotals.entrySet()) {
            if (entry.getValue() > topAmount) {
                topAmount = entry.getValue();
                Category cat = dbHelper.getCategoryById(entry.getKey());
                if (cat != null) {
                    topCategory = cat.getName();
                }
            }
        }

        // Format amounts
        java.text.NumberFormat currencyFormat = java.text.NumberFormat.getInstance(
                new java.util.Locale("vi", "VN"));
        String formattedTotal = currencyFormat.format(monthlyTotal) + "đ";
        String formattedRemaining = currencyFormat.format(budgetRemaining) + "đ";
        String formattedTopAmount = currencyFormat.format(topAmount) + "đ";

        // Build dashboard text
        StringBuilder dashboard = new StringBuilder();
        dashboard.append("Hello, ").append(userName).append("! 👋\n\n");
        dashboard.append("📊 Monthly Summary\n");
        dashboard.append("━━━━━━━━━━━━━━━━\n\n");
        dashboard.append("💰 Total Spent: ").append(formattedTotal).append("\n");
        dashboard.append("💵 Budget Remaining: ").append(formattedRemaining).append("\n");
        dashboard.append("🏆 Top Category: ").append(topCategory)
                .append(" (").append(formattedTopAmount).append(")\n\n");
        dashboard.append("⚡ Quick Actions\n");
        dashboard.append("━━━━━━━━━━━━━━━━\n\n");
        dashboard.append("• Add Expense\n");
        dashboard.append("• View Budget\n");
        dashboard.append("• Generate Report\n");

        tvDashboardPlaceholder.setText(dashboard.toString());
    }

    /**
     * Show Expenses tab
     */
    private void showExpenses() {
        // Navigate to ExpenseListActivity instead of showing placeholder
        Intent intent = new Intent(MainActivity.this, ExpenseListActivity.class);
        startActivity(intent);
    }

    /**
     * Show Profile tab
     */
    private void showProfile() {
        tvDashboardPlaceholder.setVisibility(TextView.GONE);
        tvExpensesPlaceholder.setVisibility(TextView.GONE);
        tvProfilePlaceholder.setVisibility(TextView.VISIBLE);

        // Navigate to ProfileActivity
        Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
        startActivity(intent);
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