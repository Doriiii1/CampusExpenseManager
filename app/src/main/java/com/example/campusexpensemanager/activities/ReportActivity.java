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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.io.font.constants.StandardFonts;

import androidx.appcompat.app.AlertDialog;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.PercentFormatter;
import android.graphics.Color;
import androidx.core.content.ContextCompat;

/**
 * ReportActivity - FIXED Runtime Permission handling
 * Now properly requests WRITE_EXTERNAL_STORAGE for Android 6-9
 */
public class    ReportActivity extends BaseActivity {

    private static final String CHANNEL_ID = "budget_alerts";
    private static final int NOTIFICATION_ID = 1001;

    // ✅ NEW: Permission request code
    private static final int STORAGE_PERMISSION_CODE = 100;

    private TextView tvDateRange, tvTotalExpense, tvExpenseCount, tvCategorySummary;
    private Button btnSelectStartDate, btnSelectEndDate, btnExportCSV, btnShareEmail, btnExportPDF;

    private PieChart pieChart;
    private LineChart lineChart;

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
        btnExportPDF = findViewById(R.id.btn_export_pdf);
        pieChart = findViewById(R.id.pie_chart);
        lineChart = findViewById(R.id.line_chart);
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
        btnExportCSV.setOnClickListener(v -> showExportFormatDialog());

