package com.example.campusexpensemanager.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campusexpensemanager.R;
import com.example.campusexpensemanager.adapters.ExpenseAdapter;
import com.example.campusexpensemanager.models.Expense;
import com.example.campusexpensemanager.utils.DatabaseHelper;
import com.example.campusexpensemanager.utils.SessionManager;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.NumberFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * ExpenseListActivity - Enhanced Sprint 6
 * NEW: Type filter (All/Income/Expense), Auto-refresh on add
 */
public class ExpenseListActivity extends BaseActivity implements ExpenseAdapter.OnExpenseClickListener {

    private RecyclerView recyclerView;
    private ExpenseAdapter adapter;
    private EditText etSearch;

    // Filter chips
    private ChipGroup chipGroupFilter;
    private Chip chipAll, chipIncome, chipExpense;

    private TextView tvMonthlyTotal, tvExpenseCount, tvEmptyState;
    private FloatingActionButton fabAddExpense;

    private DatabaseHelper dbHelper;
    private SessionManager sessionManager;

    private List<Expense> expenses;
    private int currentFilter = -1; // -1=All, 0=Expense, 1=Income

    // Activity Result Launcher for Add/Edit
    private ActivityResultLauncher<Intent> addExpenseLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expense_list);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Giao dịch");
        }

        dbHelper = DatabaseHelper.getInstance(this);
        sessionManager = new SessionManager(this);

        if (!sessionManager.isLoggedIn()) {
            finish();
            return;
        }

        initializeViews();
        setupRecyclerView();
        setupSearch();
        setupFilterChips();
        setupClickListeners();
        setupActivityLaunchers();
        loadExpenses();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void initializeViews() {
        recyclerView = findViewById(R.id.recycler_expenses);
        etSearch = findViewById(R.id.et_search);

        chipGroupFilter = findViewById(R.id.chip_group_filter);
        chipAll = findViewById(R.id.chip_all);
        chipIncome = findViewById(R.id.chip_income);
        chipExpense = findViewById(R.id.chip_expense);

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

    private void setupFilterChips() {
        chipGroupFilter.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chip_all) {
                currentFilter = -1;
            } else if (checkedId == R.id.chip_income) {
                currentFilter = Expense.TYPE_INCOME;
            } else if (checkedId == R.id.chip_expense) {
                currentFilter = Expense.TYPE_EXPENSE;
            }
            applyTypeFilter();
        });
    }

    private void setupClickListeners() {
        fabAddExpense.setOnClickListener(v -> {
            Intent intent = new Intent(ExpenseListActivity.this, AddExpenseActivity.class);
            addExpenseLauncher.launch(intent);
        });

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0) {
                    fabAddExpense.hide();
                } else if (dy < 0) {
                    fabAddExpense.show();
                }
            }
        });
    }

    private void setupActivityLaunchers() {
        // Launcher for Add Expense - Auto refresh on result
        addExpenseLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    // ALWAYS reload expenses when returning from Add/Edit
                    // Even if result code is not OK (user may have added then pressed back)
                    loadExpenses();
                }
        );
    }

    private void loadExpenses() {
        int userId = sessionManager.getUserId();
        expenses = dbHelper.getExpensesByUser(userId);

        if (expenses.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            tvEmptyState.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            tvEmptyState.setVisibility(View.GONE);

            adapter = new ExpenseAdapter(this, expenses, this);
            recyclerView.setAdapter(adapter);

            // Apply current filter
            applyTypeFilter();
        }

        updateSummary();
    }

    private void applyTypeFilter() {
        if (adapter == null) return;

        List<Expense> filtered = new java.util.ArrayList<>();

        for (Expense expense : expenses) {
            if (currentFilter == -1) {
                filtered.add(expense); // Show all
            } else if (expense.getType() == currentFilter) {
                filtered.add(expense); // Show matching type
            }
        }

        adapter.updateExpenses(filtered);
        updateSummary();
    }

    private void updateSummary() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        long monthStart = calendar.getTimeInMillis();

        calendar.add(Calendar.MONTH, 1);
        long monthEnd = calendar.getTimeInMillis();

        double monthlyTotal = 0;
        int monthlyCount = 0;

        for (Expense expense : expenses) {
            if (expense.getDate() >= monthStart && expense.getDate() < monthEnd) {
                // Apply filter
                if (currentFilter == -1 || expense.getType() == currentFilter) {
                    if (expense.isExpense()) {
                        monthlyTotal += expense.getAmount();
                    }
                    monthlyCount++;
                }
            }
        }

        NumberFormat currencyFormat = NumberFormat.getInstance(new Locale("vi", "VN"));
        String formattedTotal = currencyFormat.format(monthlyTotal) + "đ";

        String filterText = "";
        if (currentFilter == Expense.TYPE_INCOME) {
            filterText = " (Thu nhập)";
        } else if (currentFilter == Expense.TYPE_EXPENSE) {
            filterText = " (Chi tiêu)";
        }

        tvMonthlyTotal.setText("Tổng tháng này" + filterText + ": " + formattedTotal);
        tvExpenseCount.setText(monthlyCount + " giao dịch tháng này | " +
                expenses.size() + " tổng cộng");
    }

    @Override
    public void onExpenseClick(Expense expense) {
        Intent intent = new Intent(ExpenseListActivity.this, EditExpenseActivity.class);
        intent.putExtra("expense_id", expense.getId());
        addExpenseLauncher.launch(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadExpenses();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}