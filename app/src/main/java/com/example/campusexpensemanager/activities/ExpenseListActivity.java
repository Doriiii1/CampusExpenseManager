package com.example.campusexpensemanager.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campusexpensemanager.R;
import com.example.campusexpensemanager.adapters.ExpenseAdapter;
import com.example.campusexpensemanager.models.Category;
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
    private Spinner spinnerSort;
    private String currentSortOption = "date_newest";

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
            getSupportActionBar().setTitle(getString(R.string.nav_expenses));
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
        setupSortSpinner();
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
        spinnerSort = findViewById(R.id.spinner_sort);
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

    /**
     * ✅ Setup Sort Spinner
     */
    private void setupSortSpinner() {
        String[] sortOptions = {
                getString(R.string.sort_date_newest),
                getString(R.string.sort_date_oldest),
                getString(R.string.sort_amount_highest),
                getString(R.string.sort_amount_lowest),
                getString(R.string.sort_category)
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                sortOptions
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSort.setAdapter(adapter);

        spinnerSort.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
                        currentSortOption = "date_newest";
                        break;
                    case 1:
                        currentSortOption = "date_oldest";
                        break;
                    case 2:
                        currentSortOption = "amount_highest";
                        break;
                    case 3:
                        currentSortOption = "amount_lowest";
                        break;
                    case 4:
                        currentSortOption = "category";
                        break;
                }
                applySortAndFilter();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
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
        applySortAndFilter();
    }

    /**
     * ✅ Apply both Filter and Sort
     */
    private void applySortAndFilter() {
        if (adapter == null) return;
        List<Expense> filtered = new ArrayList<>();
        // Step 1: Filter by type
        for (Expense expense : expenses) {
            if (currentFilter == -1) {
                filtered.add(expense);
            } else if (expense.getType() == currentFilter) {
                filtered.add(expense);
            }
        }
        // Step 2: Sort filtered list
        sortExpenses(filtered);
        // Step 3: Update adapter
        adapter.updateExpenses(filtered);

        // Update empty state
        if (filtered.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            tvEmptyState.setVisibility(View.VISIBLE);
            tvEmptyState.setText(getString(R.string.msg_no_expenses));
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            tvEmptyState.setVisibility(View.GONE);
        }

        updateSummary();
    }

    /**
     * ✅ Sort expenses based on current option
     */
    private void sortExpenses(List<Expense> expenseList) {
        switch (currentSortOption) {
            case "date_newest":
                Collections.sort(expenseList, (e1, e2) ->
                        Long.compare(e2.getDate(), e1.getDate()));
                break;

            case "date_oldest":
                Collections.sort(expenseList, (e1, e2) ->
                        Long.compare(e1.getDate(), e2.getDate()));
                break;

            case "amount_highest":
                Collections.sort(expenseList, (e1, e2) ->
                        Double.compare(e2.getAmount(), e1.getAmount()));
                break;

            case "amount_lowest":
                Collections.sort(expenseList, (e1, e2) ->
                        Double.compare(e1.getAmount(), e2.getAmount()));
                break;

            case "category":
                Collections.sort(expenseList, (e1, e2) -> {
                    Category cat1 = dbHelper.getCategoryById(e1.getCategoryId());
                    Category cat2 = dbHelper.getCategoryById(e2.getCategoryId());

                    String name1 = (cat1 != null) ?
                            DatabaseHelper.getLocalizedCategoryName(this, cat1.getName()) : "";
                    String name2 = (cat2 != null) ?
                            DatabaseHelper.getLocalizedCategoryName(this, cat2.getName()) : "";

                    return name1.compareTo(name2);
                });
                break;
        }
    }

    /**
     * ✅ FIX: Update summary text with localized strings
     */
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
                // Apply filter logic to summary
                if (currentFilter == -1 || expense.getType() == currentFilter) {

                    // Sửa lại logic tính tổng cho phù hợp bộ lọc:
                    if (currentFilter == Expense.TYPE_INCOME) {
                        monthlyTotal += expense.getAmount(); // Tổng thu nhập
                    } else if (currentFilter == Expense.TYPE_EXPENSE) {
                        monthlyTotal += expense.getAmount(); // Tổng chi tiêu
                    } else {

                        if (expense.isExpense()) {
                            monthlyTotal += expense.getAmount();
                        }
                    }

                    monthlyCount++;
                }
            }
        }

        NumberFormat currencyFormat = NumberFormat.getInstance(new Locale("vi", "VN"));
        String formattedTotal = currencyFormat.format(monthlyTotal) + "đ";

        // Xác định hậu tố (Suffix) dựa trên bộ lọc
        String filterSuffix = "";
        if (currentFilter == Expense.TYPE_INCOME) {
            filterSuffix = getString(R.string.filter_income_suffix);
        } else if (currentFilter == Expense.TYPE_EXPENSE) {
            filterSuffix = getString(R.string.filter_expense_suffix);
        }

        tvMonthlyTotal.setText(getString(R.string.summary_monthly_total, filterSuffix, formattedTotal));
        tvExpenseCount.setText(getString(R.string.summary_expense_count, monthlyCount, expenses.size()));
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