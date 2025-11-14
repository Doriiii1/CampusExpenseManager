package com.example.campusexpensemanager.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campusexpensemanager.R;
import com.example.campusexpensemanager.adapters.ExpenseAdapter;
import com.example.campusexpensemanager.models.Expense;
import com.example.campusexpensemanager.utils.DatabaseHelper;
import com.example.campusexpensemanager.utils.SessionManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.NumberFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * ExpenseListActivity displays all user expenses in RecyclerView
 * Features: Search, filter, monthly summary, add new expense
 */
public class ExpenseListActivity extends AppCompatActivity implements ExpenseAdapter.OnExpenseClickListener {

    private RecyclerView recyclerView;
    private ExpenseAdapter adapter;
    private EditText etSearch;
    private TextView tvMonthlyTotal, tvExpenseCount, tvEmptyState;
    private FloatingActionButton fabAddExpense;

    private DatabaseHelper dbHelper;
    private SessionManager sessionManager;

    private List<Expense> expenses;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expense_list);

        // Enable back button in action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Expenses");
        }

        // Initialize helpers
        dbHelper = DatabaseHelper.getInstance(this);
        sessionManager = new SessionManager(this);

        // Check authentication
        if (!sessionManager.isLoggedIn()) {
            finish();
            return;
        }

        // Initialize views
        initializeViews();

        // Setup RecyclerView
        setupRecyclerView();

        // Setup search
        setupSearch();

        // Setup click listeners
        setupClickListeners();

        // Load expenses
        loadExpenses();

        // Setup back pressed callback
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        // Handle back button in action bar
        onBackPressed();
        return true;
    }

    private void initializeViews() {
        recyclerView = findViewById(R.id.recycler_expenses);
        etSearch = findViewById(R.id.et_search);
        tvMonthlyTotal = findViewById(R.id.tv_monthly_total);
        tvExpenseCount = findViewById(R.id.tv_expense_count);
        tvEmptyState = findViewById(R.id.tv_empty_state);
        fabAddExpense = findViewById(R.id.fab_add_expense);
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (adapter != null) {
                    adapter.filter(s.toString());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupClickListeners() {
        // Add new expense
        fabAddExpense.setOnClickListener(v -> {
            Intent intent = new Intent(ExpenseListActivity.this, AddExpenseActivity.class);
            startActivity(intent);
        });

        // Animate FAB on scroll
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0) {
                    // Scrolling down - hide FAB
                    fabAddExpense.hide();
                } else if (dy < 0) {
                    // Scrolling up - show FAB
                    fabAddExpense.show();
                }
            }
        });
    }

    /**
     * Load expenses from database
     */
    private void loadExpenses() {
        int userId = sessionManager.getUserId();
        expenses = dbHelper.getExpensesByUser(userId);

        if (expenses.isEmpty()) {
            // Show empty state
            recyclerView.setVisibility(View.GONE);
            tvEmptyState.setVisibility(View.VISIBLE);
        } else {
            // Show list
            recyclerView.setVisibility(View.VISIBLE);
            tvEmptyState.setVisibility(View.GONE);

            // Create adapter
            adapter = new ExpenseAdapter(this, expenses, this);
            recyclerView.setAdapter(adapter);
        }

        // Calculate and display summary
        updateSummary();
    }

    /**
     * Update monthly total and expense count
     */
    private void updateSummary() {
        // Get current month's expenses
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        long monthStart = calendar.getTimeInMillis();

        calendar.add(Calendar.MONTH, 1);
        long monthEnd = calendar.getTimeInMillis();

        // Calculate total for this month
        double monthlyTotal = 0;
        int monthlyCount = 0;

        for (Expense expense : expenses) {
            if (expense.getDate() >= monthStart && expense.getDate() < monthEnd) {
                monthlyTotal += expense.getAmount();
                monthlyCount++;
            }
        }

        // Format and display
        NumberFormat currencyFormat = NumberFormat.getInstance(new Locale("vi", "VN"));
        String formattedTotal = currencyFormat.format(monthlyTotal) + "đ";

        tvMonthlyTotal.setText("Total This Month: " + formattedTotal);
        tvExpenseCount.setText(monthlyCount + " expenses this month | " +
                expenses.size() + " total");
    }

    /**
     * Handle expense item click
     */
    @Override
    public void onExpenseClick(Expense expense) {
        // Navigate to edit activity
        Intent intent = new Intent(ExpenseListActivity.this, EditExpenseActivity.class);
        intent.putExtra("expense_id", expense.getId());
        startActivity(intent);
    }

    @Override
    public void onBackPressed() {
        // Return to MainActivity instead of reloading
        super.onBackPressed();
        finish();
    }
}