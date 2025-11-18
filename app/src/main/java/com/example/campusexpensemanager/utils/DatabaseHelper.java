package com.example.campusexpensemanager.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.example.campusexpensemanager.models.Budget;
import com.example.campusexpensemanager.models.Category;
import com.example.campusexpensemanager.models.Expense;
import com.example.campusexpensemanager.models.ExpenseTemplate;
import com.example.campusexpensemanager.models.Feedback;
import com.example.campusexpensemanager.models.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DatabaseHelper - Sprint 5 Enhanced
 * NEW: Recurring expenses, Income tracking, Expense templates
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = "DatabaseHelper";
    private static final String DATABASE_NAME = "CampusExpense.db";
    private static final int DATABASE_VERSION = 3; // ✅ UPGRADED from 2 to 3

    // Table Names
    private static final String TABLE_USERS = "users";
    private static final String TABLE_CATEGORIES = "categories";
    private static final String TABLE_EXPENSES = "expenses";
    private static final String TABLE_BUDGETS = "budgets";
    private static final String TABLE_CURRENCIES = "currencies";
    private static final String TABLE_TEMPLATES = "expense_templates"; // NEW

    // Common Columns
    private static final String KEY_ID = "id";
    private static final String KEY_CREATED_AT = "created_at";

    // User Columns
    private static final String KEY_USER_EMAIL = "email";
    private static final String KEY_USER_PASSWORD = "password_hash";
    private static final String KEY_USER_NAME = "name";
    private static final String KEY_USER_ADDRESS = "address";
    private static final String KEY_USER_PHONE = "phone";
    private static final String KEY_USER_AVATAR = "avatar_path";
    private static final String KEY_USER_DARK_MODE = "dark_mode_enabled";

    // Add these constants at the top with other table names
    private static final String TABLE_FEEDBACK = "feedback";

    // Feedback Columns
    private static final String KEY_FEEDBACK_USER_ID = "user_id";
    private static final String KEY_FEEDBACK_RATING = "rating";
    private static final String KEY_FEEDBACK_CONTENT = "content";
    private static final String KEY_FEEDBACK_TIMESTAMP = "timestamp";

    // Category Columns
    private static final String KEY_CATEGORY_NAME = "name";
    private static final String KEY_CATEGORY_ICON = "icon_resource";

    // Expense Columns (ENHANCED)
    private static final String KEY_EXPENSE_USER_ID = "user_id";
    private static final String KEY_EXPENSE_CATEGORY_ID = "category_id";
    private static final String KEY_EXPENSE_CURRENCY_ID = "currency_id";
    private static final String KEY_EXPENSE_AMOUNT = "amount";
    private static final String KEY_EXPENSE_DATE = "date";
    private static final String KEY_EXPENSE_DESCRIPTION = "description";
    private static final String KEY_EXPENSE_RECEIPT = "receipt_path";
    private static final String KEY_EXPENSE_TYPE = "type"; // 0=expense, 1=income
    // NEW RECURRING COLUMNS
    private static final String KEY_EXPENSE_IS_RECURRING = "is_recurring";
    private static final String KEY_EXPENSE_RECURRENCE_PERIOD = "recurrence_period"; // daily/weekly/monthly
    private static final String KEY_EXPENSE_NEXT_OCCURRENCE = "next_occurrence_date";

    // Budget Columns
    private static final String KEY_BUDGET_USER_ID = "user_id";
    private static final String KEY_BUDGET_CATEGORY_ID = "category_id";
    private static final String KEY_BUDGET_AMOUNT = "amount";
    private static final String KEY_BUDGET_PERIOD_START = "period_start";
    private static final String KEY_BUDGET_PERIOD_END = "period_end";

    // Currency Columns
    private static final String KEY_CURRENCY_CODE = "code";
    private static final String KEY_CURRENCY_RATE = "rate_to_vnd";
    private static final String KEY_CURRENCY_UPDATED = "last_updated";

    // Template Columns (NEW)
    private static final String KEY_TEMPLATE_NAME = "name";
    private static final String KEY_TEMPLATE_CATEGORY_ID = "category_id";
    private static final String KEY_TEMPLATE_DEFAULT_AMOUNT = "default_amount";
    private static final String KEY_TEMPLATE_ICON = "icon_resource";

    private static DatabaseHelper instance;

    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    private DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d(TAG, "Creating database tables...");

        // Users Table
        String CREATE_USERS_TABLE = "CREATE TABLE " + TABLE_USERS + "("
                + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_USER_EMAIL + " TEXT UNIQUE NOT NULL,"
                + KEY_USER_PASSWORD + " TEXT NOT NULL,"
                + KEY_USER_NAME + " TEXT NOT NULL,"
                + KEY_USER_ADDRESS + " TEXT,"
                + KEY_USER_PHONE + " TEXT,"
                + KEY_USER_AVATAR + " TEXT,"
                + KEY_USER_DARK_MODE + " INTEGER DEFAULT 0,"
                + KEY_CREATED_AT + " INTEGER NOT NULL"
                + ")";
        db.execSQL(CREATE_USERS_TABLE);

        // Categories Table
        String CREATE_CATEGORIES_TABLE = "CREATE TABLE " + TABLE_CATEGORIES + "("
                + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_CATEGORY_NAME + " TEXT NOT NULL,"
                + KEY_CATEGORY_ICON + " TEXT"
                + ")";
        db.execSQL(CREATE_CATEGORIES_TABLE);

        // Currencies Table
        String CREATE_CURRENCIES_TABLE = "CREATE TABLE " + TABLE_CURRENCIES + "("
                + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_CURRENCY_CODE + " TEXT UNIQUE NOT NULL,"
                + KEY_CURRENCY_RATE + " REAL DEFAULT 1,"
                + KEY_CURRENCY_UPDATED + " INTEGER"
                + ")";
        db.execSQL(CREATE_CURRENCIES_TABLE);

        // Expenses Table (ENHANCED with Recurring + Type)
        String CREATE_EXPENSES_TABLE = "CREATE TABLE " + TABLE_EXPENSES + "("
                + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_EXPENSE_USER_ID + " INTEGER NOT NULL,"
                + KEY_EXPENSE_CATEGORY_ID + " INTEGER NOT NULL,"
                + KEY_EXPENSE_CURRENCY_ID + " INTEGER DEFAULT 1,"
                + KEY_EXPENSE_AMOUNT + " REAL NOT NULL,"
                + KEY_EXPENSE_DATE + " INTEGER NOT NULL,"
                + KEY_EXPENSE_DESCRIPTION + " TEXT,"
                + KEY_EXPENSE_RECEIPT + " TEXT,"
                + KEY_EXPENSE_TYPE + " INTEGER DEFAULT 0," // 0=expense, 1=income
                + KEY_EXPENSE_IS_RECURRING + " INTEGER DEFAULT 0," // NEW
                + KEY_EXPENSE_RECURRENCE_PERIOD + " TEXT," // NEW: daily/weekly/monthly
                + KEY_EXPENSE_NEXT_OCCURRENCE + " INTEGER," // NEW: timestamp
                + KEY_CREATED_AT + " INTEGER NOT NULL,"
                + "FOREIGN KEY(" + KEY_EXPENSE_USER_ID + ") REFERENCES "
                + TABLE_USERS + "(" + KEY_ID + ") ON DELETE CASCADE,"
                + "FOREIGN KEY(" + KEY_EXPENSE_CATEGORY_ID + ") REFERENCES "
                + TABLE_CATEGORIES + "(" + KEY_ID + "),"
                + "FOREIGN KEY(" + KEY_EXPENSE_CURRENCY_ID + ") REFERENCES "
                + TABLE_CURRENCIES + "(" + KEY_ID + ")"
                + ")";
        db.execSQL(CREATE_EXPENSES_TABLE);

        // Budgets Table
        String CREATE_BUDGETS_TABLE = "CREATE TABLE " + TABLE_BUDGETS + "("
                + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_BUDGET_USER_ID + " INTEGER NOT NULL,"
                + KEY_BUDGET_CATEGORY_ID + " INTEGER DEFAULT 0,"
                + KEY_BUDGET_AMOUNT + " REAL NOT NULL,"
                + KEY_BUDGET_PERIOD_START + " INTEGER NOT NULL,"
                + KEY_BUDGET_PERIOD_END + " INTEGER NOT NULL,"
                + KEY_CREATED_AT + " INTEGER NOT NULL,"
                + "FOREIGN KEY(" + KEY_BUDGET_USER_ID + ") REFERENCES "
                + TABLE_USERS + "(" + KEY_ID + ") ON DELETE CASCADE,"
                + "FOREIGN KEY(" + KEY_BUDGET_CATEGORY_ID + ") REFERENCES "
                + TABLE_CATEGORIES + "(" + KEY_ID + ")"
                + ")";
        db.execSQL(CREATE_BUDGETS_TABLE);

        // Expense Templates Table (NEW)
        String CREATE_TEMPLATES_TABLE = "CREATE TABLE " + TABLE_TEMPLATES + "("
                + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_TEMPLATE_NAME + " TEXT NOT NULL,"
                + KEY_TEMPLATE_CATEGORY_ID + " INTEGER NOT NULL,"
                + KEY_TEMPLATE_DEFAULT_AMOUNT + " REAL DEFAULT 0,"
                + KEY_TEMPLATE_ICON + " TEXT,"
                + "FOREIGN KEY(" + KEY_TEMPLATE_CATEGORY_ID + ") REFERENCES "
                + TABLE_CATEGORIES + "(" + KEY_ID + ")"
                + ")";
        db.execSQL(CREATE_TEMPLATES_TABLE);

        // Enable foreign keys
        db.execSQL("PRAGMA foreign_keys=ON");

        // Pre-populate data
        prepopulateCategories(db);
        prepopulateCurrencies(db);
        prepopulateTemplates(db); // NEW

        Log.d(TAG, "Database v2 created successfully with Sprint 5 enhancements");

        // Feedback Table (NEW - Priority 4)
        String CREATE_FEEDBACK_TABLE = "CREATE TABLE " + TABLE_FEEDBACK + "("
                + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_FEEDBACK_USER_ID + " INTEGER NOT NULL,"
                + KEY_FEEDBACK_RATING + " INTEGER NOT NULL,"
                + KEY_FEEDBACK_CONTENT + " TEXT,"
                + KEY_FEEDBACK_TIMESTAMP + " INTEGER NOT NULL,"
                + "FOREIGN KEY(" + KEY_FEEDBACK_USER_ID + ") REFERENCES "
                + TABLE_USERS + "(" + KEY_ID + ") ON DELETE CASCADE"
                + ")";
        db.execSQL(CREATE_FEEDBACK_TABLE);

        Log.d(TAG, "Feedback table created successfully");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(TAG, "Upgrading database from v" + oldVersion + " to v" + newVersion);

        if (oldVersion < 2) {
            // Add new columns to existing Expenses table
            try {
                db.execSQL("ALTER TABLE " + TABLE_EXPENSES + " ADD COLUMN "
                        + KEY_EXPENSE_IS_RECURRING + " INTEGER DEFAULT 0");
                db.execSQL("ALTER TABLE " + TABLE_EXPENSES + " ADD COLUMN "
                        + KEY_EXPENSE_RECURRENCE_PERIOD + " TEXT");
                db.execSQL("ALTER TABLE " + TABLE_EXPENSES + " ADD COLUMN "
                        + KEY_EXPENSE_NEXT_OCCURRENCE + " INTEGER");

                // Create Templates table
                String CREATE_TEMPLATES_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_TEMPLATES + "("
                        + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                        + KEY_TEMPLATE_NAME + " TEXT NOT NULL,"
                        + KEY_TEMPLATE_CATEGORY_ID + " INTEGER NOT NULL,"
                        + KEY_TEMPLATE_DEFAULT_AMOUNT + " REAL DEFAULT 0,"
                        + KEY_TEMPLATE_ICON + " TEXT,"
                        + "FOREIGN KEY(" + KEY_TEMPLATE_CATEGORY_ID + ") REFERENCES "
                        + TABLE_CATEGORIES + "(" + KEY_ID + ")"
                        + ")";
                db.execSQL(CREATE_TEMPLATES_TABLE);

                prepopulateTemplates(db);
                Log.d(TAG, "Database upgraded to v2 successfully");
            } catch (Exception e) {
                Log.e(TAG, "Error upgrading database: " + e.getMessage());
                e.printStackTrace();
            }
        }

        if (oldVersion < 3) {
            // Add Feedback table
            try {
                String CREATE_FEEDBACK_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_FEEDBACK + "("
                        + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                        + KEY_FEEDBACK_USER_ID + " INTEGER NOT NULL,"
                        + KEY_FEEDBACK_RATING + " INTEGER NOT NULL,"
                        + KEY_FEEDBACK_CONTENT + " TEXT,"
                        + KEY_FEEDBACK_TIMESTAMP + " INTEGER NOT NULL,"
                        + "FOREIGN KEY(" + KEY_FEEDBACK_USER_ID + ") REFERENCES "
                        + TABLE_USERS + "(" + KEY_ID + ") ON DELETE CASCADE"
                        + ")";
                db.execSQL(CREATE_FEEDBACK_TABLE);
                Log.d(TAG, "Database upgraded to v3 - Feedback table added");
            } catch (Exception e) {
                Log.e(TAG, "Error upgrading to v3: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        db.execSQL("PRAGMA foreign_keys=ON");
    }

    // =============== PRE-POPULATE DATA ===============

    private void prepopulateCategories(SQLiteDatabase db) {
        String[] categories = {
                "Food & Dining", "ic_food",
                "Transportation", "ic_transport",
                "Study & Books", "ic_study",
                "Entertainment", "ic_entertainment",
                "Shopping", "ic_shopping",
                "Healthcare", "ic_health",
                "Utilities", "ic_utilities",
                "Housing", "ic_housing",
                "Personal Care", "ic_personal",
                "Salary", "ic_salary", // NEW for income
                "Others", "ic_others"
        };

        for (int i = 0; i < categories.length; i += 2) {
            ContentValues values = new ContentValues();
            values.put(KEY_CATEGORY_NAME, categories[i]);
            values.put(KEY_CATEGORY_ICON, categories[i + 1]);
            db.insert(TABLE_CATEGORIES, null, values);
        }
        Log.d(TAG, "Pre-populated categories");
    }

    private void prepopulateCurrencies(SQLiteDatabase db) {
        // VND
        ContentValues vnd = new ContentValues();
        vnd.put(KEY_CURRENCY_CODE, "VND");
        vnd.put(KEY_CURRENCY_RATE, 1.0);
        vnd.put(KEY_CURRENCY_UPDATED, System.currentTimeMillis());
        db.insert(TABLE_CURRENCIES, null, vnd);

        // USD (example rate: 1 USD = 24,000 VND)
        ContentValues usd = new ContentValues();
        usd.put(KEY_CURRENCY_CODE, "USD");
        usd.put(KEY_CURRENCY_RATE, 24000.0);
        usd.put(KEY_CURRENCY_UPDATED, System.currentTimeMillis());
        db.insert(TABLE_CURRENCIES, null, usd);

        Log.d(TAG, "Pre-populated currencies");
    }

    private void prepopulateTemplates(SQLiteDatabase db) {
        // Quick templates (name, category_id, default_amount, icon)
        Object[][] templates = {
                {"Tiền trọ", 8, 1500000.0, "🏠"}, // Housing
                {"Ăn sáng", 1, 25000.0, "🍜"},     // Food
                {"Ăn trưa", 1, 40000.0, "🍱"},     // Food
                {"Ăn tối", 1, 35000.0, "🍲"},      // Food
                {"Cà phê", 1, 30000.0, "☕"},      // Food
                {"Xe bus", 2, 7000.0, "🚌"},       // Transport
                {"Grab", 2, 50000.0, "🚗"},        // Transport
                {"Xăng xe", 2, 100000.0, "⛽"},    // Transport
                {"Điện nước", 7, 200000.0, "⚡"},  // Utilities
                {"Internet", 7, 150000.0, "📡"},   // Utilities
                {"Học phí", 3, 5000000.0, "📚"},   // Study
                {"Sách vở", 3, 100000.0, "📖"},    // Study
                {"Xem phim", 4, 80000.0, "🎬"},    // Entertainment
                {"Đi chơi", 4, 200000.0, "🎮"},    // Entertainment
        };

        for (Object[] template : templates) {
            ContentValues values = new ContentValues();
            values.put(KEY_TEMPLATE_NAME, (String) template[0]);
            values.put(KEY_TEMPLATE_CATEGORY_ID, (Integer) template[1]);
            values.put(KEY_TEMPLATE_DEFAULT_AMOUNT, (Double) template[2]);
            values.put(KEY_TEMPLATE_ICON, (String) template[3]);
            db.insert(TABLE_TEMPLATES, null, values);
        }
        Log.d(TAG, "Pre-populated expense templates");
    }

    // =============== EXPENSE TEMPLATE CRUD ===============

    public List<ExpenseTemplate> getAllTemplates() {
        List<ExpenseTemplate> templates = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_TEMPLATES, null, null, null, null, null, KEY_TEMPLATE_NAME);

        if (cursor != null && cursor.moveToFirst()) {
            do {
                ExpenseTemplate template = cursorToTemplate(cursor);
                templates.add(template);
            } while (cursor.moveToNext());
            cursor.close();
        }

        return templates;
    }

    public ExpenseTemplate getTemplateById(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_TEMPLATES, null, KEY_ID + "=?",
                new String[]{String.valueOf(id)}, null, null, null);

        ExpenseTemplate template = null;
        if (cursor != null && cursor.moveToFirst()) {
            template = cursorToTemplate(cursor);
            cursor.close();
        }
        return template;
    }

    // =============== RECURRING EXPENSE METHODS ===============

    /**
     * Get all recurring expenses that need to be created
     * @return List of expenses due for recurrence
     */
    public List<Expense> getDueRecurringExpenses() {
        List<Expense> dueExpenses = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        long currentTime = System.currentTimeMillis();

        String query = "SELECT * FROM " + TABLE_EXPENSES
                + " WHERE " + KEY_EXPENSE_IS_RECURRING + "=1"
                + " AND " + KEY_EXPENSE_NEXT_OCCURRENCE + "<=" + currentTime;

        Cursor cursor = db.rawQuery(query, null);

        if (cursor != null && cursor.moveToFirst()) {
            do {
                Expense expense = cursorToExpense(cursor);
                dueExpenses.add(expense);
            } while (cursor.moveToNext());
            cursor.close();
        }

        return dueExpenses;
    }

    /**
     * Create new occurrence of recurring expense
     * @param originalExpense The recurring expense template
     * @return ID of new expense
     */
    public long createRecurringOccurrence(Expense originalExpense) {
        // Create new expense with current date
        Expense newExpense = new Expense(
                originalExpense.getUserId(),
                originalExpense.getCategoryId(),
                originalExpense.getAmount(),
                System.currentTimeMillis(),
                originalExpense.getDescription(),
                originalExpense.getType()
        );
        newExpense.setCurrencyId(originalExpense.getCurrencyId());
        newExpense.setIsRecurring(false); // New occurrence is NOT recurring
        newExpense.setReceiptPath(null); // No receipt for auto-created

        // Insert new expense
        long newId = insertExpense(newExpense);

        if (newId != -1) {

            Log.d(TAG, "Created recurring occurrence, ID: " + newId);

            // Update original expense's next_occurrence_date
            long nextOccurrence = calculateNextOccurrence(
                    originalExpense.getNextOccurrenceDate(),
                    originalExpense.getRecurrencePeriod()
            );

            SQLiteDatabase db = this.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(KEY_EXPENSE_NEXT_OCCURRENCE, nextOccurrence);

            db.update(TABLE_EXPENSES, values, KEY_ID + "=?",
                    new String[]{String.valueOf(originalExpense.getId())});

            Log.d(TAG, "Created recurring occurrence, next due: " + nextOccurrence);
        }

        return newId;
    }

    /**
     * Calculate next occurrence date based on period
     */
    private long calculateNextOccurrence(long currentDate, String period) {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.setTimeInMillis(currentDate);

        switch (period.toLowerCase()) {
            case "daily":
                calendar.add(java.util.Calendar.DAY_OF_MONTH, 1);
                break;
            case "weekly":
                calendar.add(java.util.Calendar.WEEK_OF_YEAR, 1);
                break;
            case "monthly":
                calendar.add(java.util.Calendar.MONTH, 1);
                break;
            default:
                calendar.add(java.util.Calendar.MONTH, 1); // Default monthly
        }

        return calendar.getTimeInMillis();
    }

    // =============== INCOME/EXPENSE STATISTICS ===============

    /**
     * Calculate total income for a user in date range
     */
    public double getTotalIncome(int userId, long startDate, long endDate) {
        SQLiteDatabase db = this.getReadableDatabase();
        double total = 0;

        String query = "SELECT SUM(" + KEY_EXPENSE_AMOUNT + ") as total FROM " + TABLE_EXPENSES
                + " WHERE " + KEY_EXPENSE_USER_ID + "=?"
                + " AND " + KEY_EXPENSE_TYPE + "=" + Expense.TYPE_INCOME
                + " AND " + KEY_EXPENSE_DATE + " BETWEEN ? AND ?";

        Cursor cursor = db.rawQuery(query, new String[]{
                String.valueOf(userId),
                String.valueOf(startDate),
                String.valueOf(endDate)
        });

        if (cursor != null && cursor.moveToFirst()) {
            total = cursor.getDouble(cursor.getColumnIndexOrThrow("total"));
            cursor.close();
        }

        return total;
    }

    /**
     * Calculate total expense for a user in date range
     */
    public double getTotalExpense(int userId, long startDate, long endDate) {
        SQLiteDatabase db = this.getReadableDatabase();
        double total = 0;

        String query = "SELECT SUM(" + KEY_EXPENSE_AMOUNT + ") as total FROM " + TABLE_EXPENSES
                + " WHERE " + KEY_EXPENSE_USER_ID + "=?"
                + " AND " + KEY_EXPENSE_TYPE + "=" + Expense.TYPE_EXPENSE
                + " AND " + KEY_EXPENSE_DATE + " BETWEEN ? AND ?";

        Cursor cursor = db.rawQuery(query, new String[]{
                String.valueOf(userId),
                String.valueOf(startDate),
                String.valueOf(endDate)
        });

        if (cursor != null && cursor.moveToFirst()) {
            total = cursor.getDouble(cursor.getColumnIndexOrThrow("total"));
            cursor.close();
        }

        return total;
    }

    /**
     * Get balance (Income - Expense)
     */
    public double getBalance(int userId, long startDate, long endDate) {
        double income = getTotalIncome(userId, startDate, endDate);
        double expense = getTotalExpense(userId, startDate, endDate);
        return income - expense;
    }

    // =============== EXISTING CRUD (Updated) ===============

    public long insertExpense(Expense expense) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_EXPENSE_USER_ID, expense.getUserId());
        values.put(KEY_EXPENSE_CATEGORY_ID, expense.getCategoryId());
        values.put(KEY_EXPENSE_CURRENCY_ID, expense.getCurrencyId());
        values.put(KEY_EXPENSE_AMOUNT, expense.getAmount());
        values.put(KEY_EXPENSE_DATE, expense.getDate());
        values.put(KEY_EXPENSE_DESCRIPTION, expense.getDescription());
        values.put(KEY_EXPENSE_RECEIPT, expense.getReceiptPath());
        values.put(KEY_EXPENSE_TYPE, expense.getType());

        // NEW: Recurring fields
        values.put(KEY_EXPENSE_IS_RECURRING, expense.isRecurring() ? 1 : 0);
        values.put(KEY_EXPENSE_RECURRENCE_PERIOD, expense.getRecurrencePeriod());
        values.put(KEY_EXPENSE_NEXT_OCCURRENCE, expense.getNextOccurrenceDate());

        values.put(KEY_CREATED_AT, expense.getCreatedAt());

        long id = db.insert(TABLE_EXPENSES, null, values);
        Log.d(TAG, "Expense inserted: " + id + " (Type: " +
                (expense.isIncome() ? "INCOME" : "EXPENSE") +
                ", Recurring: " + expense.isRecurring() + ")");

        return id;
    }

    public int updateExpense(Expense expense) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_EXPENSE_CATEGORY_ID, expense.getCategoryId());
        values.put(KEY_EXPENSE_CURRENCY_ID, expense.getCurrencyId());
        values.put(KEY_EXPENSE_AMOUNT, expense.getAmount());
        values.put(KEY_EXPENSE_DATE, expense.getDate());
        values.put(KEY_EXPENSE_DESCRIPTION, expense.getDescription());
        values.put(KEY_EXPENSE_RECEIPT, expense.getReceiptPath());
        values.put(KEY_EXPENSE_TYPE, expense.getType());

        // NEW: Recurring fields
        values.put(KEY_EXPENSE_IS_RECURRING, expense.isRecurring() ? 1 : 0);
        values.put(KEY_EXPENSE_RECURRENCE_PERIOD, expense.getRecurrencePeriod());
        values.put(KEY_EXPENSE_NEXT_OCCURRENCE, expense.getNextOccurrenceDate());

        int rowsAffected = db.update(TABLE_EXPENSES, values, KEY_ID + "=?",
                new String[]{String.valueOf(expense.getId())});

        Log.d(TAG, "Expense updated: " + rowsAffected + " rows");
        return rowsAffected;
    }

    // =============== HELPER METHODS ===============

    private ExpenseTemplate cursorToTemplate(Cursor cursor) {
        return new ExpenseTemplate(
                cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ID)),
                cursor.getString(cursor.getColumnIndexOrThrow(KEY_TEMPLATE_NAME)),
                cursor.getInt(cursor.getColumnIndexOrThrow(KEY_TEMPLATE_CATEGORY_ID)),
                cursor.getDouble(cursor.getColumnIndexOrThrow(KEY_TEMPLATE_DEFAULT_AMOUNT)),
                cursor.getString(cursor.getColumnIndexOrThrow(KEY_TEMPLATE_ICON))
        );
    }

    private Expense cursorToExpense(Cursor cursor) {
        int typeIndex = cursor.getColumnIndex(KEY_EXPENSE_TYPE);
        int type = (typeIndex >= 0) ? cursor.getInt(typeIndex) : 0;

        // NEW: Recurring fields
        int isRecurringIndex = cursor.getColumnIndex(KEY_EXPENSE_IS_RECURRING);
        boolean isRecurring = (isRecurringIndex >= 0) && cursor.getInt(isRecurringIndex) == 1;

        int periodIndex = cursor.getColumnIndex(KEY_EXPENSE_RECURRENCE_PERIOD);
        String period = (periodIndex >= 0) ? cursor.getString(periodIndex) : null;

        int nextOccIndex = cursor.getColumnIndex(KEY_EXPENSE_NEXT_OCCURRENCE);
        long nextOcc = (nextOccIndex >= 0) ? cursor.getLong(nextOccIndex) : 0;

        Expense expense = new Expense(
                cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ID)),
                cursor.getInt(cursor.getColumnIndexOrThrow(KEY_EXPENSE_USER_ID)),
                cursor.getInt(cursor.getColumnIndexOrThrow(KEY_EXPENSE_CATEGORY_ID)),
                cursor.getInt(cursor.getColumnIndexOrThrow(KEY_EXPENSE_CURRENCY_ID)),
                cursor.getDouble(cursor.getColumnIndexOrThrow(KEY_EXPENSE_AMOUNT)),
                cursor.getLong(cursor.getColumnIndexOrThrow(KEY_EXPENSE_DATE)),
                cursor.getString(cursor.getColumnIndexOrThrow(KEY_EXPENSE_DESCRIPTION)),
                cursor.getString(cursor.getColumnIndexOrThrow(KEY_EXPENSE_RECEIPT)),
                cursor.getLong(cursor.getColumnIndexOrThrow(KEY_CREATED_AT)),
                type
        );

        expense.setIsRecurring(isRecurring);
        expense.setRecurrencePeriod(period);
        expense.setNextOccurrenceDate(nextOcc);

        return expense;
    }

    // Keep existing methods: getUserById, getUserByEmail, updateUser, deleteUser,
    // getAllCategories, getCategoryById, getExpensesByUser, deleteExpense,
    // insertBudget, getBudgetsByUser, updateBudget, deleteBudget, cursorToUser,
    // cursorToCategory, cursorToBudget

    // (Copy from original DatabaseHelper.java - not repeating here for brevity)

    public User getUserById(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, null, KEY_ID + "=?",
                new String[]{String.valueOf(userId)}, null, null, null);
        User user = null;
        if (cursor != null && cursor.moveToFirst()) {
            user = cursorToUser(cursor);
            cursor.close();
        }
        return user;
    }

    public User getUserByEmail(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, null, KEY_USER_EMAIL + "=?",
                new String[]{email}, null, null, null);
        User user = null;
        if (cursor != null && cursor.moveToFirst()) {
            user = cursorToUser(cursor);
            cursor.close();
        }
        return user;
    }

    public int updateUser(User user) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_USER_EMAIL, user.getEmail());
        values.put(KEY_USER_PASSWORD, user.getPasswordHash());
        values.put(KEY_USER_NAME, user.getName());
        values.put(KEY_USER_ADDRESS, user.getAddress());
        values.put(KEY_USER_PHONE, user.getPhone());
        values.put(KEY_USER_AVATAR, user.getAvatarPath());
        values.put(KEY_USER_DARK_MODE, user.isDarkModeEnabled() ? 1 : 0);
        return db.update(TABLE_USERS, values, KEY_ID + "=?",
                new String[]{String.valueOf(user.getId())});
    }

    public long insertUser(User user) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_USER_EMAIL, user.getEmail());
        values.put(KEY_USER_PASSWORD, user.getPasswordHash());
        values.put(KEY_USER_NAME, user.getName());
        values.put(KEY_USER_ADDRESS, user.getAddress());
        values.put(KEY_USER_PHONE, user.getPhone());
        values.put(KEY_USER_AVATAR, user.getAvatarPath());
        values.put(KEY_USER_DARK_MODE, user.isDarkModeEnabled() ? 1 : 0);
        values.put(KEY_CREATED_AT, user.getCreatedAt());
        return db.insert(TABLE_USERS, null, values);
    }

    public int deleteUser(int userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLE_USERS, KEY_ID + "=?", new String[]{String.valueOf(userId)});
    }

    public List<Category> getAllCategories() {
        List<Category> categories = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_CATEGORIES, null, null, null, null, null, KEY_CATEGORY_NAME);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                categories.add(cursorToCategory(cursor));
            } while (cursor.moveToNext());
            cursor.close();
        }
        return categories;
    }

    public Category getCategoryById(int categoryId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_CATEGORIES, null, KEY_ID + "=?",
                new String[]{String.valueOf(categoryId)}, null, null, null);
        Category category = null;
        if (cursor != null && cursor.moveToFirst()) {
            category = cursorToCategory(cursor);
            cursor.close();
        }
        return category;
    }

    public List<Expense> getExpensesByUser(int userId) {
        List<Expense> expenses = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_EXPENSES, null, KEY_EXPENSE_USER_ID + "=?",
                new String[]{String.valueOf(userId)}, null, null, KEY_EXPENSE_DATE + " DESC");
        if (cursor != null && cursor.moveToFirst()) {
            do {
                expenses.add(cursorToExpense(cursor));
            } while (cursor.moveToNext());
            cursor.close();
        }
        return expenses;
    }

    public int deleteExpense(int expenseId) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLE_EXPENSES, KEY_ID + "=?", new String[]{String.valueOf(expenseId)});
    }

    public long insertBudget(Budget budget) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_BUDGET_USER_ID, budget.getUserId());
        values.put(KEY_BUDGET_CATEGORY_ID, budget.getCategoryId());
        values.put(KEY_BUDGET_AMOUNT, budget.getAmount());
        values.put(KEY_BUDGET_PERIOD_START, budget.getPeriodStart());
        values.put(KEY_BUDGET_PERIOD_END, budget.getPeriodEnd());
        values.put(KEY_CREATED_AT, budget.getCreatedAt());
        return db.insert(TABLE_BUDGETS, null, values);
    }

    public List<Budget> getBudgetsByUser(int userId) {
        List<Budget> budgets = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_BUDGETS, null, KEY_BUDGET_USER_ID + "=?",
                new String[]{String.valueOf(userId)}, null, null, KEY_BUDGET_PERIOD_END + " DESC");
        if (cursor != null && cursor.moveToFirst()) {
            do {
                budgets.add(cursorToBudget(cursor));
            } while (cursor.moveToNext());
            cursor.close();
        }
        return budgets;
    }

    public int updateBudget(Budget budget) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_BUDGET_CATEGORY_ID, budget.getCategoryId());
        values.put(KEY_BUDGET_AMOUNT, budget.getAmount());
        values.put(KEY_BUDGET_PERIOD_START, budget.getPeriodStart());
        values.put(KEY_BUDGET_PERIOD_END, budget.getPeriodEnd());
        return db.update(TABLE_BUDGETS, values, KEY_ID + "=?",
                new String[]{String.valueOf(budget.getId())});
    }

    public int deleteBudget(int budgetId) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLE_BUDGETS, KEY_ID + "=?", new String[]{String.valueOf(budgetId)});
    }

    private User cursorToUser(Cursor cursor) {
        return new User(
                cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ID)),
                cursor.getString(cursor.getColumnIndexOrThrow(KEY_USER_EMAIL)),
                cursor.getString(cursor.getColumnIndexOrThrow(KEY_USER_PASSWORD)),
                cursor.getString(cursor.getColumnIndexOrThrow(KEY_USER_NAME)),
                cursor.getString(cursor.getColumnIndexOrThrow(KEY_USER_ADDRESS)),
                cursor.getString(cursor.getColumnIndexOrThrow(KEY_USER_PHONE)),
                cursor.getString(cursor.getColumnIndexOrThrow(KEY_USER_AVATAR)),
                cursor.getInt(cursor.getColumnIndexOrThrow(KEY_USER_DARK_MODE)) == 1,
                cursor.getLong(cursor.getColumnIndexOrThrow(KEY_CREATED_AT))
        );
    }

    private Category cursorToCategory(Cursor cursor) {
        return new Category(
                cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ID)),
                cursor.getString(cursor.getColumnIndexOrThrow(KEY_CATEGORY_NAME)),
                cursor.getString(cursor.getColumnIndexOrThrow(KEY_CATEGORY_ICON))
        );
    }

    private Budget cursorToBudget(Cursor cursor) {
        return new Budget(
                cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ID)),
                cursor.getInt(cursor.getColumnIndexOrThrow(KEY_BUDGET_USER_ID)),
                cursor.getInt(cursor.getColumnIndexOrThrow(KEY_BUDGET_CATEGORY_ID)),
                cursor.getDouble(cursor.getColumnIndexOrThrow(KEY_BUDGET_AMOUNT)),
                cursor.getLong(cursor.getColumnIndexOrThrow(KEY_BUDGET_PERIOD_START)),
                cursor.getLong(cursor.getColumnIndexOrThrow(KEY_BUDGET_PERIOD_END)),
                cursor.getLong(cursor.getColumnIndexOrThrow(KEY_CREATED_AT))
        );
    }

    // ✅ NEW OPTIMIZED METHODS - Add to DatabaseHelper.java
