package com.example.campusexpensemanager.activities;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.campusexpensemanager.R;
import com.example.campusexpensemanager.models.Category;
import com.example.campusexpensemanager.models.Currency;
import com.example.campusexpensemanager.models.Expense;
import com.example.campusexpensemanager.utils.DatabaseHelper;
import com.example.campusexpensemanager.utils.SessionManager;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
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
 * EditExpenseActivity - Enhanced for Sprint 5
 * NEW: Income/Expense toggle, Currency, Recurring support
 */
public class EditExpenseActivity extends AppCompatActivity {

    // Type toggle
    private ChipGroup chipGroupType;
    private Chip chipExpense, chipIncome;

    private TextInputLayout tilAmount, tilDescription;
    private TextInputEditText etAmount, etDescription;
    private Spinner spinnerCategory, spinnerCurrency;
    private Button btnSelectDate, btnSelectTime, btnDelete;
    private ImageView ivReceiptPreview;

    // Recurring fields
    private CheckBox cbRecurring;
    private Spinner spinnerRecurrencePeriod;

    private FloatingActionButton fabUpdate;

    private DatabaseHelper dbHelper;
    private SessionManager sessionManager;

    private Expense currentExpense;
    private List<Category> categories;
    private List<Currency> currencies;
    private Calendar selectedDateTime;
    private int currentType;

