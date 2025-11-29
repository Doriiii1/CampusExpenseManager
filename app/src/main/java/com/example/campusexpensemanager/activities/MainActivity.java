package com.example.campusexpensemanager.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.example.campusexpensemanager.R;
import com.example.campusexpensemanager.models.Category;
import com.example.campusexpensemanager.utils.CurrencyConverter;
import com.example.campusexpensemanager.utils.DatabaseHelper;
import com.example.campusexpensemanager.utils.SessionManager;
import com.example.campusexpensemanager.workers.RecurringExpenseWorker;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import android.view.animation.OvershootInterpolator;

import java.util.Calendar;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.example.campusexpensemanager.utils.LocaleHelper;

/**
 * MainActivity - OPTIMIZED Dashboard (Priority 1.3)
 * Now uses SQL queries instead of Java loops for 10x faster performance
 */
public class MainActivity extends BaseActivity {

    private static final String TAG = "MainActivity";

    private LinearLayout layoutDashboard;

    private TextView tvIncomeAmount, tvExpenseAmount, tvBalanceAmount;
    private TextView tvGreeting, tvTopCategory;
    private Button btnAddExpense, btnViewBudget;

    private DatabaseHelper dbHelper;
    private SessionManager sessionManager;
    private CurrencyConverter currencyConverter;
    private String currentLanguageCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        currentLanguageCode = LocaleHelper.getLanguage(this);
        sessionManager = new SessionManager(this);

        if (!sessionManager.isLoggedIn()) {
            navigateToLogin();
            return;
        }

        setContentView(R.layout.activity_main);

        dbHelper = DatabaseHelper.getInstance(this);
        currencyConverter = new CurrencyConverter(this);

