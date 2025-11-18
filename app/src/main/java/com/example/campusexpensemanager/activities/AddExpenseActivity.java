package com.example.campusexpensemanager.activities;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campusexpensemanager.R;
import com.example.campusexpensemanager.adapters.TemplateAdapter;
import com.example.campusexpensemanager.models.Category;
import com.example.campusexpensemanager.models.Currency;
import com.example.campusexpensemanager.models.Expense;
import com.example.campusexpensemanager.models.ExpenseTemplate;
import com.example.campusexpensemanager.utils.DatabaseHelper;
import com.example.campusexpensemanager.utils.SessionManager;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.File;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * AddExpenseActivity - FIXED for Camera Data Loss (Priority 1.2)
 * Now properly saves/restores receiptPhotoPath in onSaveInstanceState
 */
public class AddExpenseActivity extends AppCompatActivity implements TemplateAdapter.OnTemplateClickListener {

    private static final int CAMERA_PERMISSION_CODE = 100;

    // ✅ NEW: Instance state keys
    private static final String KEY_RECEIPT_PATH = "receipt_photo_path";
    private static final String KEY_SELECTED_DATE = "selected_date_time";
    private static final String KEY_CURRENT_TYPE = "current_type";

    // Type toggle
    private ChipGroup chipGroupType;
    private Chip chipExpense, chipIncome;

    // Form fields
    private TextInputLayout tilAmount, tilDescription;
    private TextInputEditText etAmount, etDescription;
    private Spinner spinnerCategory, spinnerCurrency;
    private Button btnSelectDate, btnSelectTime, btnCaptureReceipt;
    private ImageView ivReceiptPreview;

    // Recurring fields
    private CheckBox cbRecurring;
    private Spinner spinnerRecurrencePeriod;

    // Quick templates
    private RecyclerView recyclerTemplates;
    private TemplateAdapter templateAdapter;

    private FloatingActionButton fabSave;

    private DatabaseHelper dbHelper;
    private SessionManager sessionManager;

    private List<Category> categories;
    private List<Currency> currencies;
    private List<ExpenseTemplate> templates;

    private Calendar selectedDateTime;
    private String receiptPhotoPath;
    private int currentType = Expense.TYPE_EXPENSE; // Default: Expense

