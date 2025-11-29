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

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.example.campusexpensemanager.R;
import com.example.campusexpensemanager.models.Budget;
import com.example.campusexpensemanager.models.Category;
import com.example.campusexpensemanager.models.Expense;
import com.example.campusexpensemanager.utils.DatabaseHelper;
import com.example.campusexpensemanager.utils.SessionManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * BudgetDashboardActivity - Đã tối ưu hóa đa ngôn ngữ
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

        dbHelper = DatabaseHelper.getInstance(this);
        sessionManager = new SessionManager(this);

        if (!sessionManager.isLoggedIn()) {
            finish();
            return;
        }

        // ✅ FIX: Sử dụng Locale mặc định của máy để format tiền tệ và ngày tháng
        currencyFormat = NumberFormat.getInstance(new Locale("vi", "VN"));
        dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

        initializeViews();
        createNotificationChannel();
        loadBudgets();
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

    private void loadBudgets() {
        int userId = sessionManager.getUserId();
        List<Budget> budgets = dbHelper.getBudgetsByUser(userId);

        budgetContainer.removeAllViews();

        if (budgets.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            budgetContainer.setVisibility(View.GONE);
        } else {
            tvEmptyState.setVisibility(View.GONE);
            budgetContainer.setVisibility(View.VISIBLE);

            for (Budget budget : budgets) {
                View budgetCard = createBudgetCard(budget);

                // Click để chỉnh sửa
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
     * ✅ FIX: Tạo thẻ ngân sách với Text đã được dịch (Localized)
     */
    private View createBudgetCard(Budget budget) {
        View cardView = getLayoutInflater().inflate(R.layout.item_budget_dashboard, budgetContainer, false);

        TextView tvCategoryName = cardView.findViewById(R.id.tv_budget_category);
        TextView tvBudgetAmount = cardView.findViewById(R.id.tv_budget_amount);
        TextView tvSpentAmount = cardView.findViewById(R.id.tv_spent_amount);
        TextView tvRemainingAmount = cardView.findViewById(R.id.tv_remaining_amount);
        TextView tvPeriod = cardView.findViewById(R.id.tv_budget_period);
        TextView tvPrediction = cardView.findViewById(R.id.tv_prediction);
        ProgressBar progressBar = cardView.findViewById(R.id.progress_budget);

        // ✅ FIX: Lấy tên danh mục đã dịch
        String categoryName;
        if (budget.getCategoryId() > 0) {
            Category category = dbHelper.getCategoryById(budget.getCategoryId());
            if (category != null) {
                categoryName = DatabaseHelper.getLocalizedCategoryName(this, category.getName());
            } else {
                categoryName = getString(R.string.cat_unknown);
            }
        } else {
            categoryName = getString(R.string.label_total_budget); // "Tổng ngân sách"
        }
        tvCategoryName.setText(categoryName);

        // Tính toán
        double spent = calculateSpent(budget);
        double remaining = budget.getAmount() - spent;
        double percentageSpent = budget.calculatePercentageSpent(spent);

        // Format số tiền
        String budgetAmount = currencyFormat.format(budget.getAmount()) + "đ";
        String spentAmount = currencyFormat.format(spent) + "đ";
        String remainingAmount = currencyFormat.format(remaining) + "đ";

        // ✅ FIX: Sử dụng getString() với prefix
        tvBudgetAmount.setText(getString(R.string.label_budget_prefix) + budgetAmount);
        tvSpentAmount.setText(getString(R.string.label_spent_prefix) + spentAmount + " (" + String.format("%.1f%%", percentageSpent) + ")");
        tvRemainingAmount.setText(getString(R.string.label_remaining_prefix) + remainingAmount);

        // Progress Bar
        progressBar.setProgress((int) percentageSpent);
        int progressColor;
        if (percentageSpent < 50) {
            progressColor = ContextCompat.getColor(this, R.color.budget_safe);
        } else if (percentageSpent < 80) {
            progressColor = ContextCompat.getColor(this, R.color.budget_warning);
        } else {
            progressColor = ContextCompat.getColor(this, R.color.budget_danger);
        }
        progressBar.setProgressTintList(android.content.res.ColorStateList.valueOf(progressColor));

        // ✅ FIX: Format ngày tháng
        String periodStart = dateFormat.format(new Date(budget.getPeriodStart()));
        String periodEnd = dateFormat.format(new Date(budget.getPeriodEnd()));
        tvPeriod.setText(getString(R.string.label_period_prefix) + periodStart + " - " + periodEnd);

        // Dự báo (Prediction)
        String prediction = calculatePrediction(budget, spent);
        if (!prediction.isEmpty()) {
            tvPrediction.setVisibility(View.VISIBLE);
            tvPrediction.setText(prediction);
        } else {
            tvPrediction.setVisibility(View.GONE);
        }

        // Gửi thông báo nếu cần
        if (percentageSpent > 80) {
            sendBudgetAlert(categoryName, remaining);
        }

        return cardView;
    }

    private double calculateSpent(Budget budget) {
        List<Expense> allExpenses = dbHelper.getExpensesByUser(budget.getUserId());
        double total = 0;
        for (Expense expense : allExpenses) {
            // Chỉ tính chi tiêu (TYPE_EXPENSE = 0)
            if (expense.getType() == Expense.TYPE_EXPENSE) {
                if (expense.getDate() >= budget.getPeriodStart() &&
                        expense.getDate() <= budget.getPeriodEnd()) {
                    if (budget.getCategoryId() == 0 ||
                            expense.getCategoryId() == budget.getCategoryId()) {
                        total += expense.getAmount();
                    }
                }
            }
        }
        return total;
    }

    /**
     * ✅ FIX: Hàm tính dự báo trả về chuỗi đa ngôn ngữ
     */
    private String calculatePrediction(Budget budget, double spent) {
        long currentTime = System.currentTimeMillis();

        if (currentTime < budget.getPeriodStart()) return "";

        long timeRemaining = budget.getPeriodEnd() - currentTime;
        if (timeRemaining <= 0) {
            return getString(R.string.prediction_ended);
        }

        long timeElapsed = currentTime - budget.getPeriodStart();
        int daysElapsed = (int) (timeElapsed / (1000 * 60 * 60 * 24));
        int daysRemaining = (int) (timeRemaining / (1000 * 60 * 60 * 24));

        if (daysElapsed == 0) return "";

        double dailyAverage = spent / daysElapsed;
        double predictedTotal = spent + (dailyAverage * daysRemaining);
        double predictedExcess = predictedTotal - budget.getAmount();

        if (predictedExcess > 0) {
            String excessAmount = currencyFormat.format(predictedExcess) + "đ";
            return getString(R.string.prediction_warning, excessAmount);
        } else {
            return getString(R.string.prediction_safe);
        }
    }

    /**
     * ✅ FIX: Gửi thông báo với nội dung đa ngôn ngữ
     */
    private void sendBudgetAlert(String categoryName, double remaining) {
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        // Dùng string resource có tham số %s
        String title = getString(R.string.budget_alert_title, categoryName);
        String message = getString(R.string.budget_alert_message, currencyFormat.format(remaining) + "đ");

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_warning) // Đảm bảo có icon này
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID, builder.build());
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.budget_alert_channel_name); // Cần thêm string này nếu chưa có
            String description = getString(R.string.budget_alert_channel_desc);
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