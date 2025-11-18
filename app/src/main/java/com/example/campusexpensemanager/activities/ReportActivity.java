package com.example.campusexpensemanager.activities;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.campusexpensemanager.R;
import com.example.campusexpensemanager.models.Category;
import com.example.campusexpensemanager.models.Expense;
import com.example.campusexpensemanager.utils.DatabaseHelper;
import com.example.campusexpensemanager.utils.SessionManager;

import java.io.File;
import java.io.FileWriter;
import java.io.OutputStream;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * ReportActivity - FIXED Runtime Permission handling
 * Now properly requests WRITE_EXTERNAL_STORAGE for Android 6-9
 */
public class ReportActivity extends AppCompatActivity {

    private static final String CHANNEL_ID = "budget_alerts";
    private static final int NOTIFICATION_ID = 1001;

    // ✅ NEW: Permission request code
    private static final int STORAGE_PERMISSION_CODE = 100;

    private TextView tvDateRange, tvTotalExpense, tvExpenseCount, tvCategorySummary;
    private Button btnSelectStartDate, btnSelectEndDate, btnExportCSV, btnShareEmail;

    private DatabaseHelper dbHelper;
    private SessionManager sessionManager;

    private Calendar startDate, endDate;
    private SimpleDateFormat dateFormat;
    private NumberFormat currencyFormat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);

        // Initialize helpers
        dbHelper = DatabaseHelper.getInstance(this);
        sessionManager = new SessionManager(this);

        // Check authentication
        if (!sessionManager.isLoggedIn()) {
            finish();
            return;
        }

        // Initialize formatters
        dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        currencyFormat = NumberFormat.getInstance(new Locale("vi", "VN"));

        // Initialize dates (default: this month)
        initializeDefaultDates();

        // Initialize views
        initializeViews();

        // Setup click listeners
        setupClickListeners();

        // Generate initial report
        generateReport();
    }

    private void initializeViews() {
        tvDateRange = findViewById(R.id.tv_date_range);
        tvTotalExpense = findViewById(R.id.tv_total_expense);
        tvExpenseCount = findViewById(R.id.tv_expense_count);
        tvCategorySummary = findViewById(R.id.tv_category_summary);
        btnSelectStartDate = findViewById(R.id.btn_select_start_date);
        btnSelectEndDate = findViewById(R.id.btn_select_end_date);
        btnExportCSV = findViewById(R.id.btn_export_csv);
        btnShareEmail = findViewById(R.id.btn_share_email);
    }

    /**
     * Initialize date range to current month
     */
    private void initializeDefaultDates() {
        startDate = Calendar.getInstance();
        startDate.set(Calendar.DAY_OF_MONTH, 1);
        startDate.set(Calendar.HOUR_OF_DAY, 0);
        startDate.set(Calendar.MINUTE, 0);
        startDate.set(Calendar.SECOND, 0);

        endDate = Calendar.getInstance();
        endDate.set(Calendar.DAY_OF_MONTH, endDate.getActualMaximum(Calendar.DAY_OF_MONTH));
        endDate.set(Calendar.HOUR_OF_DAY, 23);
        endDate.set(Calendar.MINUTE, 59);
        endDate.set(Calendar.SECOND, 59);
    }

    private void setupClickListeners() {
        // Start date picker
        btnSelectStartDate.setOnClickListener(v -> showDatePicker(true));

        // End date picker
        btnSelectEndDate.setOnClickListener(v -> showDatePicker(false));

        // Export CSV
        btnExportCSV.setOnClickListener(v -> exportToCSV());

        // Share via email
        btnShareEmail.setOnClickListener(v -> shareViaEmail());
    }

    /**
     * Show date picker dialog
     */
    private void showDatePicker(boolean isStartDate) {
        Calendar calendar = isStartDate ? startDate : endDate;

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    calendar.set(Calendar.YEAR, year);
                    calendar.set(Calendar.MONTH, month);
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                    // Update button text
                    if (isStartDate) {
                        btnSelectStartDate.setText(dateFormat.format(startDate.getTime()));
                    } else {
                        btnSelectEndDate.setText(dateFormat.format(endDate.getTime()));
                    }

                    // Regenerate report
                    generateReport();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        datePickerDialog.show();
    }

    /**
     * Generate report summary
     */
    private void generateReport() {
        int userId = sessionManager.getUserId();
        List<Expense> allExpenses = dbHelper.getExpensesByUser(userId);

        // Filter expenses by date range
        double totalExpense = 0;
        int expenseCount = 0;
        Map<Integer, Double> categoryTotals = new HashMap<>();

        long startTime = startDate.getTimeInMillis();
        long endTime = endDate.getTimeInMillis();

        for (Expense expense : allExpenses) {
            if (expense.getDate() >= startTime && expense.getDate() <= endTime) {
                totalExpense += expense.getAmount();
                expenseCount++;

                // Aggregate by category
                int categoryId = expense.getCategoryId();
                categoryTotals.put(categoryId,
                        categoryTotals.getOrDefault(categoryId, 0.0) + expense.getAmount());
            }
        }

        // Update date range display
        String dateRangeText = dateFormat.format(startDate.getTime()) + " - " +
                dateFormat.format(endDate.getTime());
        tvDateRange.setText("Report Period: " + dateRangeText);

        // Update total expense
        String totalText = currencyFormat.format(totalExpense) + "đ";
        tvTotalExpense.setText("Total Expense: " + totalText);

        // Update expense count
        tvExpenseCount.setText("Number of Expenses: " + expenseCount);

        // Generate category summary
        StringBuilder categorySummary = new StringBuilder("Expenses by Category:\n\n");

        if (categoryTotals.isEmpty()) {
            categorySummary.append("No expenses in this period");
        } else {
            for (Map.Entry<Integer, Double> entry : categoryTotals.entrySet()) {
                Category category = dbHelper.getCategoryById(entry.getKey());
                String categoryName = category != null ? category.getName() : "Unknown";
                String amount = currencyFormat.format(entry.getValue()) + "đ";

                categorySummary.append("• ").append(categoryName)
                        .append(": ").append(amount).append("\n");
            }
        }

        tvCategorySummary.setText(categorySummary.toString());
    }

    /**
     * ✅ FIXED: Export CSV with proper permission handling
     */
    private void exportToCSV() {
        try {
            // Get filtered expenses
            List<Expense> expenses = getFilteredExpenses();

            if (expenses.isEmpty()) {
                Toast.makeText(this, "No expenses to export", Toast.LENGTH_SHORT).show();
                return;
            }

            // ✅ Check Android version and permissions
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ (Scoped Storage) - No permission needed
                performCSVExport(expenses);
            } else {
                // Android 6-9 - Need WRITE_EXTERNAL_STORAGE
                if (checkStoragePermission()) {
                    performCSVExport(expenses);
                } else {
                    requestStoragePermission();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to export: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * ✅ NEW: Check if WRITE_EXTERNAL_STORAGE permission is granted
     */
    private boolean checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ doesn't need this permission
            return true;
        }

        return ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * ✅ NEW: Request WRITE_EXTERNAL_STORAGE permission
     */
    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Not needed for Android 10+
            return;
        }

        // Show rationale if needed
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            Toast.makeText(this,
                    "Storage permission is needed to export CSV files to Downloads folder",
                    Toast.LENGTH_LONG).show();
        }

        // Request permission
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                STORAGE_PERMISSION_CODE);
    }

    /**
     * ✅ NEW: Handle permission result
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted - retry export
                Toast.makeText(this, "Permission granted. Exporting...",
                        Toast.LENGTH_SHORT).show();
                exportToCSV();
            } else {
                // Permission denied
                Toast.makeText(this,
                        "Permission denied. Cannot export to Downloads folder.",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * ✅ NEW: Perform actual CSV export (separated from permission logic)
     */
    private void performCSVExport(List<Expense> expenses) throws Exception {
        // Generate CSV content
        StringBuilder csv = new StringBuilder();
        csv.append("Category,Amount,Date,Description\n");

        for (Expense expense : expenses) {
            Category category = dbHelper.getCategoryById(expense.getCategoryId());
            String categoryName = category != null ? category.getName() : "Unknown";
            String amount = String.valueOf(expense.getAmount());
            String date = dateFormat.format(new Date(expense.getDate()));
            String description = expense.getDescription() != null ?
                    expense.getDescription().replace(",", ";") : "";

            csv.append(categoryName).append(",")
                    .append(amount).append(",")
                    .append(date).append(",")
                    .append(description).append("\n");
        }

        // Save to Downloads folder
        String fileName = "expense_report_" +
                new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                        .format(new Date()) + ".csv";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ (Scoped Storage)
            saveToDownloadsQ(fileName, csv.toString());
        } else {
            // Android 9 and below
            saveToDownloadsLegacy(fileName, csv.toString());
        }

        Toast.makeText(this, "Report saved to Downloads", Toast.LENGTH_LONG).show();
    }

    /**
     * Save CSV to Downloads (Android 10+)
     */
    private void saveToDownloadsQ(String fileName, String content) throws Exception {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
        values.put(MediaStore.Downloads.MIME_TYPE, "text/csv");
        values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

        Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);

        if (uri != null) {
            OutputStream outputStream = getContentResolver().openOutputStream(uri);
            if (outputStream != null) {
                outputStream.write(content.getBytes());
                outputStream.close();
            }
        }
    }

    /**
     * ✅ FIXED: Save CSV to Downloads (Android 6-9)
     * This method is now only called AFTER permission check
     */
    private void saveToDownloadsLegacy(String fileName, String content) throws Exception {
        File downloadsDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS);

        // Ensure directory exists
        if (!downloadsDir.exists()) {
            downloadsDir.mkdirs();
        }

        File file = new File(downloadsDir, fileName);

        FileWriter writer = new FileWriter(file);
        writer.write(content);
        writer.close();
    }

    /**
     * Share report via email
     */
    private void shareViaEmail() {
        try {
            // Generate CSV content
            List<Expense> expenses = getFilteredExpenses();

            if (expenses.isEmpty()) {
                Toast.makeText(this, "No expenses to share", Toast.LENGTH_SHORT).show();
                return;
            }

            // Create email intent
            String subject = String.format(getString(R.string.report_email_subject),
                    dateFormat.format(startDate.getTime()));

            StringBuilder body = new StringBuilder();
            body.append("Expense Report\n\n");
            body.append(tvDateRange.getText()).append("\n");
            body.append(tvTotalExpense.getText()).append("\n");
            body.append(tvExpenseCount.getText()).append("\n\n");
            body.append(tvCategorySummary.getText());

            Intent emailIntent = new Intent(Intent.ACTION_SEND);
            emailIntent.setType("text/plain");
            emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{sessionManager.getUserEmail()});
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
            emailIntent.putExtra(Intent.EXTRA_TEXT, body.toString());

            startActivity(Intent.createChooser(emailIntent, "Send report via..."));

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to share: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Get expenses filtered by date range
     */
    private List<Expense> getFilteredExpenses() {
        int userId = sessionManager.getUserId();
        List<Expense> allExpenses = dbHelper.getExpensesByUser(userId);

        // Filter by date range
        List<Expense> filtered = new java.util.ArrayList<>();
        long startTime = startDate.getTimeInMillis();
        long endTime = endDate.getTimeInMillis();

        for (Expense expense : allExpenses) {
            if (expense.getDate() >= startTime && expense.getDate() <= endTime) {
                filtered.add(expense);
            }
        }

        return filtered;
    }
}