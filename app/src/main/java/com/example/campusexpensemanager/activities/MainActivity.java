package com.example.campusexpensemanager.activities;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.example.campusexpensemanager.R;
import com.example.campusexpensemanager.adapters.BudgetPreviewAdapter;
import com.example.campusexpensemanager.models.Budget;
import com.example.campusexpensemanager.models.Category;
import com.example.campusexpensemanager.utils.CurrencyConverter;
import com.example.campusexpensemanager.utils.DatabaseHelper;
import com.example.campusexpensemanager.utils.LocaleHelper;
import com.example.campusexpensemanager.utils.SessionManager;
import com.example.campusexpensemanager.workers.RecurringExpenseWorker;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * MainActivity - REDESIGNED with Minimalist UI + Animations
 * ✅ Features:
 * - Number rolling animation for balance
 * - Eye toggle for privacy
 * - Bar chart (Income vs Expense)
 * - Pie chart (Expense by Category)
 * - Budget preview (2-3 items)
 * - Expandable FAB menu
 * - Staggered entrance animation
 */
public class MainActivity extends BaseActivity {

    private static final String TAG = "MainActivity";

    // ===== UI Components =====
    private LinearLayout layoutDashboard;
    private TextView tvIncomeAmount, tvExpenseAmount, tvBalanceAmount;
    private TextView tvGreeting, tvTopCategory, tvEmptyBudgets;
    private ImageButton btnToggleBalanceVisibility;

    // ✅ NEW: Charts
    private BarChart barChartIncomeExpense;
    private PieChart pieChartCategory;

    // ✅ NEW: Budget Preview
    private RecyclerView rvBudgetPreview;
    private BudgetPreviewAdapter budgetPreviewAdapter;

    // ✅ NEW: FAB Menu
    private FloatingActionButton fabMain, fabAddExpense, fabSetBudget;
    private View dimOverlay;
    private boolean isFabMenuOpen = false;

    // ===== Data & State =====
    private DatabaseHelper dbHelper;
    private SessionManager sessionManager;
    private CurrencyConverter currencyConverter;
    private String currentLanguageCode;
    private boolean isBalanceVisible = true;
    private double currentBalance = 0; // For eye toggle

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
        setupBalanceToggle();
        setupFabMenu();
        showDashboard();
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

        // ✅ NEW: Eye toggle button
        btnToggleBalanceVisibility = findViewById(R.id.btn_toggle_balance_visibility);

        // ✅ NEW: Charts
        barChartIncomeExpense = findViewById(R.id.bar_chart_income_expense);
        pieChartCategory = findViewById(R.id.pie_chart_category);

        // ✅ NEW: Budget Preview RecyclerView
        rvBudgetPreview = findViewById(R.id.rv_budget_preview);
        tvEmptyBudgets = findViewById(R.id.tv_empty_budgets);
        rvBudgetPreview.setLayoutManager(new LinearLayoutManager(this));
        rvBudgetPreview.setNestedScrollingEnabled(false);

        // ✅ NEW: FAB Menu components
        fabMain = findViewById(R.id.fab_main);
        fabAddExpense = findViewById(R.id.fab_add_expense);
        fabSetBudget = findViewById(R.id.fab_set_budget);
        dimOverlay = findViewById(R.id.dim_overlay);
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

