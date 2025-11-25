package com.example.campusexpensemanager.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campusexpensemanager.R;
import com.example.campusexpensemanager.adapters.BudgetAdapter;
import com.example.campusexpensemanager.models.Budget;
import com.example.campusexpensemanager.models.Category;
import com.example.campusexpensemanager.utils.DatabaseHelper;
import com.example.campusexpensemanager.utils.SessionManager;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * SetBudgetActivity handles creating and viewing budgets
 */
public class SetBudgetActivity extends BaseActivity implements BudgetAdapter.OnBudgetClickListener {

    private TextInputLayout tilAmount;
    private TextInputEditText etAmount;
    private Spinner spinnerCategory, spinnerPeriod;
    private TextView tvPeriodDates;
    private Button btnSaveBudget;
    private RecyclerView recyclerBudgets;

    private DatabaseHelper dbHelper;
    private SessionManager sessionManager;
    private BudgetAdapter adapter;

    private List<Category> categories;
    private List<Budget> budgets;

    private long periodStart;
    private long periodEnd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_budget);
        hideBottomNavigation();

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

        // Load categories
        loadCategories();

        // Setup period spinner
        setupPeriodSpinner();

        // Setup click listeners
        setupClickListeners();

        // Load existing budgets
        loadBudgets();
    }

    private void initializeViews() {
        tilAmount = findViewById(R.id.til_budget_amount);
        etAmount = findViewById(R.id.et_budget_amount);
        spinnerCategory = findViewById(R.id.spinner_budget_category);
        spinnerPeriod = findViewById(R.id.spinner_budget_period);
        tvPeriodDates = findViewById(R.id.tv_period_dates);
        btnSaveBudget = findViewById(R.id.btn_save_budget);
        recyclerBudgets = findViewById(R.id.recycler_budgets);

        // Setup RecyclerView
        recyclerBudgets.setLayoutManager(new LinearLayoutManager(this));
        recyclerBudgets.setHasFixedSize(true);
    }

    /**
     * Load categories into spinner
     */
    /**
     * ✅ FIX: Load categories with localization support
     */
    private void loadCategories() {
        categories = new ArrayList<>();

        // Thêm tùy chọn "Tất cả danh mục" với ID = 0
        // Ta dùng key "cat_all" hoặc để trống name, adapter sẽ xử lý hiển thị
        Category allCategories = new Category(0, "cat_all", "");
        categories.add(allCategories);

        // Thêm các danh mục từ database
        List<Category> dbCategories = dbHelper.getAllCategories();
        categories.addAll(dbCategories);

        // Sử dụng Custom Adapter để hiển thị tên đa ngôn ngữ
        ArrayAdapter<Category> adapter = new ArrayAdapter<Category>(
                this,
                android.R.layout.simple_spinner_item,
                categories
        ) {
            @androidx.annotation.NonNull
            @Override
            public View getView(int position, View convertView, @androidx.annotation.NonNull android.view.ViewGroup parent) {
                android.widget.TextView label = (android.widget.TextView) super.getView(position, convertView, parent);
                setLabelText(label, getItem(position));
                return label;
            }

            @Override
            public View getDropDownView(int position, View convertView, @androidx.annotation.NonNull android.view.ViewGroup parent) {
                android.widget.TextView label = (android.widget.TextView) super.getDropDownView(position, convertView, parent);
                setLabelText(label, getItem(position));
                return label;
            }

            // Hàm phụ trợ để set text
            private void setLabelText(android.widget.TextView label, Category category) {
                if (category != null) {
                    if (category.getId() == 0) {
                        // Nếu là item đầu tiên -> Lấy chuỗi "Tất cả danh mục"
                        label.setText(getString(R.string.all_categories));
                    } else {
                        // Các item khác -> Lấy tên từ DB đã được dịch
                        label.setText(category.getLocalizedName(getContext()));
                    }
                }
            }
        };

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);
    }

    /**
     * ✅ FIX: Setup period spinner with localized strings
     */
    private void setupPeriodSpinner() {
        // Tạo danh sách tùy chọn từ Resource String
        List<String> periods = new ArrayList<>();
        periods.add(getString(R.string.period_weekly));  // Index 0
        periods.add(getString(R.string.period_monthly)); // Index 1

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                periods
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPeriod.setAdapter(adapter);

        // Tính toán ngày ban đầu
        calculatePeriodDates();

        // Update dates when period changes
        spinnerPeriod.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                calculatePeriodDates();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }

    /**
     * Calculate period start and end dates
     */
    /**
     * ✅ FIX: Calculate dates based on index instead of hardcoded string
     */
    private void calculatePeriodDates() {
        Calendar calendar = Calendar.getInstance();
        periodStart = calendar.getTimeInMillis();

        // Lấy vị trí đang chọn (0 = Weekly, 1 = Monthly)
        int selectedIndex = spinnerPeriod.getSelectedItemPosition();

        if (selectedIndex == 0) { // Weekly
            calendar.add(Calendar.DAY_OF_MONTH, 7);
        } else { // Monthly (Index 1)
            calendar.add(Calendar.MONTH, 1);
        }

        periodEnd = calendar.getTimeInMillis();

        // Display dates
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        String startDate = dateFormat.format(periodStart);
        String endDate = dateFormat.format(periodEnd);

        // Sử dụng string resource cho nhãn "Period:"
        tvPeriodDates.setText(getString(R.string.label_period_prefix) + startDate + " - " + endDate);
    }

    private void setupClickListeners() {
        btnSaveBudget.setOnClickListener(v -> saveBudget());
    }

    /**
     * ✅ FIX: Validate and save budget with localized messages
     */
    private void saveBudget() {
        // Validate amount
        String amountStr = etAmount.getText().toString().trim();
        if (amountStr.isEmpty()) {
            tilAmount.setError(getString(R.string.error_empty_field));
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            tilAmount.setError(getString(R.string.msg_invalid_amount));
            return;
        }

        if (amount <= 0) {
            tilAmount.setError(getString(R.string.err_amount_positive));
            return;
        }

        tilAmount.setError(null);

        // Get selected category
        Category selectedCategory = (Category) spinnerCategory.getSelectedItem();
        int categoryId = selectedCategory.getId();

        // Create budget object
        int userId = sessionManager.getUserId();
        Budget budget = new Budget(userId, categoryId, amount, periodStart, periodEnd);

        // Insert into database
        long budgetId = dbHelper.insertBudget(budget);

        if (budgetId != -1) {
            Toast.makeText(this, getString(R.string.budget_added), Toast.LENGTH_SHORT).show();

            // Clear form
            etAmount.setText("");

            // Reload budgets
            loadBudgets();
        } else {
            Toast.makeText(this, getString(R.string.msg_save_budget_error), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Load existing budgets
     */
    private void loadBudgets() {
        int userId = sessionManager.getUserId();
        budgets = dbHelper.getBudgetsByUser(userId);

        if (budgets.isEmpty()) {
            recyclerBudgets.setVisibility(View.GONE);
        } else {
            recyclerBudgets.setVisibility(View.VISIBLE);

            if (adapter == null) {
                adapter = new BudgetAdapter(this, budgets, this);
                recyclerBudgets.setAdapter(adapter);
            } else {
                adapter.updateBudgets(budgets);
            }
        }
    }

    @Override
    public void onBudgetClick(Budget budget) {
        // Navigate to EditBudgetActivity
        Intent intent = new Intent(SetBudgetActivity.this, EditBudgetActivity.class);
        intent.putExtra("budget_id", budget.getId());
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadBudgets();
    }
}