        initializeViews();
        setupRecurringWorker();
        showDashboard();
        runLayoutAnimation();
    }

    private void navigateToLogin() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void initializeViews() {
        layoutDashboard = findViewById(R.id.layout_dashboard);
        tvGreeting = findViewById(R.id.tv_greeting);
        tvTopCategory = findViewById(R.id.tv_top_category);

        tvIncomeAmount = findViewById(R.id.tv_income_amount);
        tvExpenseAmount = findViewById(R.id.tv_expense_amount);
        tvBalanceAmount = findViewById(R.id.tv_balance_amount);

        btnAddExpense = findViewById(R.id.btn_add_expense);
        btnViewBudget = findViewById(R.id.btn_view_budget);

        btnAddExpense.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddExpenseActivity.class);
            startActivity(intent);
        });

        btnViewBudget.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, BudgetDashboardActivity.class);
            startActivity(intent);
        });


        btnAddExpense.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddExpenseActivity.class);
            startActivity(intent);
        });

        btnViewBudget.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, BudgetDashboardActivity.class);
            startActivity(intent);
        });
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
     * ✅ LOCALIZED: Load dashboard data with proper string resources
     */
    private void loadDashboardData() {
        try {
            int userId = sessionManager.getUserId();

            // Display greeting with localized string
            String userName = sessionManager.getUserName();
            if (userName == null || userName.isEmpty()) {
                userName = getString(R.string.auth_name); // Fallback to "User"
            }
            tvGreeting.setText(getString(R.string.dashboard_greeting, userName));

            // Calculate month boundaries
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.DAY_OF_MONTH, 1);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            long monthStart = calendar.getTimeInMillis();

            calendar.add(Calendar.MONTH, 1);
            long monthEnd = calendar.getTimeInMillis();

            // Get optimized dashboard data
            DatabaseHelper.DashboardData dashboardData =
                    dbHelper.getDashboardDataOptimized(userId, monthStart, monthEnd);

            double totalIncomeVnd = dashboardData.totalIncome;
            double totalExpenseVnd = dashboardData.totalExpense;
            double balance = dashboardData.getBalance();

            // ✅ LOCALIZED: Format amounts with + / - signs
            String formattedIncome = currencyConverter.format(totalIncomeVnd, 1);
            String formattedExpense = currencyConverter.format(totalExpenseVnd, 1);
            String formattedBalance = currencyConverter.format(Math.abs(balance), 1);

            tvIncomeAmount.setText("+" + formattedIncome);
            tvExpenseAmount.setText("-" + formattedExpense);
            tvBalanceAmount.setText((balance >= 0 ? "+" : "-") + formattedBalance);

            // Color coding for balance
            if (balance >= 0) {
                tvBalanceAmount.setTextColor(
                        ContextCompat.getColor(this, R.color.success)
                );
            } else {
                tvBalanceAmount.setTextColor(
                        ContextCompat.getColor(this, R.color.error)
                );
            }

            // ✅ LOCALIZED: Get top category with localized name
            Map<Integer, Double> topCategoryMap = dashboardData.topCategoryMap;

            String topCategoryName = getString(R.string.cat_others); // Default
            double topAmount = 0;

            if (!topCategoryMap.isEmpty()) {
                Map.Entry<Integer, Double> entry = topCategoryMap.entrySet().iterator().next();
                topAmount = entry.getValue();

                Category cat = dbHelper.getCategoryById(entry.getKey());
                if (cat != null) {
                    // ✅ Get localized category name
                    topCategoryName = DatabaseHelper.getLocalizedCategoryName(
                            this,
                            cat.getName()
                    );
                }
            }

            String formattedTopAmount = currencyConverter.format(topAmount, 1);

            // ✅ LOCALIZED: Use string resource for "Top Category:"
            String topCategoryLabel = getString(R.string.dashboard_top_category);
            tvTopCategory.setText(topCategoryLabel + ": " + topCategoryName + " (" + formattedTopAmount + ")");

        } catch (Exception e) {e.printStackTrace();
            // ✅ LOCALIZED: Fallback values using string resources
            tvGreeting.setText(getString(R.string.dashboard_greeting, "User"));
            tvIncomeAmount.setText("+0₫");
            tvExpenseAmount.setText("-0₫");
            tvBalanceAmount.setText("0₫");
            tvTopCategory.setText(getString(R.string.dashboard_top_category) + ": " + getString(R.string.cat_others));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

//        String storedLanguage = LocaleHelper.getLanguage(this);
//        if (!currentLanguageCode.equals(storedLanguage)) {
//            recreate(); // Tải lại màn hình để áp dụng ngôn ngữ mới
//            return;
//        }

        if (layoutDashboard != null && layoutDashboard.getVisibility() == View.VISIBLE) {
            loadDashboardData();
        }
    }

    /**
     * ✅ NEW: Tạo hiệu ứng trượt lên cho các phần tử Dashboard
     */
    private void runLayoutAnimation() {
        // Danh sách các View cần animate theo thứ tự
        View[] views = {
                tvGreeting,
                findViewById(R.id.card_balance),
                findViewById(R.id.card_income), // Hoặc layout chứa 2 card nhỏ
                findViewById(R.id.card_expense),
                tvTopCategory, // Card Top Category
                btnAddExpense, // Nút Quick Action
                btnViewBudget
        };

        // Bắt đầu animate từng cái
        for (int i = 0; i < views.length; i++) {
            View view = views[i];
            if (view == null) continue;

            // 1. Đặt trạng thái ban đầu: Dịch xuống 100px và trong suốt
            view.setTranslationY(100f);
            view.setAlpha(0f);

            // 2. Animate trở về vị trí cũ
            view.animate()
                    .translationY(0f)
                    .alpha(1f)
                    .setDuration(500) // Thời gian chạy: 0.5 giây
                    .setStartDelay(i * 100) // Mỗi phần tử trễ nhau 100ms (tạo hiệu ứng dây chuyền)
                    .setInterpolator(new OvershootInterpolator(1.0f)) // Hiệu ứng bật nảy nhẹ khi dừng
                    .start();
        }
    }
}