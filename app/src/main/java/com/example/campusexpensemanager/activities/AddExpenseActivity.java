package com.example.campusexpensemanager.activities;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
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
import com.example.campusexpensemanager.models.Budget;
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

import android.app.PendingIntent;
import android.content.Context;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import java.text.NumberFormat;

/**
 * AddExpenseActivity - Fully Localized
 * Updated to use resources (getString) instead of hardcoded text.
 */
public class    AddExpenseActivity extends BaseActivity implements TemplateAdapter.OnTemplateClickListener {

    private static final int CAMERA_PERMISSION_CODE = 100;

    // Instance state keys
    private static final String KEY_RECEIPT_PATH = "receipt_photo_path";
    private static final String KEY_SELECTED_DATE = "selected_date_time";
    private static final String KEY_CURRENT_TYPE = "current_type";

    private static final String BUDGET_CHANNEL_ID = "budget_alerts";
    private static final int BUDGET_NOTIFICATION_ID = 2001;

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
        hideBottomNavigation();

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

        createBudgetNotificationChannel();
    }

    /**
     * ✅ Create notification channel for budget alerts (Android 8.0+)
     */
    private void createBudgetNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.budget_alert_channel_name); // "Budget Alerts"
            String description = getString(R.string.budget_alert_channel_desc); // "Notifications when expenses exceed budget"
            int importance = NotificationManager.IMPORTANCE_HIGH;

            NotificationChannel channel = new NotificationChannel(BUDGET_CHANNEL_ID, name, importance);
            channel.setDescription(description);
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 500, 200, 500});

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
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
            Toast.makeText(this, getString(R.string.msg_no_categories), Toast.LENGTH_SHORT).show();
            return;
        }

        // ✅ FIX: Custom Adapter để hiển thị tên đa ngôn ngữ thay vì key "cat_..."
        ArrayAdapter<Category> adapter = new ArrayAdapter<Category>(
                this,
                android.R.layout.simple_spinner_item,
                categories
        ) {
            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull android.view.ViewGroup parent) {
                android.widget.TextView label = (android.widget.TextView) super.getView(position, convertView, parent);
                Category category = getItem(position);
                if (category != null) {
                    label.setText(category.getLocalizedName(getContext()));
                }
                return label;
            }

            @Override
            public View getDropDownView(int position, View convertView, @NonNull android.view.ViewGroup parent) {
                android.widget.TextView label = (android.widget.TextView) super.getDropDownView(position, convertView, parent);
                Category category = getItem(position);
                if (category != null) {
                    label.setText(category.getLocalizedName(getContext()));
                }
                return label;
            }
        };

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

        if (currentType == Expense.TYPE_EXPENSE) {
            // Nếu là chi tiêu -> Kiểm tra ngân sách trước
            checkBudgetAndConfirmSave(expense);
        } else {
            // Nếu là thu nhập -> Lưu luôn không cần hỏi
            proceedToSaveExpense(expense);
        }
    }

    /**
     * ✅ Check if new expense exceeds budget and send notification
     * @param categoryId Category ID of the expense
     * @param newAmount Amount of new expense
     */
    private void checkBudgetAndNotify(int categoryId, double newAmount) {
        try {
            int userId = sessionManager.getUserId();
            List<Budget> budgets = dbHelper.getBudgetsByUser(userId);

            // Find budget for this category or total budget (categoryId = 0)
            Budget relevantBudget = null;
            for (Budget budget : budgets) {
                // Check if budget is currently active
                long currentTime = System.currentTimeMillis();
                if (currentTime < budget.getPeriodStart() || currentTime > budget.getPeriodEnd()) {
                    continue; // Skip expired budgets
                }

                // Match category-specific budget or total budget
                if (budget.getCategoryId() == categoryId || budget.getCategoryId() == 0) {
                    relevantBudget = budget;
                    break;
                }
            }

            if (relevantBudget == null) {
                return; // No active budget found, no need to check
            }

            // Calculate current spending in this budget period
            double currentSpending = calculateSpentInPeriod(
                    userId,
                    relevantBudget.getCategoryId(),
                    relevantBudget.getPeriodStart(),
                    relevantBudget.getPeriodEnd()
            );

            // Add new expense to calculate projected total
            double projectedTotal = currentSpending + newAmount;
            double budgetLimit = relevantBudget.getAmount();
            double percentageUsed = (projectedTotal / budgetLimit) * 100;

            // Check if threshold exceeded (80% or 100%)
            if (percentageUsed >= 80) {
                sendBudgetNotification(
                        relevantBudget,
                        projectedTotal,
                        budgetLimit,
                        percentageUsed
                );
            }

        } catch (Exception e) {
            Log.e("AddExpenseActivity", "Error checking budget: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * ✅ Calculate spent amount in budget period
     */
    private double calculateSpentInPeriod(int userId, int categoryId, long periodStart, long periodEnd) {
        List<Expense> allExpenses = dbHelper.getExpensesByUser(userId);
        double total = 0;

        for (Expense expense : allExpenses) {
            // Only count expenses (not income)
            if (expense.getType() != Expense.TYPE_EXPENSE) {
                continue;
            }

            // Check if expense is in period
            if (expense.getDate() < periodStart || expense.getDate() > periodEnd) {
                continue;
            }

            // Check category match (categoryId = 0 means total budget)
            if (categoryId == 0 || expense.getCategoryId() == categoryId) {
                total += expense.getAmount();
            }
        }

        return total;
    }

    /**
     * ✅ Send budget alert notification
     */
    private void sendBudgetNotification(Budget budget, double spent, double limit, double percentage) {
        try {
            // Get category name
            String categoryName = getString(R.string.label_total_budget); // "Total Budget"
            if (budget.getCategoryId() > 0) {
                Category category = dbHelper.getCategoryById(budget.getCategoryId());
                if (category != null) {
                    categoryName = DatabaseHelper.getLocalizedCategoryName(this, category.getName());
                }
            }

            // Format amounts
            NumberFormat currencyFormat = NumberFormat.getInstance(new Locale("vi", "VN"));
            String spentAmount = currencyFormat.format(spent) + "đ";
            String limitAmount = currencyFormat.format(limit) + "đ";

            // Build notification content
            String title;
            String message;

            if (percentage >= 100) {
                title = getString(R.string.budget_exceeded_title); // "⚠️ Budget Exceeded!"
                message = getString(R.string.budget_exceeded_message, categoryName, spentAmount, limitAmount);
                // "You've exceeded your {category} budget! Spent: {spent} / Limit: {limit}"
            } else {
                title = getString(R.string.budget_warning_title); // "⚠️ Budget Warning"
                message = getString(R.string.budget_warning_message,
                        categoryName,
                        String.format("%.0f%%", percentage),
                        spentAmount,
                        limitAmount);
                // "Your {category} budget is {percentage}% used. Spent: {spent} / Limit: {limit}"
            }

            // Create notification
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, BUDGET_CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_warning) // ⚠️ Add ic_warning.xml to drawable
                    .setContentTitle(title)
                    .setContentText(message)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .setVibrate(new long[]{0, 500, 200, 500})
                    .setColor(ContextCompat.getColor(this, R.color.error));

            // Add action to view budget details
            Intent budgetIntent = new Intent(this, BudgetDashboardActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    this,
                    0,
                    budgetIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            builder.setContentIntent(pendingIntent);

            // Show notification
            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            if (notificationManager != null) {
                notificationManager.notify(BUDGET_NOTIFICATION_ID, builder.build());
                Log.d("AddExpenseActivity", "Budget notification sent: " + categoryName +
                        " (" + String.format("%.1f%%", percentage) + ")");
            }

        } catch (Exception e) {
            Log.e("AddExpenseActivity", "Error sending notification: " + e.getMessage());
            e.printStackTrace();
        }
    }
    /**
     * ✅ NEW: Kiểm tra ngân sách và hiện Dialog cảnh báo (Đã tối ưu đa ngôn ngữ)
     */
    private void checkBudgetAndConfirmSave(Expense expense) {
        int userId = sessionManager.getUserId();
        List<Budget> budgets = dbHelper.getBudgetsByUser(userId);
        Budget relevantBudget = null;

        // Tìm ngân sách phù hợp
        for (Budget budget : budgets) {
            if (expense.getDate() >= budget.getPeriodStart() && expense.getDate() <= budget.getPeriodEnd()) {
                if (budget.getCategoryId() == expense.getCategoryId() || budget.getCategoryId() == 0) {
                    relevantBudget = budget;
                    break; // Ưu tiên tìm thấy ngân sách category cụ thể
                }
            }
        }

        if (relevantBudget == null) {
            proceedToSaveExpense(expense); // Không có ngân sách -> Lưu luôn
            return;
        }

        // Tính toán chi tiêu hiện tại
        double currentSpent = calculateSpentInPeriod(userId, relevantBudget.getCategoryId(),
                relevantBudget.getPeriodStart(), relevantBudget.getPeriodEnd());
        double newTotal = currentSpent + expense.getAmount();

        // Nếu vượt quá 100% -> Hiện cảnh báo
        if (newTotal > relevantBudget.getAmount()) {
            // Format số tiền: 500,000đ
            String formattedBudget = String.format("%,.0f", relevantBudget.getAmount()) + "đ";
            String formattedProjected = String.format("%,.0f", newTotal) + "đ";

            // Tạo nội dung thông báo từ resources
            String message = getString(R.string.dialog_budget_exceeded_message, formattedBudget, formattedProjected);

            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle(getString(R.string.budget_exceeded_title)) // Tiêu đề lấy từ strings.xml
                    .setMessage(message) // Nội dung đã format
                    .setPositiveButton(getString(R.string.action_save), (dialog, which) -> proceedToSaveExpense(expense))
                    .setNegativeButton(getString(R.string.action_cancel), null)
                    .setIcon(R.drawable.ic_warning) // Đảm bảo icon này tồn tại hoặc dùng android.R.drawable.ic_dialog_alert
                    .show();
        } else {
            proceedToSaveExpense(expense); // Chưa vượt -> Lưu luôn
        }
    }

    /**
     * ✅ NEW: Hàm thực hiện lưu vào DB (Đã tối ưu đa ngôn ngữ)
     */
    private void proceedToSaveExpense(Expense expense) {
        long expenseId = dbHelper.insertExpense(expense);

        if (expenseId != -1) {
            String typeText = currentType == Expense.TYPE_INCOME ?
                    getString(R.string.label_income) : getString(R.string.label_expense);

            // Format lại số tiền để hiển thị Toast đẹp hơn
            java.text.NumberFormat format = java.text.NumberFormat.getInstance(new java.util.Locale("vi", "VN"));
            String formattedAmount = format.format(expense.getAmount()) + "đ";

            Toast.makeText(this, getString(R.string.msg_transaction_added, typeText, formattedAmount),
                    Toast.LENGTH_SHORT).show();

            setResult(RESULT_OK);
            finish();
        } else {
            // Sử dụng string resource cho thông báo lỗi
            Toast.makeText(this, getString(R.string.msg_save_error), Toast.LENGTH_SHORT).show();
        }
    }
}