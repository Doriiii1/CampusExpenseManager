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
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * AddExpenseActivity - Fully Localized
 * Updated to use resources (getString) instead of hardcoded text.
 */
public class AddExpenseActivity extends BaseActivity implements TemplateAdapter.OnTemplateClickListener {

    private static final int CAMERA_PERMISSION_CODE = 100;

    // Instance state keys
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

        // Restore instance state if available
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

        // Restore receipt preview if path exists
        if (receiptPhotoPath != null && !receiptPhotoPath.isEmpty()) {
            restoreReceiptPreview();
        }
    }

    /**
     * Save instance state to preserve data across config changes
     */
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_RECEIPT_PATH, receiptPhotoPath);
        outState.putLong(KEY_SELECTED_DATE, selectedDateTime.getTimeInMillis());
        outState.putInt(KEY_CURRENT_TYPE, currentType);
    }

    /**
     * Restore instance state (called automatically by Android)
     */
    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        receiptPhotoPath = savedInstanceState.getString(KEY_RECEIPT_PATH);
        long savedTime = savedInstanceState.getLong(KEY_SELECTED_DATE, System.currentTimeMillis());
        selectedDateTime.setTimeInMillis(savedTime);
        currentType = savedInstanceState.getInt(KEY_CURRENT_TYPE, Expense.TYPE_EXPENSE);

        updateUIForType();
        updateDateTimeButtons();
        restoreReceiptPreview();
    }

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
        btnCaptureReceipt = findViewById(R.id.btn_capture_receipt);
        ivReceiptPreview = findViewById(R.id.iv_receipt_preview);

        cbRecurring = findViewById(R.id.cb_recurring);
        spinnerRecurrencePeriod = findViewById(R.id.spinner_recurrence_period);

        recyclerTemplates = findViewById(R.id.recycler_templates);
        recyclerTemplates.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        fabSave = findViewById(R.id.fab_save);
        spinnerRecurrencePeriod.setVisibility(View.GONE);
    }

    private void loadCategories() {
        categories = dbHelper.getAllCategories();

        if (categories.isEmpty()) {
            // FIX: Localized string
            Toast.makeText(this, getString(R.string.msg_no_categories), Toast.LENGTH_SHORT).show();
            return;
        }

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
                        restoreReceiptPreview();
                        // FIX: Localized string
                        Toast.makeText(this, getString(R.string.msg_receipt_captured), Toast.LENGTH_SHORT).show();
                    }
                }
        );
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
        btnCaptureReceipt.setOnClickListener(v -> captureReceipt());

        cbRecurring.setOnCheckedChangeListener((buttonView, isChecked) -> {
            spinnerRecurrencePeriod.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });

        // FIX: Localized strings for recurring periods
        String[] periods = {
                getString(R.string.recurring_option_daily),
                getString(R.string.recurring_option_weekly),
                getString(R.string.recurring_option_monthly)
        };
        ArrayAdapter<String> periodAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, periods);
        periodAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRecurrencePeriod.setAdapter(periodAdapter);
        spinnerRecurrencePeriod.setSelection(2); // Default: Monthly

        fabSave.setOnClickListener(v -> saveExpense());
    }

    private void updateUIForType() {
        if (currentType == Expense.TYPE_INCOME) {
            tilAmount.setBoxStrokeColor(getResources().getColor(R.color.success));
            chipIncome.setChipBackgroundColorResource(R.color.success);
            chipExpense.setChipBackgroundColorResource(R.color.light_surface_variant);
            chipIncome.setChecked(true);
        } else {
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
                // FIX: Localized string
                Toast.makeText(this, getString(R.string.msg_no_camera_app), Toast.LENGTH_SHORT).show();
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
            // FIX: Localized string
            Toast.makeText(this, getString(R.string.msg_camera_error), Toast.LENGTH_SHORT).show();
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
                // FIX: Localized string
                Toast.makeText(this, getString(R.string.msg_permission_denied), Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onTemplateClick(ExpenseTemplate template) {
        for (int i = 0; i < categories.size(); i++) {
            if (categories.get(i).getId() == template.getCategoryId()) {
                spinnerCategory.setSelection(i);
                break;
            }
        }

        etDescription.setText(template.getName());
        if (template.getDefaultAmount() > 0) {
            etAmount.setText(String.valueOf(template.getDefaultAmount()));
        }
        etAmount.requestFocus();

        // FIX: Localized string
        Toast.makeText(this, getString(R.string.msg_template_applied, template.getName()), Toast.LENGTH_SHORT).show();
    }

    private void saveExpense() {
        String amountStr = etAmount.getText().toString().trim();
        if (amountStr.isEmpty()) {
            tilAmount.setError(getString(R.string.error_empty_field));
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            // FIX: Localized error
            tilAmount.setError(getString(R.string.err_invalid_amount));
            return;
        }

        if (amount <= 0) {
            // FIX: Localized error
            tilAmount.setError(getString(R.string.err_amount_zero));
            return;
        }
        tilAmount.setError(null);

        if (spinnerCategory.getSelectedItem() == null) {
            // FIX: Localized warning
            Toast.makeText(this, getString(R.string.msg_select_category), Toast.LENGTH_SHORT).show();
            return;
        }

        int userId = sessionManager.getUserId();
        Category selectedCategory = (Category) spinnerCategory.getSelectedItem();
        Currency selectedCurrency = (Currency) spinnerCurrency.getSelectedItem();
        long dateTime = selectedDateTime.getTimeInMillis();
        String description = etDescription.getText().toString().trim();

        Expense expense = new Expense(userId, selectedCategory.getId(),
                amount, dateTime, description, currentType);
        expense.setCurrencyId(selectedCurrency.getId());
        expense.setReceiptPath(receiptPhotoPath);

        if (cbRecurring.isChecked()) {
            expense.setIsRecurring(true);

            // Map localized period back to DB value (english) for consistency or save logic
            // Ideally, save constants like "daily", "weekly", "monthly" regardless of display language
            // Here we assume the spinner index 0=daily, 1=weekly, 2=monthly
            String periodValue = "monthly";
            switch (spinnerRecurrencePeriod.getSelectedItemPosition()) {
                case 0: periodValue = "daily"; break;
                case 1: periodValue = "weekly"; break;
                case 2: periodValue = "monthly"; break;
            }
            expense.setRecurrencePeriod(periodValue);

            Calendar nextOcc = (Calendar) selectedDateTime.clone();
            switch (periodValue) {
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

        long expenseId = dbHelper.insertExpense(expense);

        if (expenseId != -1) {
            // FIX: Localized type text
            String typeText = currentType == Expense.TYPE_INCOME ?
                    getString(R.string.label_income) : getString(R.string.label_expense);
            String formattedAmount = selectedCurrency.formatAmount(amount);

            Toast.makeText(this, getString(R.string.msg_transaction_added, typeText, formattedAmount),
                    Toast.LENGTH_SHORT).show();

            setResult(RESULT_OK);
            finish();
        } else {
            String typeText = currentType == Expense.TYPE_INCOME ?
                    getString(R.string.label_income) : getString(R.string.label_expense);
            Toast.makeText(this, getString(R.string.msg_transaction_failed, typeText),
                    Toast.LENGTH_SHORT).show();
        }
    }
}