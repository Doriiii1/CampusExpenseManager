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
import com.example.campusexpensemanager.models.User;

import java.util.ArrayList;
import java.util.List;

/**
 * DatabaseHelper manages SQLite database for CampusExpense Manager
 * Handles CRUD operations for User, Category, Expense, Budget, and Currency tables
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = "DatabaseHelper";

    // Database Info
    private static final String DATABASE_NAME = "CampusExpense.db";
    private static final int DATABASE_VERSION = 1;

    // Table Names
    private static final String TABLE_USERS = "users";
    private static final String TABLE_CATEGORIES = "categories";
    private static final String TABLE_EXPENSES = "expenses";
    private static final String TABLE_BUDGETS = "budgets";
    private static final String TABLE_CURRENCIES = "currencies";

    // Common Column Names
    private static final String KEY_ID = "id";
    private static final String KEY_CREATED_AT = "created_at";

    // User Table Columns
    private static final String KEY_USER_EMAIL = "email";
    private static final String KEY_USER_PASSWORD = "password_hash";
    private static final String KEY_USER_NAME = "name";
    private static final String KEY_USER_ADDRESS = "address";
    private static final String KEY_USER_PHONE = "phone";
    private static final String KEY_USER_AVATAR = "avatar_path";
    private static final String KEY_USER_DARK_MODE = "dark_mode_enabled";

    // Category Table Columns
    private static final String KEY_CATEGORY_NAME = "name";
    private static final String KEY_CATEGORY_ICON = "icon_resource";

    // Expense Table Columns
    private static final String KEY_EXPENSE_USER_ID = "user_id";
    private static final String KEY_EXPENSE_CATEGORY_ID = "category_id";
    private static final String KEY_EXPENSE_CURRENCY_ID = "currency_id";
    private static final String KEY_EXPENSE_AMOUNT = "amount";
    private static final String KEY_EXPENSE_DATE = "date";
    private static final String KEY_EXPENSE_DESCRIPTION = "description";
    private static final String KEY_EXPENSE_RECEIPT = "receipt_path";

    // Budget Table Columns
    private static final String KEY_BUDGET_USER_ID = "user_id";
    private static final String KEY_BUDGET_CATEGORY_ID = "category_id";
    private static final String KEY_BUDGET_AMOUNT = "amount";
    private static final String KEY_BUDGET_PERIOD_START = "period_start";
    private static final String KEY_BUDGET_PERIOD_END = "period_end";

    // Currency Table Columns
    private static final String KEY_CURRENCY_CODE = "code";
    private static final String KEY_CURRENCY_RATE = "rate_to_vnd";
    private static final String KEY_CURRENCY_UPDATED = "last_updated";

    private static DatabaseHelper instance;

    // Singleton pattern
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

        // Create Users Table
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

        // Create Categories Table
        String CREATE_CATEGORIES_TABLE = "CREATE TABLE " + TABLE_CATEGORIES + "("
                + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_CATEGORY_NAME + " TEXT NOT NULL,"
                + KEY_CATEGORY_ICON + " TEXT"
                + ")";
        db.execSQL(CREATE_CATEGORIES_TABLE);

        // Create Currencies Table
        String CREATE_CURRENCIES_TABLE = "CREATE TABLE " + TABLE_CURRENCIES + "("
                + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_CURRENCY_CODE + " TEXT UNIQUE NOT NULL,"
                + KEY_CURRENCY_RATE + " REAL DEFAULT 1,"
                + KEY_CURRENCY_UPDATED + " INTEGER"
                + ")";
        db.execSQL(CREATE_CURRENCIES_TABLE);

        // Create Expenses Table with Foreign Keys
        String CREATE_EXPENSES_TABLE = "CREATE TABLE " + TABLE_EXPENSES + "("
                + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_EXPENSE_USER_ID + " INTEGER NOT NULL,"
                + KEY_EXPENSE_CATEGORY_ID + " INTEGER NOT NULL,"
                + KEY_EXPENSE_CURRENCY_ID + " INTEGER DEFAULT 1,"
                + KEY_EXPENSE_AMOUNT + " REAL NOT NULL,"
                + KEY_EXPENSE_DATE + " INTEGER NOT NULL,"
                + KEY_EXPENSE_DESCRIPTION + " TEXT,"
                + KEY_EXPENSE_RECEIPT + " TEXT,"
                + KEY_CREATED_AT + " INTEGER NOT NULL,"
                + "FOREIGN KEY(" + KEY_EXPENSE_USER_ID + ") REFERENCES "
                + TABLE_USERS + "(" + KEY_ID + ") ON DELETE CASCADE,"
                + "FOREIGN KEY(" + KEY_EXPENSE_CATEGORY_ID + ") REFERENCES "
                + TABLE_CATEGORIES + "(" + KEY_ID + "),"
                + "FOREIGN KEY(" + KEY_EXPENSE_CURRENCY_ID + ") REFERENCES "
                + TABLE_CURRENCIES + "(" + KEY_ID + ")"
                + ")";
        db.execSQL(CREATE_EXPENSES_TABLE);

        // Create Budgets Table with Foreign Keys
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

        // Enable foreign key constraints
        db.execSQL("PRAGMA foreign_keys=ON");

        // Pre-populate categories
        prepopulateCategories(db);

        // Pre-populate currencies
        prepopulateCurrencies(db);

        Log.d(TAG, "Database tables created successfully");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion);

        // Drop older tables if existed
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_BUDGETS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_EXPENSES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CURRENCIES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CATEGORIES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);

        // Create tables again
        onCreate(db);
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        // Enable foreign key constraints
        db.execSQL("PRAGMA foreign_keys=ON");
    }

    /**
     * Pre-populate categories with default expense types
     */
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
                "Others", "ic_others"
        };

        for (int i = 0; i < categories.length; i += 2) {
            ContentValues values = new ContentValues();
            values.put(KEY_CATEGORY_NAME, categories[i]);
            values.put(KEY_CATEGORY_ICON, categories[i + 1]);
            db.insert(TABLE_CATEGORIES, null, values);
        }

        Log.d(TAG, "Pre-populated " + (categories.length / 2) + " categories");
    }

    /**
     * Pre-populate currencies with VND default
     */
    private void prepopulateCurrencies(SQLiteDatabase db) {
        ContentValues values = new ContentValues();
        values.put(KEY_CURRENCY_CODE, "VND");
        values.put(KEY_CURRENCY_RATE, 1.0);
        values.put(KEY_CURRENCY_UPDATED, System.currentTimeMillis());
        db.insert(TABLE_CURRENCIES, null, values);

        Log.d(TAG, "Pre-populated VND currency");
    }

    // =============== USER CRUD OPERATIONS ===============

    /**
     * Insert new user into database
     * @param user User object to insert
     * @return User ID if successful, -1 if failed
     */
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

        long id = db.insert(TABLE_USERS, null, values);
        Log.d(TAG, "User inserted with ID: " + id);

        return id;
    }

    /**
     * Get user by ID
     * @param userId User ID
     * @return User object or null if not found
     */
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

    /**
     * Get user by email
     * @param email User email
     * @return User object or null if not found
     */
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

    /**
     * Update user information
     * @param user User object with updated data
     * @return Number of rows affected
     */
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

        int rowsAffected = db.update(TABLE_USERS, values, KEY_ID + "=?",
                new String[]{String.valueOf(user.getId())});

        Log.d(TAG, "User updated: " + rowsAffected + " rows");
        return rowsAffected;
    }

    /**
     * Delete user by ID
     * @param userId User ID
     * @return Number of rows deleted
     */
    public int deleteUser(int userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rowsDeleted = db.delete(TABLE_USERS, KEY_ID + "=?",
                new String[]{String.valueOf(userId)});

        Log.d(TAG, "User deleted: " + rowsDeleted + " rows");
        return rowsDeleted;
    }

    // =============== CATEGORY CRUD OPERATIONS ===============

    /**
     * Get all categories
     * @return List of Category objects
     */
    public List<Category> getAllCategories() {
        List<Category> categories = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_CATEGORIES, null, null, null, null, null, KEY_CATEGORY_NAME);

        if (cursor != null && cursor.moveToFirst()) {
            do {
                Category category = cursorToCategory(cursor);
                categories.add(category);
            } while (cursor.moveToNext());
            cursor.close();
        }

        return categories;
    }

    /**
     * Get category by ID
     * @param categoryId Category ID
     * @return Category object or null
     */
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

    // =============== EXPENSE CRUD OPERATIONS ===============

    /**
     * Insert new expense
     * @param expense Expense object
     * @return Expense ID if successful, -1 if failed
     */
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
        values.put(KEY_CREATED_AT, expense.getCreatedAt());

        long id = db.insert(TABLE_EXPENSES, null, values);
        Log.d(TAG, "Expense inserted with ID: " + id);

        return id;
    }

    /**
     * Get all expenses for a user
     * @param userId User ID
     * @return List of Expense objects
     */
    public List<Expense> getExpensesByUser(int userId) {
        List<Expense> expenses = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_EXPENSES, null, KEY_EXPENSE_USER_ID + "=?",
                new String[]{String.valueOf(userId)}, null, null, KEY_EXPENSE_DATE + " DESC");

        if (cursor != null && cursor.moveToFirst()) {
            do {
                Expense expense = cursorToExpense(cursor);
                expenses.add(expense);
            } while (cursor.moveToNext());
            cursor.close();
        }

        return expenses;
    }

    /**
     * Update expense
     * @param expense Expense object with updated data
     * @return Number of rows affected
     */
    public int updateExpense(Expense expense) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_EXPENSE_CATEGORY_ID, expense.getCategoryId());
        values.put(KEY_EXPENSE_CURRENCY_ID, expense.getCurrencyId());
        values.put(KEY_EXPENSE_AMOUNT, expense.getAmount());
        values.put(KEY_EXPENSE_DATE, expense.getDate());
        values.put(KEY_EXPENSE_DESCRIPTION, expense.getDescription());
        values.put(KEY_EXPENSE_RECEIPT, expense.getReceiptPath());

        int rowsAffected = db.update(TABLE_EXPENSES, values, KEY_ID + "=?",
                new String[]{String.valueOf(expense.getId())});

        Log.d(TAG, "Expense updated: " + rowsAffected + " rows");
        return rowsAffected;
    }

    /**
     * Delete expense
     * @param expenseId Expense ID
     * @return Number of rows deleted
     */
    public int deleteExpense(int expenseId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rowsDeleted = db.delete(TABLE_EXPENSES, KEY_ID + "=?",
                new String[]{String.valueOf(expenseId)});

        Log.d(TAG, "Expense deleted: " + rowsDeleted + " rows");
        return rowsDeleted;
    }

    // =============== BUDGET CRUD OPERATIONS ===============

    /**
     * Insert new budget
     * @param budget Budget object
     * @return Budget ID if successful, -1 if failed
     */
    public long insertBudget(Budget budget) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_BUDGET_USER_ID, budget.getUserId());
        values.put(KEY_BUDGET_CATEGORY_ID, budget.getCategoryId());
        values.put(KEY_BUDGET_AMOUNT, budget.getAmount());
        values.put(KEY_BUDGET_PERIOD_START, budget.getPeriodStart());
        values.put(KEY_BUDGET_PERIOD_END, budget.getPeriodEnd());
        values.put(KEY_CREATED_AT, budget.getCreatedAt());

        long id = db.insert(TABLE_BUDGETS, null, values);
        Log.d(TAG, "Budget inserted with ID: " + id);

        return id;
    }

    /**
     * Get all budgets for a user
     * @param userId User ID
     * @return List of Budget objects
     */
    public List<Budget> getBudgetsByUser(int userId) {
        List<Budget> budgets = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_BUDGETS, null, KEY_BUDGET_USER_ID + "=?",
                new String[]{String.valueOf(userId)}, null, null, KEY_BUDGET_PERIOD_END + " DESC");

        if (cursor != null && cursor.moveToFirst()) {
            do {
                Budget budget = cursorToBudget(cursor);
                budgets.add(budget);
            } while (cursor.moveToNext());
            cursor.close();
        }

        return budgets;
    }

    /**
     * Update budget
     * @param budget Budget object with updated data
     * @return Number of rows affected
     */
    public int updateBudget(Budget budget) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_BUDGET_CATEGORY_ID, budget.getCategoryId());
        values.put(KEY_BUDGET_AMOUNT, budget.getAmount());
        values.put(KEY_BUDGET_PERIOD_START, budget.getPeriodStart());
        values.put(KEY_BUDGET_PERIOD_END, budget.getPeriodEnd());

        int rowsAffected = db.update(TABLE_BUDGETS, values, KEY_ID + "=?",
                new String[]{String.valueOf(budget.getId())});

        Log.d(TAG, "Budget updated: " + rowsAffected + " rows");
        return rowsAffected;
    }

    /**
     * Delete budget
     * @param budgetId Budget ID
     * @return Number of rows deleted
     */
    public int deleteBudget(int budgetId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rowsDeleted = db.delete(TABLE_BUDGETS, KEY_ID + "=?",
                new String[]{String.valueOf(budgetId)});

        Log.d(TAG, "Budget deleted: " + rowsDeleted + " rows");
        return rowsDeleted;
    }

    // =============== HELPER METHODS ===============

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

    private Expense cursorToExpense(Cursor cursor) {
        return new Expense(
                cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ID)),
                cursor.getInt(cursor.getColumnIndexOrThrow(KEY_EXPENSE_USER_ID)),
                cursor.getInt(cursor.getColumnIndexOrThrow(KEY_EXPENSE_CATEGORY_ID)),
                cursor.getInt(cursor.getColumnIndexOrThrow(KEY_EXPENSE_CURRENCY_ID)),
                cursor.getDouble(cursor.getColumnIndexOrThrow(KEY_EXPENSE_AMOUNT)),
                cursor.getLong(cursor.getColumnIndexOrThrow(KEY_EXPENSE_DATE)),
                cursor.getString(cursor.getColumnIndexOrThrow(KEY_EXPENSE_DESCRIPTION)),
                cursor.getString(cursor.getColumnIndexOrThrow(KEY_EXPENSE_RECEIPT)),
                cursor.getLong(cursor.getColumnIndexOrThrow(KEY_CREATED_AT))
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
}