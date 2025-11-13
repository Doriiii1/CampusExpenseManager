package com.example.campusexpensemanager.activities;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.campusexpensemanager.R;
import com.example.campusexpensemanager.models.Category;
import com.example.campusexpensemanager.models.Expense;
import com.example.campusexpensemanager.utils.DatabaseHelper;
import com.example.campusexpensemanager.utils.SessionManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * EditExpenseActivity handles editing and deleting expenses
 * Features: Pre-filled form, Update button, Delete with undo
 */
public class EditExpenseActivity extends AppCompatActivity {

    private TextInputLayout tilAmount, tilDescription;
    private TextInputEditText etAmount, etDescription;
    private Spinner spinnerCategory;
    private Button btnSelectDate, btnSelectTime, btnDelete;
    private ImageView ivReceiptPreview;
    private FloatingActionButton fabUpdate;

    private DatabaseHelper dbHelper;
    private SessionManager sessionManager;

    private Expense currentExpense;
    private List<Category> categories;
    private Calendar selectedDateTime;

    private Expense deletedExpense; // For undo functionality

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_expense);

        // Initialize helpers
        dbHelper = DatabaseHelper.getInstance(this);
        sessionManager = new SessionManager(this);

        // Check authentication
        if (!sessionManager.isLoggedIn()) {
            finish();
            return;
        }

        // Get expense ID from intent
        int expenseId = getIntent().getIntExtra("expense_id", -1);
        if (expenseId == -1) {
            Toast.makeText(this, "Invalid expense", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Load expense from database
        loadExpense(expenseId);

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
        tilAmount = findViewById(R.id.til_amount);
        tilDescription = findViewById(R.id.til_description);
        etAmount = findViewById(R.id.et_amount);
        etDescription = findViewById(R.id.et_description);
        spinnerCategory = findViewById(R.id.spinner_category);
        btnSelectDate = findViewById(R.id.btn_select_date);
        btnSelectTime = findViewById(R.id.btn_select_time);
        btnDelete = findViewById(R.id.btn_delete);
        ivReceiptPreview = findViewById(R.id.iv_receipt_preview);
        fabUpdate = findViewById(R.id.fab_update);
    }

    /**
     * Load expense from database
     */
    private void loadExpense(int expenseId) {
        // Query database for expense
        List<Expense> allExpenses = dbHelper.getExpensesByUser(sessionManager.getUserId());

        for (Expense expense : allExpenses) {
            if (expense.getId() == expenseId) {
                currentExpense = expense;
                break;
            }
        }

        if (currentExpense == null) {
            Toast.makeText(this, "Expense not found", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    /**
     * Load categories from database
     */
    private void loadCategories() {
        categories = dbHelper.getAllCategories();

        ArrayAdapter<Category> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                categories
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);
    }

    /**
     * Pre-fill form with current expense data
     */
    private void prefillForm() {
        // Amount
        etAmount.setText(String.valueOf(currentExpense.getAmount()));

        // Category
        for (int i = 0; i < categories.size(); i++) {
            if (categories.get(i).getId() == currentExpense.getCategoryId()) {
                spinnerCategory.setSelection(i);
                break;
            }
        }

        // Date and time
        selectedDateTime = Calendar.getInstance();
        selectedDateTime.setTimeInMillis(currentExpense.getDate());
        updateDateTimeButtons();

        // Description
        if (currentExpense.getDescription() != null) {
            etDescription.setText(currentExpense.getDescription());
        }

        // Receipt preview (if available)
        if (currentExpense.getReceiptPath() != null && !currentExpense.getReceiptPath().isEmpty()) {
            ivReceiptPreview.setVisibility(View.VISIBLE);
            // TODO: Load image from path
        }
    }

    private void setupClickListeners() {
        // Date picker
        btnSelectDate.setOnClickListener(v -> showDatePicker());

        // Time picker
        btnSelectTime.setOnClickListener(v -> showTimePicker());

        // Update expense
        fabUpdate.setOnClickListener(v -> updateExpense());

        // Delete expense
        btnDelete.setOnClickListener(v -> showDeleteConfirmation());
    }

    /**
     * Show date picker dialog
     */
    private void showDatePicker() {
        int year = selectedDateTime.get(Calendar.YEAR);
        int month = selectedDateTime.get(Calendar.MONTH);
        int day = selectedDateTime.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    selectedDateTime.set(Calendar.YEAR, selectedYear);
                    selectedDateTime.set(Calendar.MONTH, selectedMonth);
                    selectedDateTime.set(Calendar.DAY_OF_MONTH, selectedDay);
                    updateDateTimeButtons();
                },
                year, month, day
        );

        datePickerDialog.show();
    }

    /**
     * Show time picker dialog
     */
    private void showTimePicker() {
        int hour = selectedDateTime.get(Calendar.HOUR_OF_DAY);
        int minute = selectedDateTime.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                (view, selectedHour, selectedMinute) -> {
                    selectedDateTime.set(Calendar.HOUR_OF_DAY, selectedHour);
                    selectedDateTime.set(Calendar.MINUTE, selectedMinute);
                    updateDateTimeButtons();
                },
                hour, minute, true
        );

        timePickerDialog.show();
    }

    /**
     * Update date and time button text
     */
    private void updateDateTimeButtons() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

        btnSelectDate.setText(dateFormat.format(selectedDateTime.getTime()));
        btnSelectTime.setText(timeFormat.format(selectedDateTime.getTime()));
    }

    /**
     * Validate and update expense
     */
    private void updateExpense() {
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
            tilAmount.setError("Invalid amount");
            return;
        }

        if (amount <= 0) {
            tilAmount.setError("Amount must be greater than 0");
            return;
        }

        tilAmount.setError(null);

        // Validate category
        if (spinnerCategory.getSelectedItem() == null) {
            Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show();
            return;
        }

        // Update expense object
        Category selectedCategory = (Category) spinnerCategory.getSelectedItem();
        currentExpense.setAmount(amount);
        currentExpense.setCategoryId(selectedCategory.getId());
        currentExpense.setDate(selectedDateTime.getTimeInMillis());
        currentExpense.setDescription(etDescription.getText().toString().trim());

        // Update in database
        int rowsAffected = dbHelper.updateExpense(currentExpense);

        if (rowsAffected > 0) {
            NumberFormat currencyFormat = NumberFormat.getInstance(new Locale("vi", "VN"));
            String formattedAmount = currencyFormat.format(amount) + "đ";

            Toast.makeText(this,
                    getString(R.string.expense_updated) + ": " + formattedAmount,
                    Toast.LENGTH_SHORT).show();

            finish();
        } else {
            Toast.makeText(this, "Failed to update expense", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Show delete confirmation dialog
     */
    private void showDeleteConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.expense_delete))
                .setMessage(getString(R.string.expense_delete_confirm))
                .setPositiveButton(getString(R.string.action_yes), (dialog, which) -> deleteExpense())
                .setNegativeButton(getString(R.string.action_no), null)
                .show();
    }

    /**
     * Delete expense with undo option
     */
    private void deleteExpense() {
        // Store for undo
        deletedExpense = new Expense(
                currentExpense.getId(),
                currentExpense.getUserId(),
                currentExpense.getCategoryId(),
                currentExpense.getCurrencyId(),
                currentExpense.getAmount(),
                currentExpense.getDate(),
                currentExpense.getDescription(),
                currentExpense.getReceiptPath(),
                currentExpense.getCreatedAt()
        );

        // Delete from database
        int rowsDeleted = dbHelper.deleteExpense(currentExpense.getId());

        if (rowsDeleted > 0) {
            // Show snackbar with undo option
            Snackbar snackbar = Snackbar.make(
                    findViewById(android.R.id.content),
                    getString(R.string.expense_deleted),
                    Snackbar.LENGTH_LONG
            );

            snackbar.setAction(getString(R.string.expense_undo), v -> undoDelete());

            snackbar.addCallback(new Snackbar.Callback() {
                @Override
                public void onDismissed(Snackbar transientBottomBar, int event) {
                    if (event != DISMISS_EVENT_ACTION) {
                        // Undo not pressed - finish activity
                        finish();
                    }
                }
            });

            snackbar.show();
        } else {
            Toast.makeText(this, "Failed to delete expense", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Undo delete operation
     */
    private void undoDelete() {
        if (deletedExpense != null) {
            long newId = dbHelper.insertExpense(deletedExpense);

            if (newId != -1) {
                deletedExpense.setId((int) newId);
                currentExpense = deletedExpense;

                Toast.makeText(this, "Expense restored", Toast.LENGTH_SHORT).show();

                // Refresh form
                prefillForm();
            } else {
                Toast.makeText(this, "Failed to restore expense", Toast.LENGTH_SHORT).show();
            }
        }
    }
}