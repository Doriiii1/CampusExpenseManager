package com.example.campusexpensemanager.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
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
 * ExpenseAdapter - Enhanced for Sprint 5 + Sera UI
 * FIXED: Dark Mode support, dynamic category icons, scroll sensitivity
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

    // ✅ NEW: Touch detection for scroll sensitivity fix
    private static final int CLICK_ACTION_THRESHOLD = 200; // milliseconds
    private long touchDownTime;

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

            // ✅ FIX 2A: Load dynamic category icon
            loadCategoryIcon(holder.ivCategoryIcon, category.getIconResource());
        }

        // Format amount with correct currency and +/- sign
        String formattedAmount = currencyConverter.formatWithSign(
                expense.getAmount(),
                expense.getCurrencyId(),
                expense.isIncome()
        );
        holder.tvAmount.setText(formattedAmount);

        // ✅ FIX: Use theme-aware colors with proper API
        if (expense.isIncome()) {
            // Green for income
            holder.tvAmount.setTextColor(
                    ContextCompat.getColor(context, R.color.success)
            );
        } else {
            // Red for expense
            holder.tvAmount.setTextColor(
                    ContextCompat.getColor(context, R.color.error)
            );
        }

        // Show recurring/receipt indicators
        if (expense.isRecurring()) {
            holder.ivReceiptIndicator.setVisibility(View.VISIBLE);
            holder.ivReceiptIndicator.setImageResource(android.R.drawable.ic_menu_rotate);
            holder.ivReceiptIndicator.setColorFilter(
                    ContextCompat.getColor(context, R.color.primary_blue)
            );
        } else if (expense.getReceiptPath() != null && !expense.getReceiptPath().isEmpty()) {
            holder.ivReceiptIndicator.setVisibility(View.VISIBLE);
            holder.ivReceiptIndicator.setImageResource(android.R.drawable.ic_menu_camera);
            holder.ivReceiptIndicator.setColorFilter(
                    ContextCompat.getColor(context, R.color.secondary_teal)
            );
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

        // ✅ FIX 2B: Improved click detection to avoid false triggers during scroll
        holder.cardView.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    touchDownTime = System.currentTimeMillis();
                    v.animate()
                            .scaleX(0.97f)
                            .scaleY(0.97f)
                            .setDuration(100)
                            .start();
                    break;

                case MotionEvent.ACTION_UP:
                    long clickDuration = System.currentTimeMillis() - touchDownTime;
                    v.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(100)
                            .start();

                    // Only trigger click if touch was short (not a scroll)
                    if (clickDuration < CLICK_ACTION_THRESHOLD) {
                        v.performClick();
                        if (listener != null) {
                            listener.onExpenseClick(expense);
                        }
                    }
                    break;

                case MotionEvent.ACTION_CANCEL:
                    v.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(100)
                            .start();
                    break;
            }
            return true; // Consume touch event
        });
    }

    /**
     * ✅ FIX 2A: Load category icon dynamically from drawable resources
     * @param imageView Target ImageView
     * @param iconName Icon resource name (e.g., "ic_food", "ic_transport")
     */
    private void loadCategoryIcon(ImageView imageView, String iconName) {
        try {
            // Get resource ID from drawable name
            Resources resources = context.getResources();
            int iconResId = resources.getIdentifier(
                    iconName,
                    "drawable",
                    context.getPackageName()
            );

            if (iconResId != 0) {
                // Icon found - load it
                Drawable icon = ContextCompat.getDrawable(context, iconResId);
                imageView.setImageDrawable(icon);
            } else {
                // Fallback to default icon
                imageView.setImageResource(android.R.drawable.ic_dialog_info);
            }
        } catch (Exception e) {
            // Error loading icon - use default
            imageView.setImageResource(android.R.drawable.ic_dialog_info);
            e.printStackTrace();
        }
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