// Insert these methods into the existing DatabaseHelper class

    /**
     * ✅ OPTIMIZED: Get total income for current month using SQL SUM
     * Replaces Java loop calculation in MainActivity
     *
     * @param userId User ID
     * @param startDate Month start timestamp
     * @param endDate Month end timestamp
     * @return Total income amount in VND
     */
    public double getMonthlyIncomeOptimized(int userId, long startDate, long endDate) {
        SQLiteDatabase db = this.getReadableDatabase();
        double total = 0;

        try {
            // Use SQL SUM instead of Java loop
            String query = "SELECT SUM(" + KEY_EXPENSE_AMOUNT + ") as total FROM " + TABLE_EXPENSES
                    + " WHERE " + KEY_EXPENSE_USER_ID + "=?"
                    + " AND " + KEY_EXPENSE_TYPE + "=" + Expense.TYPE_INCOME
                    + " AND " + KEY_EXPENSE_DATE + " BETWEEN ? AND ?";

            Cursor cursor = db.rawQuery(query, new String[]{
                    String.valueOf(userId),
                    String.valueOf(startDate),
                    String.valueOf(endDate)
            });

            if (cursor != null && cursor.moveToFirst()) {
                int totalIndex = cursor.getColumnIndex("total");
                if (totalIndex >= 0 && !cursor.isNull(totalIndex)) {
                    total = cursor.getDouble(totalIndex);
                }
                cursor.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error calculating monthly income: " + e.getMessage());
            e.printStackTrace();
        }

        return total;
    }

    /**
     * ✅ OPTIMIZED: Get total expense for current month using SQL SUM
     *
     * @param userId User ID
     * @param startDate Month start timestamp
     * @param endDate Month end timestamp
     * @return Total expense amount in VND
     */
    public double getMonthlyExpenseOptimized(int userId, long startDate, long endDate) {
        SQLiteDatabase db = this.getReadableDatabase();
        double total = 0;

        try {
            String query = "SELECT SUM(" + KEY_EXPENSE_AMOUNT + ") as total FROM " + TABLE_EXPENSES
                    + " WHERE " + KEY_EXPENSE_USER_ID + "=?"
                    + " AND " + KEY_EXPENSE_TYPE + "=" + Expense.TYPE_EXPENSE
                    + " AND " + KEY_EXPENSE_DATE + " BETWEEN ? AND ?";

            Cursor cursor = db.rawQuery(query, new String[]{
                    String.valueOf(userId),
                    String.valueOf(startDate),
                    String.valueOf(endDate)
            });

            if (cursor != null && cursor.moveToFirst()) {
                int totalIndex = cursor.getColumnIndex("total");
                if (totalIndex >= 0 && !cursor.isNull(totalIndex)) {
                    total = cursor.getDouble(totalIndex);
                }
                cursor.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error calculating monthly expense: " + e.getMessage());
            e.printStackTrace();
        }

        return total;
    }

    /**
     * ✅ OPTIMIZED: Get expense count for current month using SQL COUNT
     *
     * @param userId User ID
     * @param startDate Month start timestamp
     * @param endDate Month end timestamp
     * @return Number of expenses in period
     */
    public int getMonthlyExpenseCountOptimized(int userId, long startDate, long endDate) {
        SQLiteDatabase db = this.getReadableDatabase();
        int count = 0;

        try {
            String query = "SELECT COUNT(*) as count FROM " + TABLE_EXPENSES
                    + " WHERE " + KEY_EXPENSE_USER_ID + "=?"
                    + " AND " + KEY_EXPENSE_DATE + " BETWEEN ? AND ?";

            Cursor cursor = db.rawQuery(query, new String[]{
                    String.valueOf(userId),
                    String.valueOf(startDate),
                    String.valueOf(endDate)
            });

            if (cursor != null && cursor.moveToFirst()) {
                int countIndex = cursor.getColumnIndex("count");
                if (countIndex >= 0) {
                    count = cursor.getInt(countIndex);
                }
                cursor.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error counting monthly expenses: " + e.getMessage());
            e.printStackTrace();
        }

        return count;
    }

    /**
     * ✅ OPTIMIZED: Get top spending category for current month using SQL GROUP BY
     *
     * @param userId User ID
     * @param startDate Month start timestamp
     * @param endDate Month end timestamp
     * @return Map with category_id -> total_amount
     */
    public Map<Integer, Double> getTopCategoryOptimized(int userId, long startDate, long endDate) {
        SQLiteDatabase db = this.getReadableDatabase();
        Map<Integer, Double> categoryTotals = new HashMap<>();

        try {
            // GROUP BY category and SUM amounts, ORDER BY total DESC
            String query = "SELECT " + KEY_EXPENSE_CATEGORY_ID + ", "
                    + "SUM(" + KEY_EXPENSE_AMOUNT + ") as total "
                    + "FROM " + TABLE_EXPENSES
                    + " WHERE " + KEY_EXPENSE_USER_ID + "=?"
                    + " AND " + KEY_EXPENSE_TYPE + "=" + Expense.TYPE_EXPENSE
                    + " AND " + KEY_EXPENSE_DATE + " BETWEEN ? AND ?"
                    + " GROUP BY " + KEY_EXPENSE_CATEGORY_ID
                    + " ORDER BY total DESC"
                    + " LIMIT 1";

            Cursor cursor = db.rawQuery(query, new String[]{
                    String.valueOf(userId),
                    String.valueOf(startDate),
                    String.valueOf(endDate)
            });

            if (cursor != null && cursor.moveToFirst()) {
                int categoryId = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_EXPENSE_CATEGORY_ID));
                double total = cursor.getDouble(cursor.getColumnIndexOrThrow("total"));
                categoryTotals.put(categoryId, total);
                cursor.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting top category: " + e.getMessage());
            e.printStackTrace();
        }

        return categoryTotals;
    }

    /**
     * ✅ OPTIMIZED: Get all dashboard data in ONE query (best performance)
     * Returns a DashboardData object with all needed info
     *
     * @param userId User ID
     * @param startDate Month start timestamp
     * @param endDate Month end timestamp
     * @return DashboardData object containing all stats
     */
    public DashboardData getDashboardDataOptimized(int userId, long startDate, long endDate) {
        SQLiteDatabase db = this.getReadableDatabase();
        DashboardData data = new DashboardData();

        try {
            // Single query to get income, expense, and count
            String query = "SELECT "
                    + "SUM(CASE WHEN " + KEY_EXPENSE_TYPE + "=" + Expense.TYPE_INCOME
                    + " THEN " + KEY_EXPENSE_AMOUNT + " ELSE 0 END) as total_income, "
                    + "SUM(CASE WHEN " + KEY_EXPENSE_TYPE + "=" + Expense.TYPE_EXPENSE
                    + " THEN " + KEY_EXPENSE_AMOUNT + " ELSE 0 END) as total_expense, "
                    + "COUNT(*) as expense_count "
                    + "FROM " + TABLE_EXPENSES
                    + " WHERE " + KEY_EXPENSE_USER_ID + "=?"
                    + " AND " + KEY_EXPENSE_DATE + " BETWEEN ? AND ?";

            Cursor cursor = db.rawQuery(query, new String[]{
                    String.valueOf(userId),
                    String.valueOf(startDate),
                    String.valueOf(endDate)
            });

            if (cursor != null && cursor.moveToFirst()) {
                data.totalIncome = cursor.getDouble(cursor.getColumnIndexOrThrow("total_income"));
                data.totalExpense = cursor.getDouble(cursor.getColumnIndexOrThrow("total_expense"));
                data.expenseCount = cursor.getInt(cursor.getColumnIndexOrThrow("expense_count"));
                cursor.close();
            }

            // Get top category (separate query due to GROUP BY)
            data.topCategoryMap = getTopCategoryOptimized(userId, startDate, endDate);

        } catch (Exception e) {
            Log.e(TAG, "Error getting dashboard data: " + e.getMessage());
            e.printStackTrace();
        }

        return data;
    }

    /**
     * Helper class to hold dashboard statistics
     */
    public static class DashboardData {
        public double totalIncome = 0;
        public double totalExpense = 0;
        public int expenseCount = 0;
        public Map<Integer, Double> topCategoryMap = new HashMap<>();

        public double getBalance() {
            return totalIncome - totalExpense;
        }
    }

    // =============== ADD NEW METHODS FOR FEEDBACK ===============

    /**
     * Insert feedback into database
     * @param userId User ID
     * @param rating Rating (1-5 stars)
     * @param content Feedback content
     * @return Feedback ID or -1 if failed
     */
    public long insertFeedback(int userId, int rating, String content) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_FEEDBACK_USER_ID, userId);
        values.put(KEY_FEEDBACK_RATING, rating);
        values.put(KEY_FEEDBACK_CONTENT, content);
        values.put(KEY_FEEDBACK_TIMESTAMP, System.currentTimeMillis());

        long id = db.insert(TABLE_FEEDBACK, null, values);
        Log.d(TAG, "Feedback inserted: " + id + " (Rating: " + rating + " stars)");

        return id;
    }

    /**
     * Get all feedback from a user
     * @param userId User ID
     * @return List of feedback
     */
    public List<Feedback> getFeedbackByUser(int userId) {
        List<Feedback> feedbackList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_FEEDBACK, null,
                KEY_FEEDBACK_USER_ID + "=?",
                new String[]{String.valueOf(userId)},
                null, null,
                KEY_FEEDBACK_TIMESTAMP + " DESC");

        if (cursor != null && cursor.moveToFirst()) {
            do {
                Feedback feedback = cursorToFeedback(cursor);
                feedbackList.add(feedback);
            } while (cursor.moveToNext());
            cursor.close();
        }

        return feedbackList;
    }

    /**
     * Get feedback count for a user
     * @param userId User ID
     * @return Number of feedback submitted
     */
    public int getFeedbackCount(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        int count = 0;

        try {
            String query = "SELECT COUNT(*) as count FROM " + TABLE_FEEDBACK
                    + " WHERE " + KEY_FEEDBACK_USER_ID + "=?";

            Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(userId)});

            if (cursor != null && cursor.moveToFirst()) {
                int countIndex = cursor.getColumnIndex("count");
                if (countIndex >= 0) {
                    count = cursor.getInt(countIndex);
                }
                cursor.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error counting feedback: " + e.getMessage());
            e.printStackTrace();
        }

        return count;
    }

    /**
     * Get average rating for a user
     * @param userId User ID
     * @return Average rating (0-5) or 0 if no feedback
     */
    public float getAverageRating(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        float avgRating = 0;

        try {
            String query = "SELECT AVG(" + KEY_FEEDBACK_RATING + ") as avg_rating FROM " + TABLE_FEEDBACK
                    + " WHERE " + KEY_FEEDBACK_USER_ID + "=?";

            Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(userId)});

            if (cursor != null && cursor.moveToFirst()) {
                int avgIndex = cursor.getColumnIndex("avg_rating");
                if (avgIndex >= 0 && !cursor.isNull(avgIndex)) {
                    avgRating = cursor.getFloat(avgIndex);
                }
                cursor.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error calculating average rating: " + e.getMessage());
            e.printStackTrace();
        }

        return avgRating;
    }

    /**
     * Delete feedback
     * @param feedbackId Feedback ID
     * @return Number of rows affected
     */
    public int deleteFeedback(int feedbackId) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLE_FEEDBACK, KEY_ID + "=?",
                new String[]{String.valueOf(feedbackId)});
    }

    /**
     * Helper method to convert cursor to Feedback object
     */
    private Feedback cursorToFeedback(Cursor cursor) {
        return new Feedback(
                cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ID)),
                cursor.getInt(cursor.getColumnIndexOrThrow(KEY_FEEDBACK_USER_ID)),
                cursor.getInt(cursor.getColumnIndexOrThrow(KEY_FEEDBACK_RATING)),
                cursor.getString(cursor.getColumnIndexOrThrow(KEY_FEEDBACK_CONTENT)),
                cursor.getLong(cursor.getColumnIndexOrThrow(KEY_FEEDBACK_TIMESTAMP))
        );
    }
}