        btnExportPDF.setOnClickListener(v -> exportToPDF());

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
     * ✅ FIX: Generate report summary with localized strings
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
                // Chỉ cộng tổng chi tiêu (Expense), không cộng thu nhập vào "Total Expense"
                if (expense.getType() == Expense.TYPE_EXPENSE) {
                    totalExpense += expense.getAmount();

                    // Aggregate by category
                    int categoryId = expense.getCategoryId();
                    categoryTotals.put(categoryId,
                            categoryTotals.getOrDefault(categoryId, 0.0) + expense.getAmount());
                }
                expenseCount++; // Đếm tổng số giao dịch (cả thu lẫn chi)
            }
        }

        // Update date range display
        String dateRangeText = dateFormat.format(startDate.getTime()) + " - " +
                dateFormat.format(endDate.getTime());
        // "Date Range: 01 Jan - 31 Jan"
        tvDateRange.setText(getString(R.string.report_date_range) + " " + dateRangeText);

        // Update total expense
        String totalText = currencyFormat.format(totalExpense) + "đ";
        // "Total Expense: 500.000đ"
        tvTotalExpense.setText(getString(R.string.report_total_expense_label) + " " + totalText);

        // Update expense count
        // "Number of Expenses: 15"
        tvExpenseCount.setText(getString(R.string.report_expense_count_label) + " " + expenseCount);

        // Generate category summary
        StringBuilder categorySummary = new StringBuilder();

        if (categoryTotals.isEmpty()) {
            categorySummary.append(getString(R.string.msg_no_data_period));
        } else {
            // "Expenses by Category:\n"
            categorySummary.append(getString(R.string.report_category_summary_default));

            for (Map.Entry<Integer, Double> entry : categoryTotals.entrySet()) {
                Category category = dbHelper.getCategoryById(entry.getKey());
                String categoryName;
                if (category != null) {
                    categoryName = DatabaseHelper.getLocalizedCategoryName(this, category.getName());
                } else {
                    categoryName = getString(R.string.cat_unknown);
                }

                String amount = currencyFormat.format(entry.getValue()) + "đ";
                categorySummary.append("• ").append(categoryName)
                        .append(": ").append(amount).append("\n");
            }
        }

        tvCategorySummary.setText(categorySummary.toString());
        setupPieChart(allExpenses, startTime, endTime);
        setupLineChart(allExpenses);
    }

    /**
     * ✅ NEW: Show dialog to select export format (CSV or PDF)
     */
    private void showExportFormatDialog() {
        String[] options = {
                getString(R.string.report_export_csv_option), // "Export as CSV"
                getString(R.string.report_export_pdf_option)  // "Export as PDF"
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.report_export_format_title))
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        exportToCSV();
                    } else {
                        exportToPDF();
                    }
                })
                .setNegativeButton(getString(R.string.action_cancel), null)
                .show();
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
     * ✅ FIX: Export CSV with localized headers
     */
    private void performCSVExport(List<Expense> expenses) throws Exception {
        // Generate CSV content with localized headers
        StringBuilder csv = new StringBuilder();
        csv.append(getString(R.string.header_category)).append(",")
                .append(getString(R.string.header_amount)).append(",")
                .append(getString(R.string.header_date)).append(",")
                .append(getString(R.string.header_description)).append("\n");

        for (Expense expense : expenses) {
            Category category = dbHelper.getCategoryById(expense.getCategoryId());
            String categoryName = (category != null) ?
                    DatabaseHelper.getLocalizedCategoryName(this, category.getName()) : getString(R.string.cat_unknown);

            // Clean category name (remove commas to avoid breaking CSV)
            categoryName = categoryName.replace(",", " ");

            String amount = String.valueOf(expense.getAmount());
            String date = dateFormat.format(new Date(expense.getDate()));
            String description = expense.getDescription() != null ?
                    expense.getDescription().replace(",", ";").replace("\n", " ") : "";

            csv.append(categoryName).append(",")
                    .append(amount).append(",")
                    .append(date).append(",")
                    .append(description).append("\n");
        }

        // ... (Phần lưu file giữ nguyên) ...
        // Đoạn cuối gọi hàm lưu:
        String fileName = "expense_report_" +
                new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".csv";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveToDownloadsQ(fileName, csv.toString());
        } else {
            saveToDownloadsLegacy(fileName, csv.toString());
        }

        Toast.makeText(this, getString(R.string.report_exported), Toast.LENGTH_LONG).show();
    }

    /**
     * ✅ NEW: Export report to PDF
     */
    private void exportToPDF() {
        try {
            // Get filtered expenses
            List<Expense> expenses = getFilteredExpenses();

            if (expenses.isEmpty()) {
                Toast.makeText(this, "No expenses to export", Toast.LENGTH_SHORT).show();
                return;
            }

            // Check Android version and permissions
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ (Scoped Storage) - No permission needed
                performPDFExport(expenses);
            } else {
                // Android 6-9 - Need WRITE_EXTERNAL_STORAGE
                if (checkStoragePermission()) {
                    performPDFExport(expenses);
                } else {
                    requestStoragePermission();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to export PDF: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * ✅ NEW: Perform actual PDF export
     */
    private void performPDFExport(List<Expense> expenses) {
        try {
            // Generate filename
            String fileName = "expense_report_" +
                    new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                            .format(new Date()) + ".pdf";

            // Get output stream based on Android version
            OutputStream outputStream;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ (Scoped Storage)
                ContentValues values = new ContentValues();
                values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
                values.put(MediaStore.Downloads.MIME_TYPE, "application/pdf");
                values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

                Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (uri == null) {
                    throw new Exception("Failed to create file URI");
                }
                outputStream = getContentResolver().openOutputStream(uri);
            } else {
                // Android 9 and below
                File downloadsDir = Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS);
                if (!downloadsDir.exists()) {
                    downloadsDir.mkdirs();
                }
                File file = new File(downloadsDir, fileName);
                outputStream = new java.io.FileOutputStream(file);
            }

            if (outputStream == null) {
                throw new Exception("Failed to create output stream");
            }

            // Create PDF document
            PdfWriter writer = new PdfWriter(outputStream);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);

            // Set margins
            document.setMargins(40, 40, 40, 40);

            // Create fonts
            PdfFont boldFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            PdfFont regularFont = PdfFontFactory.createFont(StandardFonts.HELVETICA);

            // ===== HEADER SECTION =====
            Paragraph title = new Paragraph(getString(R.string.pdf_title))
                    .setFont(boldFont)
                    .setFontSize(20)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(20)
                    .setFontColor(new DeviceRgb(15, 136, 241)); // Primary blue

            document.add(title);

            // Date range: "Period: 01 Jan - 31 Jan"
            String dateRangeText = dateFormat.format(startDate.getTime()) + " - " + dateFormat.format(endDate.getTime());
            Paragraph dateRange = new Paragraph(getString(R.string.pdf_period, dateRangeText))
                    .setFont(regularFont).setFontSize(12).setTextAlignment(TextAlignment.CENTER).setMarginBottom(10);
            document.add(dateRange);

            // Generated date
            String genDate = new SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(new Date());
            Paragraph generatedDate = new Paragraph(getString(R.string.pdf_generated_on, genDate))
                    .setFont(regularFont).setFontSize(10).setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(20).setFontColor(ColorConstants.GRAY);
            document.add(generatedDate);

            // ===== SUMMARY SECTION =====
            // Calculate totals
            double totalExpense = 0;
            double totalIncome = 0;
            Map<Integer, Double> categoryTotals = new HashMap<>();

            for (Expense expense : expenses) {
                if (expense.getType() == Expense.TYPE_EXPENSE) {
                    totalExpense += expense.getAmount();
                } else {
                    totalIncome += expense.getAmount();
                }

                int categoryId = expense.getCategoryId();
                categoryTotals.put(categoryId,
                        categoryTotals.getOrDefault(categoryId, 0.0) + expense.getAmount());
            }

            double balance = totalIncome - totalExpense;

            // Summary table (3 columns)
            float[] summaryColumnWidths = {1, 1, 1};
            Table summaryTable = new Table(UnitValue.createPercentArray(summaryColumnWidths));
            summaryTable.setWidth(UnitValue.createPercentValue(100));
            summaryTable.setMarginBottom(20);

            // Headers
            DeviceRgb headerColor = new DeviceRgb(79, 195, 247); // Light blue

            summaryTable.addCell(new Cell().add(new Paragraph(getString(R.string.report_total_income))
                            .setFont(boldFont).setFontSize(10))
                    .setBackgroundColor(headerColor)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setPadding(8));

            summaryTable.addCell(new Cell().add(new Paragraph(getString(R.string.report_total_expense))
                            .setFont(boldFont).setFontSize(10))
                    .setBackgroundColor(headerColor)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setPadding(8));

            summaryTable.addCell(new Cell().add(new Paragraph(getString(R.string.balance))
                            .setFont(boldFont).setFontSize(10))
                    .setBackgroundColor(headerColor)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setPadding(8));

            // Values
            summaryTable.addCell(new Cell().add(new Paragraph(currencyFormat.format(totalIncome) + "đ")
                            .setFont(regularFont).setFontSize(11).setFontColor(new DeviceRgb(76, 175, 80))) // Green
                    .setTextAlignment(TextAlignment.CENTER)
                    .setPadding(8));

            summaryTable.addCell(new Cell().add(new Paragraph(currencyFormat.format(totalExpense) + "đ")
                            .setFont(regularFont).setFontSize(11).setFontColor(new DeviceRgb(244, 67, 54))) // Red
                    .setTextAlignment(TextAlignment.CENTER)
                    .setPadding(8));

            DeviceRgb balanceColor = balance >= 0 ?
                    new DeviceRgb(76, 175, 80) : new DeviceRgb(244, 67, 54);
            summaryTable.addCell(new Cell().add(new Paragraph(currencyFormat.format(balance) + "đ")
                            .setFont(boldFont).setFontSize(11).setFontColor(balanceColor))
                    .setTextAlignment(TextAlignment.CENTER)
                    .setPadding(8));

            document.add(summaryTable);

            // ===== CATEGORY BREAKDOWN =====
            if (!categoryTotals.isEmpty()) {
                Paragraph categoryTitle = new Paragraph(getString(R.string.report_category_summary_default))
                        .setFont(boldFont)
                        .setFontSize(14)
                        .setMarginTop(10)
                        .setMarginBottom(10);
                document.add(categoryTitle);

                float[] categoryColumnWidths = {3, 2};
                Table categoryTable = new Table(UnitValue.createPercentArray(categoryColumnWidths));
                categoryTable.setWidth(UnitValue.createPercentValue(100));
                categoryTable.setMarginBottom(20);

                // Header
                categoryTable.addHeaderCell(new Cell().add(new Paragraph(getString(R.string.expense_category))
                                .setFont(boldFont).setFontSize(10))
                        .setBackgroundColor(new DeviceRgb(245, 245, 245))
                        .setPadding(8));

                categoryTable.addHeaderCell(new Cell().add(new Paragraph(getString(R.string.expense_amount))
                                .setFont(boldFont).setFontSize(10))
                        .setBackgroundColor(new DeviceRgb(245, 245, 245))
                        .setTextAlignment(TextAlignment.RIGHT)
                        .setPadding(8));

                // Rows
                for (Map.Entry<Integer, Double> entry : categoryTotals.entrySet()) {
                    Category category = dbHelper.getCategoryById(entry.getKey());
                    String categoryName = category != null ?
                            DatabaseHelper.getLocalizedCategoryName(this, category.getName()) : "Unknown";
                    String amount = currencyFormat.format(entry.getValue()) + "đ";

                    categoryTable.addCell(new Cell().add(new Paragraph(categoryName)
                                    .setFont(regularFont).setFontSize(10))
                            .setPadding(6));

                    categoryTable.addCell(new Cell().add(new Paragraph(amount)
                                    .setFont(regularFont).setFontSize(10))
                            .setTextAlignment(TextAlignment.RIGHT)
                            .setPadding(6));
                }

                document.add(categoryTable);
            }

            // ===== TRANSACTION DETAILS =====
            Paragraph detailsTitle = new Paragraph(getString(R.string.pdf_transaction_details, expenses.size()))
                    .setFont(boldFont)
                    .setFontSize(14)
                    .setMarginTop(10)
                    .setMarginBottom(10);
            document.add(detailsTitle);

            float[] detailsColumnWidths = {2, 3, 2, 3};
            Table detailsTable = new Table(UnitValue.createPercentArray(detailsColumnWidths));
            detailsTable.setWidth(UnitValue.createPercentValue(100));

            // Header
            DeviceRgb detailsHeaderColor = new DeviceRgb(245, 245, 245);

            detailsTable.addHeaderCell(new Cell().add(new Paragraph(getString(R.string.expense_date))
                            .setFont(boldFont).setFontSize(9))
                    .setBackgroundColor(detailsHeaderColor)
                    .setPadding(6));

            detailsTable.addHeaderCell(new Cell().add(new Paragraph(getString(R.string.expense_category))
                            .setFont(boldFont).setFontSize(9))
                    .setBackgroundColor(detailsHeaderColor)
                    .setPadding(6));

            detailsTable.addHeaderCell(new Cell().add(new Paragraph(getString(R.string.expense_amount))
                            .setFont(boldFont).setFontSize(9))
                    .setBackgroundColor(detailsHeaderColor)
                    .setTextAlignment(TextAlignment.RIGHT)
                    .setPadding(6));

            detailsTable.addHeaderCell(new Cell().add(new Paragraph(getString(R.string.expense_description))
                            .setFont(boldFont).setFontSize(9))
                    .setBackgroundColor(detailsHeaderColor)
                    .setPadding(6));

            // Rows
            for (Expense expense : expenses) {
                Category category = dbHelper.getCategoryById(expense.getCategoryId());
                String categoryName = category != null ?
                        DatabaseHelper.getLocalizedCategoryName(this, category.getName()) : "Unknown";
                String date = new SimpleDateFormat("dd MMM", Locale.getDefault())
                        .format(new Date(expense.getDate()));
                String amount = currencyFormat.format(expense.getAmount()) + "đ";
                String description = expense.getDescription() != null && !expense.getDescription().isEmpty() ?
                        expense.getDescription() : "-";

                DeviceRgb amountColor = expense.getType() == Expense.TYPE_INCOME ?
                        new DeviceRgb(76, 175, 80) : new DeviceRgb(244, 67, 54);

                detailsTable.addCell(new Cell().add(new Paragraph(date)
                                .setFont(regularFont).setFontSize(8))
                        .setPadding(5));

                detailsTable.addCell(new Cell().add(new Paragraph(categoryName)
                                .setFont(regularFont).setFontSize(8))
                        .setPadding(5));

                detailsTable.addCell(new Cell().add(new Paragraph(amount)
                                .setFont(regularFont).setFontSize(8).setFontColor(amountColor))
                        .setTextAlignment(TextAlignment.RIGHT)
                        .setPadding(5));

                detailsTable.addCell(new Cell().add(new Paragraph(description)
                                .setFont(regularFont).setFontSize(8))
                        .setPadding(5));
            }

            document.add(detailsTable);

            // ===== FOOTER =====
            Paragraph footer = new Paragraph("\n" + getString(R.string.pdf_footer))
                    .setFont(regularFont)
                    .setFontSize(8)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(20)
                    .setFontColor(ColorConstants.GRAY);
            document.add(footer);

            // Close document
            document.close();
            outputStream.close();

            Toast.makeText(this, getString(R.string.report_pdf_saved), Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, getString(R.string.report_pdf_failed) + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
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
    /**
     * ✅ FIX: Share email with localized subject and body
     */
    private void shareViaEmail() {
        try {
            List<Expense> expenses = getFilteredExpenses();

            if (expenses.isEmpty()) {
                Toast.makeText(this, getString(R.string.msg_no_data_share), Toast.LENGTH_SHORT).show();
                return;
            }

            // Subject: "Expense Report - 12 Nov 2025"
            String subject = getString(R.string.report_email_subject, dateFormat.format(startDate.getTime()));

            StringBuilder body = new StringBuilder();
            body.append(getString(R.string.email_header)).append("\n\n"); // "Expense Report"
            body.append(tvDateRange.getText()).append("\n");
            body.append(tvTotalExpense.getText()).append("\n");
            body.append(tvExpenseCount.getText()).append("\n\n");
            body.append(tvCategorySummary.getText());

            Intent emailIntent = new Intent(Intent.ACTION_SEND);
            emailIntent.setType("text/plain");
            emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{sessionManager.getUserEmail()});
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
            emailIntent.putExtra(Intent.EXTRA_TEXT, body.toString());

            startActivity(Intent.createChooser(emailIntent, getString(R.string.chooser_email_title)));

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, getString(R.string.error_generic), Toast.LENGTH_SHORT).show();
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

    /**
     * ✅ Setup Pie Chart (Category Breakdown)
     * Copied from ExpenseOverviewActivity
     */
    private void setupPieChart(List<Expense> expenses, long monthStart, long monthEnd) {
        Map<Integer, Double> categoryTotals = new HashMap<>();
        double totalSpent = 0;

        // Aggregate by category (only expenses, not income)
        for (Expense expense : expenses) {
            if (expense.getDate() >= monthStart && expense.getDate() <= monthEnd) {
                if (expense.isExpense()) {
                    int categoryId = expense.getCategoryId();
                    double amount = expense.getAmount();
                    categoryTotals.put(categoryId,
                            categoryTotals.getOrDefault(categoryId, 0.0) + amount);
                    totalSpent += amount;
                }
            }
        }

        if (categoryTotals.isEmpty()) {
            pieChart.setNoDataText(getString(R.string.msg_no_expenses_month));
            pieChart.invalidate();
            return;
        }

        // Create Pie Chart entries
        List<PieEntry> pieEntries = new ArrayList<>();
        int[] colors = getSeraUIColors();

        for (Map.Entry<Integer, Double> entry : categoryTotals.entrySet()) {
            Category category = dbHelper.getCategoryById(entry.getKey());

            String categoryName;
            if (category != null) {
                categoryName = DatabaseHelper.getLocalizedCategoryName(this, category.getName());
            } else {
                categoryName = getString(R.string.cat_unknown);
            }

            float value = entry.getValue().floatValue();
            pieEntries.add(new PieEntry(value, categoryName));
        }

        // Setup Pie Chart styling
        PieDataSet dataSet = new PieDataSet(pieEntries, getString(R.string.chart_expense_by_category));
        dataSet.setColors(colors);
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setSliceSpace(3f);
        dataSet.setValueFormatter(new PercentFormatter(pieChart));

        PieData pieData = new PieData(dataSet);
        pieChart.setData(pieData);

        pieChart.setUsePercentValues(true);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(Color.WHITE);
        pieChart.setHoleRadius(40f);
        pieChart.setTransparentCircleRadius(45f);
        pieChart.setDrawCenterText(true);
        pieChart.setCenterText(getString(R.string.chart_center_text));
        pieChart.setCenterTextSize(14f);
        pieChart.setRotationEnabled(true);
        pieChart.setHighlightPerTapEnabled(true);

        Description description = new Description();
        description.setText("");
        pieChart.setDescription(description);

        Legend legend = pieChart.getLegend();
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setDrawInside(false);
        legend.setWordWrapEnabled(true);
        legend.setTextSize(10f);

        pieChart.animateY(1000);
        pieChart.invalidate();
    }

    /**
     * ✅ Setup Line Chart (6-Month Trend)
     * Copied from ExpenseOverviewActivity
     */
    private void setupLineChart(List<Expense> expenses) {
        Calendar calendar = Calendar.getInstance();
        List<String> monthLabels = new ArrayList<>();
        List<Entry> lineEntries = new ArrayList<>();

        // Start from 5 months ago
        calendar.add(Calendar.MONTH, -5);

        for (int i = 0; i < 6; i++) {
            calendar.set(Calendar.DAY_OF_MONTH, 1);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            long monthStart = calendar.getTimeInMillis();

            calendar.add(Calendar.MONTH, 1);
            long monthEnd = calendar.getTimeInMillis();

            double monthTotal = 0;
            for (Expense expense : expenses) {
                if (expense.getDate() >= monthStart && expense.getDate() < monthEnd) {
                    if (expense.isExpense()) {
                        monthTotal += expense.getAmount();
                    }
                }
            }

            lineEntries.add(new Entry(i, (float) monthTotal));

            SimpleDateFormat monthFormat = new SimpleDateFormat("MMM", Locale.getDefault());
            calendar.add(Calendar.MONTH, -1);
            monthLabels.add(monthFormat.format(calendar.getTime()));
            calendar.add(Calendar.MONTH, 1);
        }

        LineDataSet dataSet = new LineDataSet(lineEntries, getString(R.string.chart_monthly_spending));
        dataSet.setColor(ContextCompat.getColor(this, R.color.primary_blue));
        dataSet.setCircleColor(ContextCompat.getColor(this, R.color.primary_blue));
        dataSet.setCircleRadius(5f);
        dataSet.setLineWidth(3f);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(ContextCompat.getColor(this, R.color.primary_blue_light));
        dataSet.setFillAlpha(50);
        dataSet.setValueTextSize(10f);
        dataSet.setValueTextColor(ContextCompat.getColor(this, R.color.primary_blue_dark));
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setDrawValues(false);

        LineData lineData = new LineData(dataSet);
        lineChart.setData(lineData);

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(monthLabels));
        xAxis.setTextSize(10f);
        xAxis.setDrawGridLines(false);

        lineChart.getAxisLeft().setTextSize(10f);
        lineChart.getAxisLeft().setDrawGridLines(true);
        lineChart.getAxisRight().setEnabled(false);

        Description description = new Description();
        description.setText(getString(R.string.chart_6_month_trend));
        description.setTextSize(12f);
        lineChart.setDescription(description);

        Legend legend = lineChart.getLegend();
        legend.setEnabled(false);

        lineChart.setTouchEnabled(true);
        lineChart.setDragEnabled(true);
        lineChart.setScaleEnabled(false);
        lineChart.setPinchZoom(false);
        lineChart.setDrawGridBackground(false);
        lineChart.animateX(1000);
        lineChart.invalidate();
    }

    /**
     * ✅ Get Sera UI color palette for charts
     */
    private int[] getSeraUIColors() {
        return new int[]{
                ContextCompat.getColor(this, R.color.primary_blue),
                ContextCompat.getColor(this, R.color.secondary_teal),
                ContextCompat.getColor(this, R.color.accent_orange),
                ContextCompat.getColor(this, R.color.accent_green),
                ContextCompat.getColor(this, R.color.accent_red),
                ContextCompat.getColor(this, R.color.accent_yellow),
                ContextCompat.getColor(this, R.color.primary_blue_light),
                ContextCompat.getColor(this, R.color.secondary_teal_light),
                ContextCompat.getColor(this, R.color.budget_warning),
                ContextCompat.getColor(this, R.color.budget_danger)
        };
    }
}