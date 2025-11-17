package com.example.campusexpensemanager.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.example.campusexpensemanager.R;
import com.example.campusexpensemanager.models.Budget;
import com.example.campusexpensemanager.models.Category;
import com.example.campusexpensemanager.models.Expense;
import com.example.campusexpensemanager.utils.DatabaseHelper;
import com.example.campusexpensemanager.utils.SessionManager;
import com.example.campusexpensemanager.workers.RecurringExpenseWorker;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;

import java.text.NumberFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * MainActivity - Enhanced Dashboard with Income/Expense/Balance cards
 * Sprint 5: Shows 3-card balance summary
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private BottomNavigationView bottomNavigation;
    private LinearLayout layoutDashboard;

    // Balance summary cards
    private MaterialCardView cardIncome, cardExpense, cardBalance;
    private TextView tvIncomeAmount, tvExpenseAmount, tvBalanceAmount;
    private TextView tvIncomeLabel, tvExpenseLabel, tvBalanceLabel;

    private TextView tvGreeting, tvTopCategory;
    private Button btnAddExpense, btnViewBudget, btnGenerateReport;

    private DatabaseHelper dbHelper;
    private SessionManager sessionManager;
    private NumberFormat currencyFormat;

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

        // Initialize database and formatters
        dbHelper = DatabaseHelper.getInstance(this);
        currencyFormat = NumberFormat.getInstance(new Locale("vi", "VN"));

        // Initialize views
        initializeViews();

        // Setup bottom navigation
        setupBottomNavigation();

        // Setup recurring expense worker
        setupRecurringWorker();

        // Show default tab (Dashboard)
        showDashboard();
    }

    private void navigateToLogin() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void initializeViews() {
        bottomNavigation = findViewById(R.id.bottom_navigation);
        layoutDashboard = findViewById(R.id.layout_dashboard);
        tvGreeting = findViewById(R.id.tv_greeting);
        tvTopCategory = findViewById(R.id.tv_top_category);

        // Balance cards
        cardIncome = findViewById(R.id.card_income);
        cardExpense = findViewById(R.id.card_expense);
        cardBalance = findViewById(R.id.card_balance);

        tvIncomeAmount = findViewById(R.id.tv_income_amount);
        tvExpenseAmount = findViewById(R.id.tv_expense_amount);
        tvBalanceAmount = findViewById(R.id.tv_balance_amount);

        tvIncomeLabel = findViewById(R.id.tv_income_label);
        tvExpenseLabel = findViewById(R.id.tv_expense_label);
        tvBalanceLabel = findViewById(R.id.tv_balance_label);

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

    private void setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_dashboard) {
                showDashboard();
                return true;
            } else if (itemId == R.id.nav_expenses) {
                Intent intent = new Intent(MainActivity.this, ExpenseListActivity.class);
                startActivity(intent);
                return false;
            } else if (itemId == R.id.nav_profile) {
                Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
                startActivity(intent);
                return false;
            }

            return false;
        });

        bottomNavigation.setSelectedItemId(R.id.nav_dashboard);
    }

    /**
     * Setup recurring expense background worker
     * Runs daily at midnight
     */
    private void setupRecurringWorker() {
        PeriodicWorkRequest recurringWork = new PeriodicWorkRequest.Builder(
                RecurringExpenseWorker.class,
                1, // Repeat interval
                TimeUnit.DAYS // Every 1 day
        ).build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "recurring_expenses",
                ExistingPeriodicWorkPolicy.KEEP, // Keep existing if already scheduled
                recurringWork
        );
    }

    private void showDashboard() {
        layoutDashboard.setVisibility(View.VISIBLE);
        loadDashboardData();
    }

    private void loadDashboardData() {
        try {
            int userId = sessionManager.getUserId();

            // Greeting
            String userName = sessionManager.getUserName();
            if (userName == null || userName.isEmpty()) {
                userName = "User";
            }
            tvGreeting.setText("Hello, " + userName + "! 👋");

            // Get current month date range
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.DAY_OF_MONTH, 1);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            long monthStart = calendar.getTimeInMillis();

            calendar.add(Calendar.MONTH, 1);
            long monthEnd = calendar.getTimeInMillis();

            // Calculate Income, Expense, Balance
            double totalIncome = dbHelper.getTotalIncome(userId, monthStart, monthEnd);
            double totalExpense = dbHelper.getTotalExpense(userId, monthStart, monthEnd);
            double balance = totalIncome - totalExpense;

            // Format and display
            String formattedIncome = currencyFormat.format(totalIncome) + "đ";
            String formattedExpense = currencyFormat.format(totalExpense) + "đ";
            String formattedBalance = currencyFormat.format(Math.abs(balance)) + "đ";

            tvIncomeAmount.setText("+" + formattedIncome);
            tvExpenseAmount.setText("-" + formattedExpense);
            tvBalanceAmount.setText((balance >= 0 ? "+" : "-") + formattedBalance);

            // Color coding for balance
            if (balance >= 0) {
                tvBalanceAmount.setTextColor(getResources().getColor(R.color.success));
                cardBalance.setCardBackgroundColor(getResources().getColor(R.color.light_surface));
            } else {
                tvBalanceAmount.setTextColor(getResources().getColor(R.color.error));
                cardBalance.setCardBackgroundColor(getResources().getColor(R.color.light_surface));
            }

            // Top category
            List<Expense> expenses = dbHelper.getExpensesByUser(userId);
            java.util.Map<Integer, Double> categoryTotals = new java.util.HashMap<>();

            for (Expense expense : expenses) {
                if (expense.getDate() >= monthStart && expense.getDate() < monthEnd) {
                    if (expense.isExpense()) { // Only count expenses, not income
                        int categoryId = expense.getCategoryId();
                        categoryTotals.put(categoryId,
                                categoryTotals.getOrDefault(categoryId, 0.0) + expense.getAmount());
                    }
                }
            }

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

            String formattedTopAmount = currencyFormat.format(topAmount) + "đ";
            tvTopCategory.setText("Top Category: " + topCategory + " (" + formattedTopAmount + ")");

        } catch (Exception e) {
            e.printStackTrace();
            tvGreeting.setText("Hello, User! 👋");
            tvIncomeAmount.setText("+0đ");
            tvExpenseAmount.setText("-0đ");
            tvBalanceAmount.setText("0đ");
            tvTopCategory.setText("Top Category: None");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (layoutDashboard != null && layoutDashboard.getVisibility() == View.VISIBLE) {
            loadDashboardData();
        }

        if (bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(R.id.nav_dashboard);
        }
    }
}