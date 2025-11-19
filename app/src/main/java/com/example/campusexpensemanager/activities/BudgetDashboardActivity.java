package com.example.campusexpensemanager.activities;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.example.campusexpensemanager.R;
import com.example.campusexpensemanager.models.Budget;
import com.example.campusexpensemanager.models.Category;
import com.example.campusexpensemanager.models.Expense;
import com.example.campusexpensemanager.utils.DatabaseHelper;
import com.example.campusexpensemanager.utils.SessionManager;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * BudgetDashboardActivity shows budget progress with predictions and alerts
 */
public class BudgetDashboardActivity extends BaseActivity {

    private static final String CHANNEL_ID = "budget_alerts";
    private static final int NOTIFICATION_ID = 1001;

    private LinearLayout budgetContainer;
    private TextView tvEmptyState;
    private FloatingActionButton fabAddBudget;

    private DatabaseHelper dbHelper;
    private SessionManager sessionManager;

    private NumberFormat currencyFormat;
    private SimpleDateFormat dateFormat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_budget_dashboard);

        // Initialize helpers
        dbHelper = DatabaseHelper.getInstance(this);
        sessionManager = new SessionManager(this);

        // Check authentication
        if (!sessionManager.isLoggedIn()) {
            finish();
            return;
        }

        // Initialize formatters
        currencyFormat = NumberFormat.getInstance(new Locale("vi", "VN"));
        dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

        // Initialize views
        initializeViews();

        // Create notification channel
        createNotificationChannel();

        // Load budgets
        loadBudgets();

        // Setup click listeners
        setupClickListeners();
    }

    private void initializeViews() {
        budgetContainer = findViewById(R.id.budget_container);
        tvEmptyState = findViewById(R.id.tv_empty_budgets);
        fabAddBudget = findViewById(R.id.fab_add_budget);
    }

    private void setupClickListeners() {
        fabAddBudget.setOnClickListener(v -> {
            Intent intent = new Intent(BudgetDashboardActivity.this, SetBudgetActivity.class);
            startActivity(intent);
        });
    }

    /**
     * Load and display budgets with progress
     */
    private void loadBudgets() {
        int userId = sessionManager.getUserId();
        List<Budget> budgets = dbHelper.getBudgetsByUser(userId);

        // Clear existing views
        budgetContainer.removeAllViews();

        if (budgets.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            budgetContainer.setVisibility(View.GONE);
        } else {
            tvEmptyState.setVisibility(View.GONE);
            budgetContainer.setVisibility(View.VISIBLE);

            // Create card for each budget
            for (Budget budget : budgets) {
                View budgetCard = createBudgetCard(budget);

                // Add click listener to navigate to EditBudgetActivity
                budgetCard.setOnClickListener(v -> {
                    Intent intent = new Intent(BudgetDashboardActivity.this, EditBudgetActivity.class);
                    intent.putExtra("budget_id", budget.getId());
                    startActivity(intent);
                });

                budgetContainer.addView(budgetCard);
            }
        }
    }

    /**
     * Create budget card with progress and prediction
     */
    private View createBudgetCard(Budget budget) {
        View cardView = getLayoutInflater().inflate(R.layout.item_budget_dashboard, budgetContainer, false);

        // Get views
        TextView tvCategoryName = cardView.findViewById(R.id.tv_budget_category);
        TextView tvBudgetAmount = cardView.findViewById(R.id.tv_budget_amount);
        TextView tvSpentAmount = cardView.findViewById(R.id.tv_spent_amount);
        TextView tvRemainingAmount = cardView.findViewById(R.id.tv_remaining_amount);
        TextView tvPeriod = cardView.findViewById(R.id.tv_budget_period);
        TextView tvPrediction = cardView.findViewById(R.id.tv_prediction);
        ProgressBar progressBar = cardView.findViewById(R.id.progress_budget);

        // Get category name
        String categoryName = "Total Budget";
        if (budget.getCategoryId() > 0) {
            Category category = dbHelper.getCategoryById(budget.getCategoryId());
            if (category != null) {
                categoryName = category.getName();
            }
        }
        tvCategoryName.setText(categoryName);

        // Calculate spent amount
        double spent = calculateSpent(budget);
        double remaining = budget.getAmount() - spent;
        double percentageSpent = budget.calculatePercentageSpent(spent);

        // Format amounts
        String budgetAmount = currencyFormat.format(budget.getAmount()) + "đ";
        String spentAmount = currencyFormat.format(spent) + "đ";
        String remainingAmount = currencyFormat.format(remaining) + "đ";

        tvBudgetAmount.setText("Budget: " + budgetAmount);
        tvSpentAmount.setText("Spent: " + spentAmount + " (" + String.format("%.1f%%", percentageSpent) + ")");
        tvRemainingAmount.setText("Remaining: " + remainingAmount);

        // Set progress bar
        progressBar.setProgress((int) percentageSpent);

        // Set progress bar color
        int progressColor;
        if (percentageSpent < 50) {
            progressColor = ContextCompat.getColor(this, R.color.budget_safe);
        } else if (percentageSpent < 80) {
            progressColor = ContextCompat.getColor(this, R.color.budget_warning);
        } else {
            progressColor = ContextCompat.getColor(this, R.color.budget_danger);
        }
        progressBar.setProgressTintList(android.content.res.ColorStateList.valueOf(progressColor));

        // Period dates
        String periodStart = dateFormat.format(new Date(budget.getPeriodStart()));
        String periodEnd = dateFormat.format(new Date(budget.getPeriodEnd()));
        tvPeriod.setText("Period: " + periodStart + " - " + periodEnd);

        // Calculate prediction
        String prediction = calculatePrediction(budget, spent);
        if (!prediction.isEmpty()) {
            tvPrediction.setVisibility(View.VISIBLE);
            tvPrediction.setText(prediction);
        } else {
            tvPrediction.setVisibility(View.GONE);
        }

        // Check for low budget alert
        if (percentageSpent > 80) {
            sendBudgetAlert(categoryName, remaining);
        }

        return cardView;
    }

    /**
     * Calculate spent amount for budget
     */
    private double calculateSpent(Budget budget) {
        List<Expense> allExpenses = dbHelper.getExpensesByUser(budget.getUserId());

        double total = 0;
        for (Expense expense : allExpenses) {
            if (expense.getDate() >= budget.getPeriodStart() &&
                    expense.getDate() <= budget.getPeriodEnd()) {

                if (budget.getCategoryId() == 0 ||
                        expense.getCategoryId() == budget.getCategoryId()) {
                    total += expense.getAmount();
                }
            }
        }

        return total;
    }

    /**
     * Calculate simple rule-based prediction
     */
    private String calculatePrediction(Budget budget, double spent) {
        long currentTime = System.currentTimeMillis();

        // Check if period has started
        if (currentTime < budget.getPeriodStart()) {
            return "";
        }

        // Calculate days elapsed and remaining
        long periodDuration = budget.getPeriodEnd() - budget.getPeriodStart();
        long timeElapsed = currentTime - budget.getPeriodStart();
        long timeRemaining = budget.getPeriodEnd() - currentTime;

        if (timeRemaining <= 0) {
            return "Budget period ended";
        }

        int daysElapsed = (int) (timeElapsed / (1000 * 60 * 60 * 24));
        int daysRemaining = (int) (timeRemaining / (1000 * 60 * 60 * 24));

        if (daysElapsed == 0) {
            return "";
        }

        // Calculate daily average
        double dailyAverage = spent / daysElapsed;

        // Predict total spending
        double predictedTotal = spent + (dailyAverage * daysRemaining);
        double predictedExcess = predictedTotal - budget.getAmount();

        if (predictedExcess > 0) {
            String excessAmount = currencyFormat.format(predictedExcess) + "đ";
            return "⚠️ Prediction: May exceed budget by " + excessAmount + " if spending continues";
        } else {
            return "✅ On track to stay within budget";
        }
    }

    /**
     * Send local notification for low budget
     */
    private void sendBudgetAlert(String categoryName, double remaining) {
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        String title = "Budget Alert: " + categoryName;
        String message = "Only " + currencyFormat.format(remaining) + "đ remaining!";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_wallet)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID, builder.build());
        }
    }

    /**
     * Create notification channel for Android 8.0+
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Budget Alerts";
            String description = "Notifications for low budget warnings";
            int importance = NotificationManager.IMPORTANCE_HIGH;

            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadBudgets();
    }
}