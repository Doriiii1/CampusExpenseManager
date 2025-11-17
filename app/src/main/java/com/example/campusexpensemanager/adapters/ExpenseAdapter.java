package com.example.campusexpensemanager.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campusexpensemanager.R;
import com.example.campusexpensemanager.models.Category;
import com.example.campusexpensemanager.models.Expense;
import com.example.campusexpensemanager.utils.CurrencyConverter;
import com.example.campusexpensemanager.utils.DatabaseHelper;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * ExpenseAdapter - Enhanced for Sprint 5
 * NEW: Income/Expense color coding with +/- signs
 */
public class ExpenseAdapter extends RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder> {

    private Context context;
    private List<Expense> expenses;
    private List<Expense> expensesFiltered;
    private DatabaseHelper dbHelper;
    private OnExpenseClickListener listener;
    private CurrencyConverter currencyConverter;

    private NumberFormat currencyFormat;
    private SimpleDateFormat dateFormat;

    public interface OnExpenseClickListener {
        void onExpenseClick(Expense expense);
    }

    public ExpenseAdapter(Context context, List<Expense> expenses, OnExpenseClickListener listener) {
        this.context = context;
        this.expenses = expenses;
        this.expensesFiltered = new ArrayList<>(expenses);
        this.listener = listener;
        this.dbHelper = DatabaseHelper.getInstance(context);
        this.currencyConverter = new CurrencyConverter(context);

        this.currencyFormat = NumberFormat.getInstance(new Locale("vi", "VN"));
        this.dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
    }

    @NonNull
    @Override
    public ExpenseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_expense, parent, false);
        return new ExpenseViewHolder(view);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onBindViewHolder(@NonNull ExpenseViewHolder holder, int position) {
        Expense expense = expensesFiltered.get(position);

        // Get category info
        Category category = dbHelper.getCategoryById(expense.getCategoryId());

        if (category != null) {
            holder.tvCategoryName.setText(category.getName());
        }

        // Format amount with correct currency and +/- sign
        String formattedAmount = currencyConverter.formatWithSign(
                expense.getAmount(),
                expense.getCurrencyId(),
                expense.isIncome()
        );
        holder.tvAmount.setText(formattedAmount);

        // Color coding based on type
        if (expense.isIncome()) {
            // Green for income
            holder.tvAmount.setTextColor(context.getResources().getColor(R.color.success));
            holder.cardView.setCardBackgroundColor(
                    context.getResources().getColor(R.color.light_surface));
        } else {
            // Red for expense
            holder.tvAmount.setTextColor(context.getResources().getColor(R.color.error));
            holder.cardView.setCardBackgroundColor(
                    context.getResources().getColor(R.color.light_surface));
        }

        // Show recurring indicator
        if (expense.isRecurring()) {
            holder.ivReceiptIndicator.setVisibility(View.VISIBLE);
            holder.ivReceiptIndicator.setImageResource(android.R.drawable.ic_menu_rotate);
            holder.ivReceiptIndicator.setColorFilter(
                    context.getResources().getColor(R.color.primary_blue));
        } else if (expense.getReceiptPath() != null && !expense.getReceiptPath().isEmpty()) {
            holder.ivReceiptIndicator.setVisibility(View.VISIBLE);
            holder.ivReceiptIndicator.setImageResource(android.R.drawable.ic_menu_camera);
        } else {
            holder.ivReceiptIndicator.setVisibility(View.GONE);
        }

        // Format date
        String formattedDate = dateFormat.format(new Date(expense.getDate()));
        holder.tvDate.setText(formattedDate);

        // Description
        String description = expense.getDescription();
        if (description != null && !description.isEmpty()) {
            if (description.length() > 50) {
                description = description.substring(0, 47) + "...";
            }
            holder.tvDescription.setText(description);
            holder.tvDescription.setVisibility(View.VISIBLE);
        } else {
            holder.tvDescription.setVisibility(View.GONE);
        }

        // Click listener
        holder.cardView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onExpenseClick(expense);
            }
        });

        // Add scale animation on touch
        holder.cardView.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).start();
                    break;
                case android.view.MotionEvent.ACTION_UP:
                case android.view.MotionEvent.ACTION_CANCEL:
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start();
                    break;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return expensesFiltered.size();
    }

    public void updateExpenses(List<Expense> newExpenses) {
        this.expenses = newExpenses;
        this.expensesFiltered = new ArrayList<>(newExpenses);
        notifyDataSetChanged();
    }

    public void filter(String query) {
        expensesFiltered.clear();

        if (query == null || query.isEmpty()) {
            expensesFiltered.addAll(expenses);
        } else {
            String lowerCaseQuery = query.toLowerCase();

            for (Expense expense : expenses) {
                String description = expense.getDescription();
                Category category = dbHelper.getCategoryById(expense.getCategoryId());

                boolean matchDescription = description != null &&
                        description.toLowerCase().contains(lowerCaseQuery);
                boolean matchCategory = category != null &&
                        category.getName().toLowerCase().contains(lowerCaseQuery);

                if (matchDescription || matchCategory) {
                    expensesFiltered.add(expense);
                }
            }
        }

        notifyDataSetChanged();
    }

    public void filterByCategory(int categoryId) {
        expensesFiltered.clear();

        if (categoryId == 0) {
            expensesFiltered.addAll(expenses);
        } else {
            for (Expense expense : expenses) {
                if (expense.getCategoryId() == categoryId) {
                    expensesFiltered.add(expense);
                }
            }
        }

        notifyDataSetChanged();
    }

    public void filterByDateRange(long startDate, long endDate) {
        expensesFiltered.clear();

        for (Expense expense : expenses) {
            long expenseDate = expense.getDate();
            if (expenseDate >= startDate && expenseDate <= endDate) {
                expensesFiltered.add(expense);
            }
        }

        notifyDataSetChanged();
    }

    static class ExpenseViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        ImageView ivCategoryIcon;
        TextView tvCategoryName;
        TextView tvAmount;
        TextView tvDate;
        TextView tvDescription;
        ImageView ivReceiptIndicator;

        public ExpenseViewHolder(@NonNull View itemView) {
            super(itemView);

            cardView = itemView.findViewById(R.id.card_expense_item);
            ivCategoryIcon = itemView.findViewById(R.id.iv_category_icon);
            tvCategoryName = itemView.findViewById(R.id.tv_category_name);
            tvAmount = itemView.findViewById(R.id.tv_amount);
            tvDate = itemView.findViewById(R.id.tv_date);
            tvDescription = itemView.findViewById(R.id.tv_description);
            ivReceiptIndicator = itemView.findViewById(R.id.iv_receipt_indicator);
        }
    }
}