    private Expense deletedExpense;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_expense);

        dbHelper = DatabaseHelper.getInstance(this);
        sessionManager = new SessionManager(this);

        if (!sessionManager.isLoggedIn()) {
            finish();
            return;
        }

        int expenseId = getIntent().getIntExtra("expense_id", -1);
        if (expenseId == -1) {
            Toast.makeText(this, "Invalid expense", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadExpense(expenseId);
        initializeViews();
        loadCategories();
        loadCurrencies();
        prefillForm();
        setupClickListeners();
    }

    private void initializeViews() {
        chipGroupType = findViewById(R.id.chip_group_type);
        chipExpense = findViewById(R.id.chip_expense);
        chipIncome = findViewById(R.id.chip_income);

        tilAmount = findViewById(R.id.til_amount);
        tilDescription = findViewById(R.id.til_description);
        etAmount = findViewById(R.id.et_amount);
        etDescription = findViewById(R.id.et_description);
        spinnerCategory = findViewById(R.id.spinner_category);
        spinnerCurrency = findViewById(R.id.spinner_currency);
        btnSelectDate = findViewById(R.id.btn_select_date);
        btnSelectTime = findViewById(R.id.btn_select_time);
        btnDelete = findViewById(R.id.btn_delete);
        ivReceiptPreview = findViewById(R.id.iv_receipt_preview);

        cbRecurring = findViewById(R.id.cb_recurring);
        spinnerRecurrencePeriod = findViewById(R.id.spinner_recurrence_period);

        fabUpdate = findViewById(R.id.fab_update);

        // Setup recurrence period spinner
        String[] periods = {"Daily", "Weekly", "Monthly"};
        ArrayAdapter<String> periodAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, periods);
        periodAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRecurrencePeriod.setAdapter(periodAdapter);
    }

    private void loadExpense(int expenseId) {
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

    private void loadCategories() {
        categories = dbHelper.getAllCategories();
        ArrayAdapter<Category> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);
    }

    private void loadCurrencies() {
        currencies = new java.util.ArrayList<>();
        currencies.add(new Currency(1, "VND", 1.0, System.currentTimeMillis()));
        currencies.add(new Currency(2, "USD", 24000.0, System.currentTimeMillis()));

        ArrayAdapter<Currency> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, currencies);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCurrency.setAdapter(adapter);
    }

    private void prefillForm() {
        // Type
        currentType = currentExpense.getType();
        if (currentType == Expense.TYPE_INCOME) {
            chipIncome.setChecked(true);
        } else {
            chipExpense.setChecked(true);
        }
        updateUIForType();

        // Amount
        etAmount.setText(String.valueOf(currentExpense.getAmount()));

        // Category
        for (int i = 0; i < categories.size(); i++) {
            if (categories.get(i).getId() == currentExpense.getCategoryId()) {
                spinnerCategory.setSelection(i);
                break;
            }
        }

        // Currency
        for (int i = 0; i < currencies.size(); i++) {
            if (currencies.get(i).getId() == currentExpense.getCurrencyId()) {
                spinnerCurrency.setSelection(i);
                break;
            }
        }

        // Date/Time
        selectedDateTime = Calendar.getInstance();
        selectedDateTime.setTimeInMillis(currentExpense.getDate());
        updateDateTimeButtons();

        // Description
        if (currentExpense.getDescription() != null) {
            etDescription.setText(currentExpense.getDescription());
        }

        // Recurring
        cbRecurring.setChecked(currentExpense.isRecurring());
        if (currentExpense.isRecurring()) {
            spinnerRecurrencePeriod.setVisibility(View.VISIBLE);
            String period = currentExpense.getRecurrencePeriod();
            if (period != null) {
                if (period.equalsIgnoreCase("daily")) {
                    spinnerRecurrencePeriod.setSelection(0);
                } else if (period.equalsIgnoreCase("weekly")) {
                    spinnerRecurrencePeriod.setSelection(1);
                } else {
                    spinnerRecurrencePeriod.setSelection(2);
                }
            }
        }

        // Receipt
        if (currentExpense.getReceiptPath() != null && !currentExpense.getReceiptPath().isEmpty()) {
            ivReceiptPreview.setVisibility(View.VISIBLE);
        }
    }

    private void setupClickListeners() {
        chipGroupType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chip_expense) {
                currentType = Expense.TYPE_EXPENSE;
            } else if (checkedId == R.id.chip_income) {
                currentType = Expense.TYPE_INCOME;
            }
            updateUIForType();
        });

        btnSelectDate.setOnClickListener(v -> showDatePicker());
        btnSelectTime.setOnClickListener(v -> showTimePicker());
        fabUpdate.setOnClickListener(v -> updateExpense());
        btnDelete.setOnClickListener(v -> showDeleteConfirmation());

        cbRecurring.setOnCheckedChangeListener((buttonView, isChecked) -> {
            spinnerRecurrencePeriod.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });
    }

    private void updateUIForType() {
        if (currentType == Expense.TYPE_INCOME) {
            tilAmount.setBoxStrokeColor(getResources().getColor(R.color.success));
            chipIncome.setChipBackgroundColorResource(R.color.success);
            chipExpense.setChipBackgroundColorResource(R.color.light_surface_variant);
        } else {
            tilAmount.setBoxStrokeColor(getResources().getColor(R.color.error));
            chipExpense.setChipBackgroundColorResource(R.color.error);
            chipIncome.setChipBackgroundColorResource(R.color.light_surface_variant);
        }
    }

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

    private void updateDateTimeButtons() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

        btnSelectDate.setText(dateFormat.format(selectedDateTime.getTime()));
        btnSelectTime.setText(timeFormat.format(selectedDateTime.getTime()));
    }

    private void updateExpense() {
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

        if (spinnerCategory.getSelectedItem() == null) {
            Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show();
            return;
        }

        Category selectedCategory = (Category) spinnerCategory.getSelectedItem();
        Currency selectedCurrency = (Currency) spinnerCurrency.getSelectedItem();

        currentExpense.setType(currentType);
        currentExpense.setAmount(amount);
        currentExpense.setCategoryId(selectedCategory.getId());
        currentExpense.setCurrencyId(selectedCurrency.getId());
        currentExpense.setDate(selectedDateTime.getTimeInMillis());
        currentExpense.setDescription(etDescription.getText().toString().trim());

        // Handle recurring
        currentExpense.setIsRecurring(cbRecurring.isChecked());
        if (cbRecurring.isChecked()) {
            String periodText = spinnerRecurrencePeriod.getSelectedItem().toString().toLowerCase();
            currentExpense.setRecurrencePeriod(periodText);

            Calendar nextOcc = (Calendar) selectedDateTime.clone();
            switch (periodText) {
                case "daily":
                    nextOcc.add(Calendar.DAY_OF_MONTH, 1);
                    break;
                case "weekly":
                    nextOcc.add(Calendar.WEEK_OF_YEAR, 1);
                    break;
                case "monthly":
                    nextOcc.add(Calendar.MONTH, 1);
                    break;
            }
            currentExpense.setNextOccurrenceDate(nextOcc.getTimeInMillis());
        } else {
            currentExpense.setRecurrencePeriod(null);
            currentExpense.setNextOccurrenceDate(0);
        }

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

    private void showDeleteConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.expense_delete))
                .setMessage(getString(R.string.expense_delete_confirm))
                .setPositiveButton(getString(R.string.action_yes), (dialog, which) -> deleteExpense())
                .setNegativeButton(getString(R.string.action_no), null)
                .show();
    }

    private void deleteExpense() {
        deletedExpense = new Expense(
                currentExpense.getId(),
                currentExpense.getUserId(),
                currentExpense.getCategoryId(),
                currentExpense.getCurrencyId(),
                currentExpense.getAmount(),
                currentExpense.getDate(),
                currentExpense.getDescription(),
                currentExpense.getReceiptPath(),
                currentExpense.getCreatedAt(),
                currentExpense.getType()
        );

        int rowsDeleted = dbHelper.deleteExpense(currentExpense.getId());

        if (rowsDeleted > 0) {
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
                        finish();
                    }
                }
            });

            snackbar.show();
        } else {
            Toast.makeText(this, "Failed to delete expense", Toast.LENGTH_SHORT).show();
        }
    }

    private void undoDelete() {
        if (deletedExpense != null) {
            long newId = dbHelper.insertExpense(deletedExpense);

            if (newId != -1) {
                deletedExpense.setId((int) newId);
                currentExpense = deletedExpense;
                Toast.makeText(this, "Expense restored", Toast.LENGTH_SHORT).show();
                prefillForm();
            } else {
                Toast.makeText(this, "Failed to restore expense", Toast.LENGTH_SHORT).show();
            }
        }
    }
}