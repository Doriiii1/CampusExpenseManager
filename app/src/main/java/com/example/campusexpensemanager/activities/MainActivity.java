package com.example.campusexpensemanager.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
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
    private LinearLayout layoutDashboard;
    private TextView tvGreeting, tvMonthlyTotal, tvBudgetRemaining, tvTopCategory;
    private Button btnAddExpense, btnViewBudget, btnGenerateReport;

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
        layoutDashboard = findViewById(R.id.layout_dashboard);
        tvGreeting = findViewById(R.id.tv_greeting);
        tvMonthlyTotal = findViewById(R.id.tv_monthly_total);
        tvBudgetRemaining = findViewById(R.id.tv_budget_remaining);
        tvTopCategory = findViewById(R.id.tv_top_category);
        btnAddExpense = findViewById(R.id.btn_add_expense);
        btnViewBudget = findViewById(R.id.btn_view_budget);
        btnGenerateReport = findViewById(R.id.btn_generate_report);

        // Setup button clicks
        btnAddExpense.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddExpenseActivity.class);
            startActivity(intent);
        });

        btnViewBudget.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, BudgetDashboardActivity.class);
            startActivity(intent);
        });

        btnGenerateReport.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ReportActivity.class);
            startActivity(intent);
        });
    }

    /**
     * Setup bottom navigation with item selection listener
     */
    private void setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_dashboard) {
                // Stay on dashboard - just show it
                showDashboard();
                return true;
            } else if (itemId == R.id.nav_expenses) {
                // Navigate to Expenses Activity
                Intent intent = new Intent(MainActivity.this, ExpenseListActivity.class);
                startActivity(intent);
                // Don't return true to prevent bottom nav selection change
                return false;
            } else if (itemId == R.id.nav_profile) {
                // Navigate to Profile Activity
                Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
                startActivity(intent);
                // Don't return true to prevent bottom nav selection change
                return false;
            }

            return false;
        });

        // Set default selection to Dashboard
        bottomNavigation.setSelectedItemId(R.id.nav_dashboard);
    }

    /**
     * Show Dashboard tab
     */
    private void showDashboard() {
        layoutDashboard.setVisibility(View.VISIBLE);
        loadDashboardData();
    }

    /**
     * Load dashboard summary data
     */
    private void loadDashboardData() {
        try {
            int userId = sessionManager.getUserId();

            // Get user name for greeting
            String userName = sessionManager.getUserName();
            if (userName == null || userName.isEmpty()) {
                userName = "User";
            }
            tvGreeting.setText("Hello, " + userName + "! 👋");

            // Get current month expenses
            List<Expense> expenses = dbHelper.getExpensesByUser(userId);

            // Calculate this month's total
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.DAY_OF_MONTH, 1);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
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
            String formattedRemaining = currencyFormat.format(Math.max(0, budgetRemaining)) + "đ";
            String formattedTopAmount = currencyFormat.format(topAmount) + "đ";

            // Update UI
            tvMonthlyTotal.setText("Total Spent: " + formattedTotal);
            tvBudgetRemaining.setText("Budget Remaining: " + formattedRemaining);
            tvTopCategory.setText("Top Category: " + topCategory + " (" + formattedTopAmount + ")");

        } catch (Exception e) {
            e.printStackTrace();
            tvGreeting.setText("Hello, User! 👋");
            tvMonthlyTotal.setText("Total Spent: 0đ");
            tvBudgetRemaining.setText("Budget Remaining: 0đ");
            tvTopCategory.setText("Top Category: None");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh dashboard when returning from other activities
        if (layoutDashboard != null && layoutDashboard.getVisibility() == View.VISIBLE) {
            loadDashboardData();
        }

        // Always reset bottom nav to dashboard when returning
        if (bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(R.id.nav_dashboard);
        }
    }
}