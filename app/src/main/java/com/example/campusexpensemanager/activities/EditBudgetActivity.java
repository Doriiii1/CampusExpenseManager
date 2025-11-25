package com.example.campusexpensemanager.activities;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.campusexpensemanager.R;
import com.example.campusexpensemanager.models.Budget;
import com.example.campusexpensemanager.models.Category;
import com.example.campusexpensemanager.utils.DatabaseHelper;
import com.example.campusexpensemanager.utils.SessionManager;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * EditBudgetActivity handles editing and deleting budgets
 */
public class EditBudgetActivity extends BaseActivity {

    private TextInputLayout tilAmount;
    private TextInputEditText etAmount;
    private Spinner spinnerCategory;
    private Button btnPeriodStart, btnPeriodEnd, btnUpdate, btnDelete;

    private DatabaseHelper dbHelper;
    private SessionManager sessionManager;

    private Budget currentBudget;
    private List<Category> categories;
    private Calendar periodStart, periodEnd;

    private Budget deletedBudget; // For undo

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_budget);
        hideBottomNavigation();

        // Initialize helpers
        dbHelper = DatabaseHelper.getInstance(this);
        sessionManager = new SessionManager(this);

        // Get budget ID from intent
        int budgetId = getIntent().getIntExtra("budget_id", -1);
        if (budgetId == -1) {
            Toast.makeText(this, "Invalid budget", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Load budget
        loadBudget(budgetId);

        if (currentBudget == null) {
            Toast.makeText(this, "Budget not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize views
        initializeViews();

        // Load categories
        loadCategories();

        // Pre-fill form
        prefillForm();

        // Setup click listeners
        setupClickListeners();
    }

    private void initializeViews() {
        tilAmount = findViewById(R.id.til_budget_amount);
        etAmount = findViewById(R.id.et_budget_amount);
        spinnerCategory = findViewById(R.id.spinner_budget_category);
        btnPeriodStart = findViewById(R.id.btn_period_start);
        btnPeriodEnd = findViewById(R.id.btn_period_end);
        btnUpdate = findViewById(R.id.btn_update_budget);
        btnDelete = findViewById(R.id.btn_delete_budget);
    }

    private void loadBudget(int budgetId) {
        List<Budget> budgets = dbHelper.getBudgetsByUser(sessionManager.getUserId());

        for (Budget budget : budgets) {
            if (budget.getId() == budgetId) {
                currentBudget = budget;
                break;
            }
        }
    }

    /**
     * ✅ FIX: Load categories with localization support
     */
    private void loadCategories() {
        categories = new ArrayList<>();

        // Thêm tùy chọn "Tất cả danh mục" (ID = 0) với key
        Category allCategories = new Category(0, "cat_all", "");
        categories.add(allCategories);

        // Thêm danh mục từ DB
        List<Category> dbCategories = dbHelper.getAllCategories();
        categories.addAll(dbCategories);

        // Custom Adapter để hiển thị tên đã dịch
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

            private void setLabelText(android.widget.TextView label, Category category) {
                if (category != null) {
                    if (category.getId() == 0) {
                        label.setText(getString(R.string.all_categories)); // Lấy từ string resource
                    } else {
                        label.setText(category.getLocalizedName(getContext()));
                    }
                }
            }
        };

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);
    }

    private void prefillForm() {
        // Amount
        etAmount.setText(String.valueOf(currentBudget.getAmount()));

        // Category
        for (int i = 0; i < categories.size(); i++) {
            if (categories.get(i).getId() == currentBudget.getCategoryId()) {
                spinnerCategory.setSelection(i);
                break;
            }
        }

        // Period dates
        periodStart = Calendar.getInstance();
        periodStart.setTimeInMillis(currentBudget.getPeriodStart());

        periodEnd = Calendar.getInstance();
        periodEnd.setTimeInMillis(currentBudget.getPeriodEnd());

        updateDateButtons();
    }

    private void setupClickListeners() {
        btnPeriodStart.setOnClickListener(v -> showStartDatePicker());
        btnPeriodEnd.setOnClickListener(v -> showEndDatePicker());
        btnUpdate.setOnClickListener(v -> updateBudget());
        btnDelete.setOnClickListener(v -> showDeleteConfirmation());
    }

    private void showStartDatePicker() {
        DatePickerDialog picker = new DatePickerDialog(
                this,
                (view, year, month, day) -> {
                    periodStart.set(Calendar.YEAR, year);
                    periodStart.set(Calendar.MONTH, month);
                    periodStart.set(Calendar.DAY_OF_MONTH, day);
                    updateDateButtons();
                },
                periodStart.get(Calendar.YEAR),
                periodStart.get(Calendar.MONTH),
                periodStart.get(Calendar.DAY_OF_MONTH)
        );
        picker.show();
    }

    private void showEndDatePicker() {
        DatePickerDialog picker = new DatePickerDialog(
                this,
                (view, year, month, day) -> {
                    periodEnd.set(Calendar.YEAR, year);
                    periodEnd.set(Calendar.MONTH, month);
                    periodEnd.set(Calendar.DAY_OF_MONTH, day);
                    updateDateButtons();
                },
                periodEnd.get(Calendar.YEAR),
                periodEnd.get(Calendar.MONTH),
                periodEnd.get(Calendar.DAY_OF_MONTH)
        );
        picker.show();
    }

    private void updateDateButtons() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        btnPeriodStart.setText(dateFormat.format(periodStart.getTime()));
        btnPeriodEnd.setText(dateFormat.format(periodEnd.getTime()));
    }

    /**
     * ✅ FIX: Update budget with localized messages
     */
    private void updateBudget() {
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

        // Validate dates
        if (periodEnd.before(periodStart)) {
            Toast.makeText(this, getString(R.string.err_date_range), Toast.LENGTH_SHORT).show();
            return;
        }

        // Update budget object
        Category selectedCategory = (Category) spinnerCategory.getSelectedItem();
        currentBudget.setAmount(amount);
        currentBudget.setCategoryId(selectedCategory.getId());
        currentBudget.setPeriodStart(periodStart.getTimeInMillis());
        currentBudget.setPeriodEnd(periodEnd.getTimeInMillis());

        // Update in database
        int rowsAffected = dbHelper.updateBudget(currentBudget);

        if (rowsAffected > 0) {
            Toast.makeText(this, getString(R.string.budget_updated), Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, getString(R.string.budget_update_failed), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * ✅ FIX: Show delete dialog with localized strings
     */
    private void showDeleteConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.budget_delete_confirm_title))
                .setMessage(getString(R.string.budget_delete_confirm_message))
                .setPositiveButton(getString(R.string.action_yes), (dialog, which) -> deleteBudget())
                .setNegativeButton(getString(R.string.action_no), null)
                .show();
    }

    /**
     * ✅ FIX: Delete budget with localized Snackbar
     */
    private void deleteBudget() {
        // Store for undo
        deletedBudget = new Budget(
                currentBudget.getId(),
                currentBudget.getUserId(),
                currentBudget.getCategoryId(),
                currentBudget.getAmount(),
                currentBudget.getPeriodStart(),
                currentBudget.getPeriodEnd(),
                currentBudget.getCreatedAt()
        );

        // Delete from database
        int rowsDeleted = dbHelper.deleteBudget(currentBudget.getId());

        if (rowsDeleted > 0) {
            // Show snackbar with undo
            Snackbar snackbar = Snackbar.make(
                    findViewById(android.R.id.content),
                    getString(R.string.budget_deleted),
                    Snackbar.LENGTH_LONG
            );

            snackbar.setAction(getString(R.string.action_undo), v -> undoDelete());

            snackbar.addCallback(new Snackbar.Callback() {
                @Override
                public void onDismissed(Snackbar transientBottomBar, int event) {
                    if (event != DISMISS_EVENT_ACTION) {
                        finish();
                    }
                }
            });

            snackbar.show();
        } else {
            Toast.makeText(this, getString(R.string.budget_delete_failed), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * ✅ FIX: Undo delete with localized messages
     */
    private void undoDelete() {
        if (deletedBudget != null) {
            long newId = dbHelper.insertBudget(deletedBudget);

            if (newId != -1) {
                deletedBudget.setId((int) newId);
                currentBudget = deletedBudget;
                Toast.makeText(this, getString(R.string.budget_restored), Toast.LENGTH_SHORT).show();
                prefillForm();
            } else {
                Toast.makeText(this, getString(R.string.budget_restore_failed), Toast.LENGTH_SHORT).show();
            }
        }
    }
}