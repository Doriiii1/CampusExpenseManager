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
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.example.campusexpensemanager.R;
import com.example.campusexpensemanager.models.Category;
import com.example.campusexpensemanager.models.Expense;
import com.example.campusexpensemanager.utils.DatabaseHelper;
import com.example.campusexpensemanager.utils.SessionManager;
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
 * AddExpenseActivity handles adding new expenses
 * Features: Amount input, Category selection, Date/Time picker, Receipt photo
 */
public class AddExpenseActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_CODE = 100;

    private TextInputLayout tilAmount, tilDescription;
    private TextInputEditText etAmount, etDescription;
    private Spinner spinnerCategory;
    private Button btnSelectDate, btnSelectTime, btnCaptureReceipt;
    private ImageView ivReceiptPreview;
    private FloatingActionButton fabSave;

    private DatabaseHelper dbHelper;
    private SessionManager sessionManager;

    private List<Category> categories;
    private Calendar selectedDateTime;
    private String receiptPhotoPath;

    private ActivityResultLauncher<Intent> cameraLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_expense);

        // Initialize helpers
        dbHelper = DatabaseHelper.getInstance(this);
        sessionManager = new SessionManager(this);

        // Check authentication
        if (!sessionManager.isLoggedIn()) {
            finish();
            return;
        }

        // Initialize date/time to current
        selectedDateTime = Calendar.getInstance();

        // Initialize views
        initializeViews();

        // Load categories
        loadCategories();

        // Setup camera launcher
        setupCameraLauncher();

        // Setup click listeners
        setupClickListeners();

        // Set initial date/time text
        updateDateTimeButtons();
    }

    private void initializeViews() {
        tilAmount = findViewById(R.id.til_amount);
        tilDescription = findViewById(R.id.til_description);
        etAmount = findViewById(R.id.et_amount);
        etDescription = findViewById(R.id.et_description);
        spinnerCategory = findViewById(R.id.spinner_category);
        btnSelectDate = findViewById(R.id.btn_select_date);
        btnSelectTime = findViewById(R.id.btn_select_time);
        btnCaptureReceipt = findViewById(R.id.btn_capture_receipt);
        ivReceiptPreview = findViewById(R.id.iv_receipt_preview);
        fabSave = findViewById(R.id.fab_save);
    }

    /**
     * Load categories from database into spinner
     */
    private void loadCategories() {
        categories = dbHelper.getAllCategories();

        if (categories.isEmpty()) {
            Toast.makeText(this, "No categories available", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create adapter with category names
        ArrayAdapter<Category> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                categories
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);
    }

    /**
     * Setup camera launcher for receipt photo
     */
    private void setupCameraLauncher() {
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        // Photo saved to receiptPhotoPath
                        ivReceiptPreview.setVisibility(View.VISIBLE);
                        ivReceiptPreview.setImageURI(Uri.fromFile(new File(receiptPhotoPath)));
                        Toast.makeText(this, "Receipt captured", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void setupClickListeners() {
        // Date picker
        btnSelectDate.setOnClickListener(v -> showDatePicker());

        // Time picker
        btnSelectTime.setOnClickListener(v -> showTimePicker());

        // Capture receipt
        btnCaptureReceipt.setOnClickListener(v -> captureReceipt());

        // Save expense
        fabSave.setOnClickListener(v -> saveExpense());
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
     * Launch camera intent to capture receipt
     */
    private void captureReceipt() {
        // Check camera permission first
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            // Request permission
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_CODE);
            return;
        }

        // Launch camera
        launchCamera();
    }

    /**
     * Launch camera with FileProvider
     */
    private void launchCamera() {
        try {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

            // Check if camera app exists
            if (takePictureIntent.resolveActivity(getPackageManager()) == null) {
                Toast.makeText(this, "No camera app found", Toast.LENGTH_SHORT).show();
                return;
            }

            // Create file to save photo
            File photoFile = createImageFile();
            if (photoFile != null) {
                receiptPhotoPath = photoFile.getAbsolutePath();

                Uri photoURI = FileProvider.getUriForFile(
                        this,
                        "com.example.campusexpensemanager.fileprovider",
                        photoFile
                );

                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                cameraLauncher.launch(takePictureIntent);
            } else {
                Toast.makeText(this, "Failed to create photo file", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Camera error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, launch camera
                launchCamera();
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Create temporary image file for receipt
     */
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

    /**
     * Validate and save expense to database
     */
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
        int categoryId = selectedCategory.getId();
        long dateTime = selectedDateTime.getTimeInMillis();
        String description = etDescription.getText().toString().trim();

        // Create expense object
        Expense expense = new Expense(userId, categoryId, amount, dateTime, description);
        expense.setReceiptPath(receiptPhotoPath);

        // Insert into database
        long expenseId = dbHelper.insertExpense(expense);

        if (expenseId != -1) {
            // Format amount for display
            NumberFormat currencyFormat = NumberFormat.getInstance(new Locale("vi", "VN"));
            String formattedAmount = currencyFormat.format(amount) + "đ";

            Toast.makeText(this,
                    getString(R.string.expense_added) + ": " + formattedAmount,
                    Toast.LENGTH_SHORT).show();

            // Return to expense list
            finish();
        } else {
            Toast.makeText(this, "Failed to add expense", Toast.LENGTH_SHORT).show();
        }
    }
}