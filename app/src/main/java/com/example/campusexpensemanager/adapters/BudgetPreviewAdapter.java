package com.example.campusexpensemanager.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campusexpensemanager.R;
import com.example.campusexpensemanager.models.Budget;
import com.example.campusexpensemanager.models.Category;
import com.example.campusexpensemanager.models.Expense;
import com.example.campusexpensemanager.utils.DatabaseHelper;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

/**
 * BudgetPreviewAdapter - Mini adapter for dashboard preview
 * Shows 2-3 budget items with progress
 */
public class BudgetPreviewAdapter extends RecyclerView.Adapter<BudgetPreviewAdapter.BudgetViewHolder> {

    private Context context;
    private List<Budget> budgets;
    private DatabaseHelper dbHelper;
    private OnItemClickListener listener;
    private NumberFormat currencyFormat;

    public interface OnItemClickListener {
        void onItemClick(Budget budget);
    }

    public BudgetPreviewAdapter(Context context, List<Budget> budgets, DatabaseHelper dbHelper) {
        this.context = context;
        this.budgets = budgets;
        this.dbHelper = dbHelper;
        this.currencyFormat = NumberFormat.getInstance(new Locale("vi", "VN"));
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public BudgetViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_budget_preview, parent, false);
        return new BudgetViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BudgetViewHolder holder, int position) {
        Budget budget = budgets.get(position);

        // Get category name
        String categoryName;
        if (budget.getCategoryId() > 0) {
            Category category = dbHelper.getCategoryById(budget.getCategoryId());
            if (category != null) {
                categoryName = DatabaseHelper.getLocalizedCategoryName(context, category.getName());
            } else {
                categoryName = context.getString(R.string.cat_unknown);
            }
        } else {
            categoryName = context.getString(R.string.label_total_budget);
        }

        // Calculate spent
        double spent = calculateSpent(budget);
        double percentage = budget.calculatePercentageSpent(spent);

        // Set data
        holder.tvCategoryName.setText(categoryName);
        holder.tvPercentage.setText(String.format("%.0f%%", percentage));
        holder.progressBar.setProgress((int) percentage);

        String spentFormatted = currencyFormat.format(spent) + "đ";
        String budgetFormatted = currencyFormat.format(budget.getAmount()) + "đ";
        holder.tvAmount.setText(spentFormatted + " / " + budgetFormatted);

        // Color coding
        int progressColor;
        if (percentage < 50) {
            progressColor = ContextCompat.getColor(context, R.color.budget_safe);
            holder.tvPercentage.setTextColor(ContextCompat.getColor(context, R.color.budget_safe));
        } else if (percentage < 80) {
            progressColor = ContextCompat.getColor(context, R.color.budget_warning);
            holder.tvPercentage.setTextColor(ContextCompat.getColor(context, R.color.budget_warning));
        } else {
            progressColor = ContextCompat.getColor(context, R.color.budget_danger);
            holder.tvPercentage.setTextColor(ContextCompat.getColor(context, R.color.budget_danger));
        }
        holder.progressBar.setProgressTintList(android.content.res.ColorStateList.valueOf(progressColor));

        // Click listener
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(budget);
            }
        });
    }

    @Override
    public int getItemCount() {
        return budgets.size();
    }

    private double calculateSpent(Budget budget) {
        List<Expense> allExpenses = dbHelper.getExpensesByUser(budget.getUserId());
        double total = 0;
        for (Expense expense : allExpenses) {
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

    static class BudgetViewHolder extends RecyclerView.ViewHolder {
        TextView tvCategoryName, tvPercentage, tvAmount;
        ProgressBar progressBar;

        public BudgetViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategoryName = itemView.findViewById(R.id.tv_budget_category_preview);
            tvPercentage = itemView.findViewById(R.id.tv_budget_percentage_preview);
            tvAmount = itemView.findViewById(R.id.tv_budget_amount_preview);
            progressBar = itemView.findViewById(R.id.progress_budget_preview);
        }
    }
}