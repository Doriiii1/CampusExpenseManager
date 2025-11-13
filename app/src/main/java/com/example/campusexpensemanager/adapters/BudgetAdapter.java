package com.example.campusexpensemanager.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campusexpensemanager.R;
import com.example.campusexpensemanager.models.Budget;
import com.example.campusexpensemanager.models.Category;
import com.example.campusexpensemanager.utils.DatabaseHelper;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * BudgetAdapter for RecyclerView displaying budget list with progress
 */
public class BudgetAdapter extends RecyclerView.Adapter<BudgetAdapter.BudgetViewHolder> {

    private Context context;
    private List<Budget> budgets;
    private DatabaseHelper dbHelper;
    private OnBudgetClickListener listener;

    private NumberFormat currencyFormat;
    private SimpleDateFormat dateFormat;

    public interface OnBudgetClickListener {
        void onBudgetClick(Budget budget);
    }

    public BudgetAdapter(Context context, List<Budget> budgets, OnBudgetClickListener listener) {
        this.context = context;
        this.budgets = budgets;
        this.listener = listener;
        this.dbHelper = DatabaseHelper.getInstance(context);

        this.currencyFormat = NumberFormat.getInstance(new Locale("vi", "VN"));
        this.dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
    }

    @NonNull
    @Override
    public BudgetViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_budget, parent, false);
        return new BudgetViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BudgetViewHolder holder, int position) {
        Budget budget = budgets.get(position);

        // Get category name
        String categoryName = "Total Budget";
        if (budget.getCategoryId() > 0) {
            Category category = dbHelper.getCategoryById(budget.getCategoryId());
            if (category != null) {
                categoryName = category.getName();
            }
        }
        holder.tvCategoryName.setText(categoryName);

        // Calculate spent amount
        double spent = calculateSpent(budget);
        double remaining = budget.getAmount() - spent;
        double percentageSpent = budget.calculatePercentageSpent(spent);

        // Format amounts
        String budgetAmount = currencyFormat.format(budget.getAmount()) + "đ";
        String spentAmount = currencyFormat.format(spent) + "đ";
        String remainingAmount = currencyFormat.format(remaining) + "đ";

        holder.tvBudgetAmount.setText("Budget: " + budgetAmount);
        holder.tvSpentAmount.setText("Spent: " + spentAmount);
        holder.tvRemainingAmount.setText("Remaining: " + remainingAmount);

        // Set progress bar
        holder.progressBar.setProgress((int) percentageSpent);

        // Set progress bar color based on percentage
        int progressColor;
        if (percentageSpent < 50) {
            progressColor = ContextCompat.getColor(context, R.color.budget_safe);
        } else if (percentageSpent < 80) {
            progressColor = ContextCompat.getColor(context, R.color.budget_warning);
        } else {
            progressColor = ContextCompat.getColor(context, R.color.budget_danger);
        }
        holder.progressBar.setProgressTintList(
                android.content.res.ColorStateList.valueOf(progressColor));

        // Period dates
        String periodStart = dateFormat.format(new Date(budget.getPeriodStart()));
        String periodEnd = dateFormat.format(new Date(budget.getPeriodEnd()));
        holder.tvPeriod.setText(periodStart + " - " + periodEnd);

        // Click listener
        holder.cardView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onBudgetClick(budget);
            }
        });
    }

    @Override
    public int getItemCount() {
        return budgets.size();
    }

    /**
     * Calculate total spent for a budget
     */
    private double calculateSpent(Budget budget) {
        // Query expenses within budget period
        List<com.example.campusexpensemanager.models.Expense> allExpenses =
                dbHelper.getExpensesByUser(budget.getUserId());

        double total = 0;
        for (com.example.campusexpensemanager.models.Expense expense : allExpenses) {
            // Check if expense is within period
            if (expense.getDate() >= budget.getPeriodStart() &&
                    expense.getDate() <= budget.getPeriodEnd()) {

                // If budget is for specific category, filter by category
                if (budget.getCategoryId() == 0 ||
                        expense.getCategoryId() == budget.getCategoryId()) {
                    total += expense.getAmount();
                }
            }
        }

        return total;
    }

    /**
     * Update budget list
     */
    public void updateBudgets(List<Budget> newBudgets) {
        this.budgets = newBudgets;
        notifyDataSetChanged();
    }

    static class BudgetViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView tvCategoryName;
        TextView tvBudgetAmount;
        TextView tvSpentAmount;
        TextView tvRemainingAmount;
        TextView tvPeriod;
        ProgressBar progressBar;

        public BudgetViewHolder(@NonNull View itemView) {
            super(itemView);

            cardView = itemView.findViewById(R.id.card_budget_item);
            tvCategoryName = itemView.findViewById(R.id.tv_budget_category);
            tvBudgetAmount = itemView.findViewById(R.id.tv_budget_amount);
            tvSpentAmount = itemView.findViewById(R.id.tv_spent_amount);
            tvRemainingAmount = itemView.findViewById(R.id.tv_remaining_amount);
            tvPeriod = itemView.findViewById(R.id.tv_budget_period);
            progressBar = itemView.findViewById(R.id.progress_budget);
        }
    }
}