    // ========== ✅ NEW: EYE TOGGLE FUNCTIONALITY ==========
    private void setupBalanceToggle() {
        btnToggleBalanceVisibility.setOnClickListener(v -> {
            Animation fadeOut = AnimationUtils.loadAnimation(this, R.anim.fade_out);
            Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);

            tvBalanceAmount.startAnimation(fadeOut);

            fadeOut.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {}

                @Override
                public void onAnimationEnd(Animation animation) {
                    if (isBalanceVisible) {
                        // Hide balance
                        tvBalanceAmount.setText("*****đ");
                        btnToggleBalanceVisibility.setImageResource(R.drawable.ic_eye_off);
                        isBalanceVisible = false;
                    } else {
                        // Show balance
                        String formattedBalance = currencyConverter.format(Math.abs(currentBalance), 1);
                        tvBalanceAmount.setText((currentBalance >= 0 ? "+" : "-") + formattedBalance);
                        btnToggleBalanceVisibility.setImageResource(R.drawable.ic_eye);
                        isBalanceVisible = true;
                    }
                    tvBalanceAmount.startAnimation(fadeIn);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {}
            });
        });
    }

    // ========== ✅ NEW: FAB EXPANDABLE MENU ==========
    private void setupFabMenu() {
        fabMain.setOnClickListener(v -> {
            if (isFabMenuOpen) {
                closeFabMenu();
            } else {
                openFabMenu();
            }
        });

        dimOverlay.setOnClickListener(v -> closeFabMenu());

        // Sub FAB actions
        fabAddExpense.setOnClickListener(v -> {
            closeFabMenu();
            Intent intent = new Intent(MainActivity.this, AddExpenseActivity.class);
            startActivity(intent);
        });

        fabSetBudget.setOnClickListener(v -> {
            closeFabMenu();
            Intent intent = new Intent(MainActivity.this, BudgetDashboardActivity.class);
            startActivity(intent);
        });
    }

    private void openFabMenu() {
        isFabMenuOpen = true;

        // Show dim overlay with fade in
        dimOverlay.setVisibility(View.VISIBLE);
        dimOverlay.animate()
                .alpha(1f)
                .setDuration(200)
                .start();

        // Rotate main FAB icon
        fabMain.animate()
                .rotation(45f)
                .setDuration(200)
                .start();

        // Show and animate sub FABs
        fabAddExpense.setVisibility(View.VISIBLE);
        fabSetBudget.setVisibility(View.VISIBLE);

        fabAddExpense.setAlpha(0f);
        fabAddExpense.setTranslationY(100f);
        fabAddExpense.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(300)
                .setInterpolator(new OvershootInterpolator(1.5f))
                .setStartDelay(50)
                .start();

        fabSetBudget.setAlpha(0f);
        fabSetBudget.setTranslationY(100f);
        fabSetBudget.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(300)
                .setInterpolator(new OvershootInterpolator(1.5f))
                .setStartDelay(100)
                .start();
    }

    private void closeFabMenu() {
        isFabMenuOpen = false;

        // Hide dim overlay
        dimOverlay.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction(() -> dimOverlay.setVisibility(View.GONE))
                .start();

        // Rotate main FAB back
        fabMain.animate()
                .rotation(0f)
                .setDuration(200)
                .start();

        // Hide sub FABs
        fabAddExpense.animate()
                .alpha(0f)
                .translationY(100f)
                .setDuration(200)
                .withEndAction(() -> fabAddExpense.setVisibility(View.GONE))
                .start();

        fabSetBudget.animate()
                .alpha(0f)
                .translationY(100f)
                .setDuration(200)
                .withEndAction(() -> fabSetBudget.setVisibility(View.GONE))
                .start();
    }

    private void showDashboard() {
        layoutDashboard.setVisibility(View.VISIBLE);
        loadDashboardData();
        runLayoutAnimation();
    }

    private void loadDashboardData() {
        try {
            int userId = sessionManager.getUserId();

            // Display greeting
            String userName = sessionManager.getUserName();
            if (userName == null || userName.isEmpty()) {
                userName = getString(R.string.auth_name);
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
            currentBalance = balance; // Store for eye toggle

            // Format amounts
            String formattedIncome = currencyConverter.format(totalIncomeVnd, 1);
            String formattedExpense = currencyConverter.format(totalExpenseVnd, 1);

            tvIncomeAmount.setText("+" + formattedIncome);
            tvExpenseAmount.setText("-" + formattedExpense);

            // ✅ ANIMATION: Number rolling for balance
            animateNumberRoll(tvBalanceAmount, 0, balance, 1500);

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

            // Get top category
            Map<Integer, Double> topCategoryMap = dashboardData.topCategoryMap;
            String topCategoryName = getString(R.string.cat_others);
            double topAmount = 0;

            if (!topCategoryMap.isEmpty()) {
                Map.Entry<Integer, Double> entry = topCategoryMap.entrySet().iterator().next();
                topAmount = entry.getValue();

                Category cat = dbHelper.getCategoryById(entry.getKey());
                if (cat != null) {
                    topCategoryName = DatabaseHelper.getLocalizedCategoryName(this, cat.getName());
                }
            }

            String formattedTopAmount = currencyConverter.format(topAmount, 1);
            String topCategoryLabel = getString(R.string.dashboard_top_category);
            tvTopCategory.setText(topCategoryLabel + ": " + topCategoryName + " (" + formattedTopAmount + ")");

            // ✅ NEW: Setup Charts
            setupBarChart(totalIncomeVnd, totalExpenseVnd);
            setupPieChart(topCategoryMap);

            // ✅ NEW: Load Budget Preview
            loadBudgetPreview();

        } catch (Exception e) {
            e.printStackTrace();
            // Fallback values
            tvGreeting.setText(getString(R.string.dashboard_greeting, "User"));
            tvIncomeAmount.setText("+0₫");
            tvExpenseAmount.setText("-0₫");
            tvBalanceAmount.setText("0₫");
            tvTopCategory.setText(getString(R.string.dashboard_top_category) + ": " + getString(R.string.cat_others));
        }
    }

    // ========== ✅ NEW: NUMBER ROLLING ANIMATION ==========
    private void animateNumberRoll(TextView textView, double start, double end, int duration) {
        ValueAnimator animator = ValueAnimator.ofFloat((float) start, (float) end);
        animator.setDuration(duration);
        animator.setInterpolator(new OvershootInterpolator(0.5f));

        animator.addUpdateListener(animation -> {
            double value = (float) animation.getAnimatedValue();
            String formatted = currencyConverter.format(Math.abs(value), 1);
            textView.setText((value >= 0 ? "+" : "-") + formatted);
        });

        animator.start();
    }

    // ========== ✅ NEW: BAR CHART (Income vs Expense) ==========
    private void setupBarChart(double income, double expense) {
        List<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(0f, (float) income));  // Income
        entries.add(new BarEntry(1f, (float) expense)); // Expense

        BarDataSet dataSet = new BarDataSet(entries, "");
        dataSet.setColors(
                ContextCompat.getColor(this, R.color.success),
                ContextCompat.getColor(this, R.color.error)
        );
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(ContextCompat.getColor(this, R.color.light_on_surface));

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.5f);
        barChartIncomeExpense.setData(barData);

        // Chart styling
        barChartIncomeExpense.getXAxis().setEnabled(false);
        barChartIncomeExpense.getAxisLeft().setEnabled(false);
        barChartIncomeExpense.getAxisRight().setEnabled(false);
        barChartIncomeExpense.getLegend().setEnabled(false);
        barChartIncomeExpense.setDrawGridBackground(false);
        barChartIncomeExpense.setTouchEnabled(false);

        Description desc = new Description();
        desc.setText("");
        barChartIncomeExpense.setDescription(desc);

        // ✅ ANIMATION: Chart growth
        barChartIncomeExpense.animateY(1000);
        barChartIncomeExpense.invalidate();
    }

    // ========== ✅ NEW: PIE CHART (Expense by Category) ==========
    private void setupPieChart(Map<Integer, Double> categoryTotals) {
        if (categoryTotals.isEmpty()) {
            pieChartCategory.setNoDataText(getString(R.string.msg_no_expenses_month));
            pieChartCategory.invalidate();
            return;
        }

        List<PieEntry> pieEntries = new ArrayList<>();
        int[] colors = getSeraUIColors();

        for (Map.Entry<Integer, Double> entry : categoryTotals.entrySet()) {
            Category category = dbHelper.getCategoryById(entry.getKey());

            String categoryName;
            if (category != null) {
                categoryName = DatabaseHelper.getLocalizedCategoryName(this, category.getName());
            } else {
                categoryName = getString(R.string.cat_unknown);
            }

            float value = entry.getValue().floatValue();
            pieEntries.add(new PieEntry(value, categoryName));
        }

        PieDataSet dataSet = new PieDataSet(pieEntries, "");
        dataSet.setColors(colors);
        dataSet.setValueTextSize(11f);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setSliceSpace(2f);
        dataSet.setValueFormatter(new PercentFormatter(pieChartCategory));

        PieData pieData = new PieData(dataSet);
        pieChartCategory.setData(pieData);

        // Chart styling
        pieChartCategory.setUsePercentValues(true);
        pieChartCategory.setDrawHoleEnabled(true);
        pieChartCategory.setHoleColor(Color.TRANSPARENT);
        pieChartCategory.setHoleRadius(35f);
        pieChartCategory.setTransparentCircleRadius(40f);
        pieChartCategory.setDrawCenterText(false);
        pieChartCategory.setRotationEnabled(true);
        pieChartCategory.setHighlightPerTapEnabled(true);

        Description description = new Description();
        description.setText("");
        pieChartCategory.setDescription(description);

        Legend legend = pieChartCategory.getLegend();
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setDrawInside(false);
        legend.setWordWrapEnabled(true);
        legend.setTextSize(9f);

        // ✅ ANIMATION: Chart growth
        pieChartCategory.animateY(1000);
        pieChartCategory.invalidate();
    }

    // ========== ✅ NEW: COLOR PALETTE ==========
    private int[] getSeraUIColors() {
        return new int[]{
                ContextCompat.getColor(this, R.color.primary_blue),
                ContextCompat.getColor(this, R.color.secondary_teal),
                ContextCompat.getColor(this, R.color.accent_orange),
                ContextCompat.getColor(this, R.color.accent_green),
                ContextCompat.getColor(this, R.color.accent_red),
                ContextCompat.getColor(this, R.color.accent_yellow),
                ContextCompat.getColor(this, R.color.primary_blue_light),
                ContextCompat.getColor(this, R.color.secondary_teal_light),
        };
    }

    // ========== ✅ NEW: BUDGET PREVIEW (2-3 items) ==========
    private void loadBudgetPreview() {
        int userId = sessionManager.getUserId();
        List<Budget> allBudgets = dbHelper.getBudgetsByUser(userId);

        if (allBudgets.isEmpty()) {
            rvBudgetPreview.setVisibility(View.GONE);
            tvEmptyBudgets.setVisibility(View.VISIBLE);
            return;
        }

        // Show only 2-3 budgets
        List<Budget> previewBudgets = allBudgets.size() > 3
                ? allBudgets.subList(0, 3)
                : allBudgets;

        budgetPreviewAdapter = new BudgetPreviewAdapter(this, previewBudgets, dbHelper);
        rvBudgetPreview.setAdapter(budgetPreviewAdapter);
        rvBudgetPreview.setVisibility(View.VISIBLE);
        tvEmptyBudgets.setVisibility(View.GONE);

        // Click to view full budget dashboard
        budgetPreviewAdapter.setOnItemClickListener(budget -> {
            Intent intent = new Intent(MainActivity.this, BudgetDashboardActivity.class);
            startActivity(intent);
        });
    }

    // ========== STAGGERED ENTRANCE ANIMATION ==========
    private void runLayoutAnimation() {
        View[] views = {
                tvGreeting,
                findViewById(R.id.card_balance),
                findViewById(R.id.card_income),
                findViewById(R.id.card_expense),
                (View) barChartIncomeExpense.getRootView().findViewById(R.id.bar_chart_income_expense).getParent(),
                (View) pieChartCategory.getRootView().findViewById(R.id.pie_chart_category).getParent(),
                (View) tvTopCategory.getParent(),
                rvBudgetPreview
        };

        for (int i = 0; i < views.length; i++) {
            View view = views[i];
            if (view == null) continue;

            view.setTranslationY(100f);
            view.setAlpha(0f);

            view.animate()
                    .translationY(0f)
                    .alpha(1f)
                    .setDuration(500)
                    .setStartDelay(i * 100)
                    .setInterpolator(new OvershootInterpolator(1.0f))
                    .start();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (layoutDashboard != null && layoutDashboard.getVisibility() == View.VISIBLE) {
            loadDashboardData();
        }
    }

    @Override
    public void onBackPressed() {
        if (isFabMenuOpen) {
            closeFabMenu();
        } else {
            super.onBackPressed();
        }
    }
}