    private ActivityResultLauncher<Intent> cameraLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_expense);

        // Initialize helpers
        dbHelper = DatabaseHelper.getInstance(this);
        sessionManager = new SessionManager(this);

        if (!sessionManager.isLoggedIn()) {
            finish();
            return;
        }

        // Initialize date/time
        selectedDateTime = Calendar.getInstance();

        // ✅ FIXED: Restore instance state if available
        if (savedInstanceState != null) {
            receiptPhotoPath = savedInstanceState.getString(KEY_RECEIPT_PATH);
            long savedTime = savedInstanceState.getLong(KEY_SELECTED_DATE,
                    System.currentTimeMillis());
            selectedDateTime.setTimeInMillis(savedTime);
            currentType = savedInstanceState.getInt(KEY_CURRENT_TYPE, Expense.TYPE_EXPENSE);
        }

        // Initialize views
        initializeViews();

        // Load data
        loadCategories();
        loadCurrencies();
        loadTemplates();

        // Setup camera launcher
        setupCameraLauncher();

        // Setup listeners
        setupClickListeners();

        // Set initial state
        updateUIForType();
        updateDateTimeButtons();

        // ✅ FIXED: Restore receipt preview if path exists
        if (receiptPhotoPath != null && !receiptPhotoPath.isEmpty()) {
            restoreReceiptPreview();
        }
    }

    /**
     * ✅ NEW: Save instance state to preserve data across config changes
     */
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        // Save receipt photo path (CRITICAL for camera restoration)
        outState.putString(KEY_RECEIPT_PATH, receiptPhotoPath);

        // Save selected date/time
        outState.putLong(KEY_SELECTED_DATE, selectedDateTime.getTimeInMillis());

        // Save current type (Income/Expense)
        outState.putInt(KEY_CURRENT_TYPE, currentType);
    }

    /**
     * ✅ NEW: Restore instance state (called automatically by Android)
     */
    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        // Restore receipt path
        receiptPhotoPath = savedInstanceState.getString(KEY_RECEIPT_PATH);

        // Restore date/time
        long savedTime = savedInstanceState.getLong(KEY_SELECTED_DATE,
                System.currentTimeMillis());
        selectedDateTime.setTimeInMillis(savedTime);

        // Restore type
        currentType = savedInstanceState.getInt(KEY_CURRENT_TYPE, Expense.TYPE_EXPENSE);

        // Update UI with restored data
        updateUIForType();
        updateDateTimeButtons();
        restoreReceiptPreview();
    }

    /**
     * ✅ NEW: Restore receipt preview from saved path
     */
    private void restoreReceiptPreview() {
        if (receiptPhotoPath == null || receiptPhotoPath.isEmpty()) {
            ivReceiptPreview.setVisibility(View.GONE);
            return;
        }

        try {
            File photoFile = new File(receiptPhotoPath);
            if (photoFile.exists()) {
                ivReceiptPreview.setVisibility(View.VISIBLE);
                ivReceiptPreview.setImageURI(Uri.fromFile(photoFile));
            } else {
                // File was deleted, clear the path
                receiptPhotoPath = null;
                ivReceiptPreview.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            e.printStackTrace();
            receiptPhotoPath = null;
            ivReceiptPreview.setVisibility(View.GONE);
        }
    }

    private void initializeViews() {
        // Type toggle
        chipGroupType = findViewById(R.id.chip_group_type);
        chipExpense = findViewById(R.id.chip_expense);
        chipIncome = findViewById(R.id.chip_income);

        // Form fields
        tilAmount = findViewById(R.id.til_amount);
        tilDescription = findViewById(R.id.til_description);
        etAmount = findViewById(R.id.et_amount);
        etDescription = findViewById(R.id.et_description);
        spinnerCategory = findViewById(R.id.spinner_category);
        spinnerCurrency = findViewById(R.id.spinner_currency);
        btnSelectDate = findViewById(R.id.btn_select_date);
        btnSelectTime = findViewById(R.id.btn_select_time);
        btnCaptureReceipt = findViewById(R.id.btn_capture_receipt);
        ivReceiptPreview = findViewById(R.id.iv_receipt_preview);

        // Recurring
        cbRecurring = findViewById(R.id.cb_recurring);
        spinnerRecurrencePeriod = findViewById(R.id.spinner_recurrence_period);

        // Templates
        recyclerTemplates = findViewById(R.id.recycler_templates);
        recyclerTemplates.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        fabSave = findViewById(R.id.fab_save);

        // Default: Hide recurring period spinner
        spinnerRecurrencePeriod.setVisibility(View.GONE);
    }

    private void loadCategories() {
        categories = dbHelper.getAllCategories();

        if (categories.isEmpty()) {
            Toast.makeText(this, "No categories available", Toast.LENGTH_SHORT).show();
            return;
        }

        ArrayAdapter<Category> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);
    }

    private void loadCurrencies() {
        // Mock currencies (should query from DB in real app)
        currencies = new java.util.ArrayList<>();
        currencies.add(new Currency(1, "VND", 1.0, System.currentTimeMillis()));
        currencies.add(new Currency(2, "USD", 24000.0, System.currentTimeMillis()));

        ArrayAdapter<Currency> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, currencies);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCurrency.setAdapter(adapter);
    }

    private void loadTemplates() {
        templates = dbHelper.getAllTemplates();

        if (!templates.isEmpty()) {
            templateAdapter = new TemplateAdapter(this, templates, this);
            recyclerTemplates.setAdapter(templateAdapter);
            recyclerTemplates.setVisibility(View.VISIBLE);
        } else {
            recyclerTemplates.setVisibility(View.GONE);
        }
    }

    private void setupCameraLauncher() {
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        // ✅ receiptPhotoPath already saved in onSaveInstanceState
                        restoreReceiptPreview();
                        Toast.makeText(this, "Receipt captured", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void setupClickListeners() {
        // Type toggle
        chipGroupType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chip_expense) {
                currentType = Expense.TYPE_EXPENSE;
            } else if (checkedId == R.id.chip_income) {
                currentType = Expense.TYPE_INCOME;
            }
            updateUIForType();
        });

        // Date/Time pickers
        btnSelectDate.setOnClickListener(v -> showDatePicker());
        btnSelectTime.setOnClickListener(v -> showTimePicker());

        // Receipt capture
        btnCaptureReceipt.setOnClickListener(v -> captureReceipt());

        // Recurring checkbox
        cbRecurring.setOnCheckedChangeListener((buttonView, isChecked) -> {
            spinnerRecurrencePeriod.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });

        // Setup recurrence period spinner
        String[] periods = {"Daily", "Weekly", "Monthly"};
        ArrayAdapter<String> periodAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, periods);
        periodAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRecurrencePeriod.setAdapter(periodAdapter);
        spinnerRecurrencePeriod.setSelection(2); // Default: Monthly

        // Save button
        fabSave.setOnClickListener(v -> saveExpense());
    }

    /**
     * Update UI colors based on Expense/Income type
     */
    private void updateUIForType() {
        if (currentType == Expense.TYPE_INCOME) {
            // Green theme for Income
            tilAmount.setBoxStrokeColor(getResources().getColor(R.color.success));
            chipIncome.setChipBackgroundColorResource(R.color.success);
            chipExpense.setChipBackgroundColorResource(R.color.light_surface_variant);
            chipIncome.setChecked(true);
        } else {
            // Red theme for Expense
            tilAmount.setBoxStrokeColor(getResources().getColor(R.color.error));
            chipExpense.setChipBackgroundColorResource(R.color.error);
            chipIncome.setChipBackgroundColorResource(R.color.light_surface_variant);
            chipExpense.setChecked(true);
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

    private void captureReceipt() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
            return;
        }
        launchCamera();
    }

    private void launchCamera() {
        try {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

            if (takePictureIntent.resolveActivity(getPackageManager()) == null) {
                Toast.makeText(this, "No camera app found", Toast.LENGTH_SHORT).show();
                return;
            }

            File photoFile = createImageFile();
            if (photoFile != null) {
                receiptPhotoPath = photoFile.getAbsolutePath();
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.campusexpensemanager.fileprovider", photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                cameraLauncher.launch(takePictureIntent);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Camera error", Toast.LENGTH_SHORT).show();
        }
    }

    private File createImageFile() {
        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                    .format(System.currentTimeMillis());
            String imageFileName = "RECEIPT_" + timeStamp + "_";
            File storageDir = getExternalFilesDir("receipts");

            if (storageDir != null && !storageDir.exists()) {
                storageDir.mkdirs();
            }
            return File.createTempFile(imageFileName, ".jpg", storageDir);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchCamera();
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Template click handler - Pre-fill form
     */
    @Override
    public void onTemplateClick(ExpenseTemplate template) {
        // Pre-fill category
        for (int i = 0; i < categories.size(); i++) {
            if (categories.get(i).getId() == template.getCategoryId()) {
                spinnerCategory.setSelection(i);
                break;
            }
        }

        // Pre-fill description
        etDescription.setText(template.getName());

        // Pre-fill amount (optional)
        if (template.getDefaultAmount() > 0) {
            etAmount.setText(String.valueOf(template.getDefaultAmount()));
        }

        // Focus on amount field
        etAmount.requestFocus();

        Toast.makeText(this, "Template applied: " + template.getName(), Toast.LENGTH_SHORT).show();
    }

    private void saveExpense() {
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

        // Get form data
        int userId = sessionManager.getUserId();
        Category selectedCategory = (Category) spinnerCategory.getSelectedItem();
        Currency selectedCurrency = (Currency) spinnerCurrency.getSelectedItem();
        long dateTime = selectedDateTime.getTimeInMillis();
        String description = etDescription.getText().toString().trim();

        // Create expense object
        Expense expense = new Expense(userId, selectedCategory.getId(),
                amount, dateTime, description, currentType);
        expense.setCurrencyId(selectedCurrency.getId());
        expense.setReceiptPath(receiptPhotoPath);

        // Handle recurring
        if (cbRecurring.isChecked()) {
            expense.setIsRecurring(true);

            String periodText = spinnerRecurrencePeriod.getSelectedItem().toString().toLowerCase();
            expense.setRecurrencePeriod(periodText);

            // Calculate next occurrence
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
            expense.setNextOccurrenceDate(nextOcc.getTimeInMillis());
        }

        // Insert into database
        long expenseId = dbHelper.insertExpense(expense);

        if (expenseId != -1) {
            String typeText = currentType == Expense.TYPE_INCOME ? "Income" : "Expense";
            String formattedAmount = selectedCurrency.formatAmount(amount);

            Toast.makeText(this, typeText + " added: " + formattedAmount,
                    Toast.LENGTH_SHORT).show();

            // Set result OK to notify ExpenseListActivity to refresh
            setResult(RESULT_OK);
            finish();
        } else {
            Toast.makeText(this, "Failed to add " +
                            (currentType == Expense.TYPE_INCOME ? "income" : "expense"),
                    Toast.LENGTH_SHORT).show();
        }
    }
}