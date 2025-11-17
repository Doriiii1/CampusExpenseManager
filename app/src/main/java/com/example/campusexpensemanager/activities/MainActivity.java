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
import com.example.campusexpensemanager.models.Category;
import com.example.campusexpensemanager.models.Expense;
import com.example.campusexpensemanager.utils.CurrencyConverter;
import com.example.campusexpensemanager.utils.DatabaseHelper;
import com.example.campusexpensemanager.utils.SessionManager;
import com.example.campusexpensemanager.workers.RecurringExpenseWorker;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.Calendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * MainActivity - Enhanced with proper Currency display
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private BottomNavigationView bottomNavigation;
    private LinearLayout layoutDashboard;

    private TextView tvIncomeAmount, tvExpenseAmount, tvBalanceAmount;
    private TextView tvGreeting, tvTopCategory;
    private Button btnAddExpense, btnViewBudget, btnGenerateReport;

    private DatabaseHelper dbHelper;
    private SessionManager sessionManager;
    private CurrencyConverter currencyConverter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);

        if (!sessionManager.isLoggedIn()) {
            navigateToLogin();
            return;
        }

        setContentView(R.layout.activity_main);

        dbHelper = DatabaseHelper.getInstance(this);
        currencyConverter = new CurrencyConverter(this);

        initializeViews();
        setupBottomNavigation();
        setupRecurringWorker();
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

        tvIncomeAmount = findViewById(R.id.tv_income_amount);
        tvExpenseAmount = findViewById(R.id.tv_expense_amount);
        tvBalanceAmount = findViewById(R.id.tv_balance_amount);

        btnAddExpense = findViewById(R.id.btn_add_expense);
        btnViewBudget = findViewById(R.id.btn_view_budget);
        btnGenerateReport = findViewById(R.id.btn_generate_report);

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

    private void setupRecurringWorker() {
        PeriodicWorkRequest recurringWork = new PeriodicWorkRequest.Builder(
                RecurringExpenseWorker.class,
                1, TimeUnit.DAYS
        ).build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "recurring_expenses",
                ExistingPeriodicWorkPolicy.KEEP,
                recurringWork
        );
    }

    private void showDashboard() {
        layoutDashboard.setVisibility(View.VISIBLE);
        loadDashboardData();
    }

    /**
     * Load dashboard data with PROPER currency conversion to VND
     */
    private void loadDashboardData() {
        try {
            int userId = sessionManager.getUserId();

            String userName = sessionManager.getUserName();
            if (userName == null || userName.isEmpty()) {
                userName = "User";
            }
            tvGreeting.setText("Hello, " + userName + "! 👋");

            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.DAY_OF_MONTH, 1);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            long monthStart = calendar.getTimeInMillis();

            calendar.add(Calendar.MONTH, 1);
            long monthEnd = calendar.getTimeInMillis();

            // Calculate Income, Expense, Balance (ALL IN VND)
            double totalIncomeVnd = 0;
            double totalExpenseVnd = 0;

            List<Expense> expenses = dbHelper.getExpensesByUser(userId);

            for (Expense expense : expenses) {
                if (expense.getDate() >= monthStart && expense.getDate() < monthEnd) {
                    // Convert to VND using currency converter
                    double amountInVnd = currencyConverter.convert(
                            expense.getAmount(),
                            expense.getCurrencyId(),
                            1 // VND currency ID
                    );

                    if (expense.isIncome()) {
                        totalIncomeVnd += amountInVnd;
                    } else {
                        totalExpenseVnd += amountInVnd;
                    }
                }
            }

            double balance = totalIncomeVnd - totalExpenseVnd;

            // Format and display IN VND (currency ID = 1)
            String formattedIncome = currencyConverter.format(totalIncomeVnd, 1);
            String formattedExpense = currencyConverter.format(totalExpenseVnd, 1);
            String formattedBalance = currencyConverter.format(Math.abs(balance), 1);

            tvIncomeAmount.setText("+" + formattedIncome);
            tvExpenseAmount.setText("-" + formattedExpense);
            tvBalanceAmount.setText((balance >= 0 ? "+" : "-") + formattedBalance);

            // Color coding for balance
            if (balance >= 0) {
                tvBalanceAmount.setTextColor(getResources().getColor(R.color.success));
            } else {
                tvBalanceAmount.setTextColor(getResources().getColor(R.color.error));
            }

            // Top category (only expenses)
            java.util.Map<Integer, Double> categoryTotals = new java.util.HashMap<>();

            for (Expense expense : expenses) {
                if (expense.getDate() >= monthStart && expense.getDate() < monthEnd) {
                    if (expense.isExpense()) {
                        int categoryId = expense.getCategoryId();
                        double amountInVnd = currencyConverter.convert(
                                expense.getAmount(),
                                expense.getCurrencyId(),
                                1
                        );
                        categoryTotals.put(categoryId,
                                categoryTotals.getOrDefault(categoryId, 0.0) + amountInVnd);
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

            String formattedTopAmount = currencyConverter.format(topAmount, 1);
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