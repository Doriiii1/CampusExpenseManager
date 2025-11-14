package com.example.campusexpensemanager.models;

/**
 * Expense model class representing a single expense/income transaction
 * Links to User, Category, and Currency tables
 */
public class Expense {
    // Transaction types
    public static final int TYPE_EXPENSE = 0; // Chi tiêu (red)
    public static final int TYPE_INCOME = 1;  // Thu nhập (green)

    private int id;
    private int userId;
    private int categoryId;
    private int currencyId;
    private double amount;
    private long date; // Unix timestamp in milliseconds
    private String description;
    private String receiptPath;
    private long createdAt;
    private int type; // 0 = expense, 1 = income

    // Default constructor
    public Expense() {
        this.createdAt = System.currentTimeMillis();
        this.date = System.currentTimeMillis();
        this.type = TYPE_EXPENSE; // Default to expense
    }

    // Constructor with essential fields (VND default, currencyId=1)
    public Expense(int userId, int categoryId, double amount, long date, String description) {
        this.userId = userId;
        this.categoryId = categoryId;
        this.currencyId = 1; // Default VND
        this.amount = amount;
        this.date = date;
        this.description = description;
        this.createdAt = System.currentTimeMillis();
        this.type = TYPE_EXPENSE; // Default to expense
    }

    // Constructor with type
    public Expense(int userId, int categoryId, double amount, long date, String description, int type) {
        this.userId = userId;
        this.categoryId = categoryId;
        this.currencyId = 1;
        this.amount = amount;
        this.date = date;
        this.description = description;
        this.createdAt = System.currentTimeMillis();
        this.type = type;
    }

    // Full constructor
    public Expense(int id, int userId, int categoryId, int currencyId, double amount,
                   long date, String description, String receiptPath, long createdAt) {
        this.id = id;
        this.userId = userId;
        this.categoryId = categoryId;
        this.currencyId = currencyId;
        this.amount = amount;
        this.date = date;
        this.description = description;
        this.receiptPath = receiptPath;
        this.createdAt = createdAt;
        this.type = TYPE_EXPENSE; // Default
    }

    // Full constructor with type
    public Expense(int id, int userId, int categoryId, int currencyId, double amount,
                   long date, String description, String receiptPath, long createdAt, int type) {
        this.id = id;
        this.userId = userId;
        this.categoryId = categoryId;
        this.currencyId = currencyId;
        this.amount = amount;
        this.date = date;
        this.description = description;
        this.receiptPath = receiptPath;
        this.createdAt = createdAt;
        this.type = type;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }

    public int getCurrencyId() {
        return currencyId;
    }

    public void setCurrencyId(int currencyId) {
        this.currencyId = currencyId;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getReceiptPath() {
        return receiptPath;
    }

    public void setReceiptPath(String receiptPath) {
        this.receiptPath = receiptPath;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public boolean isIncome() {
        return type == TYPE_INCOME;
    }

    public boolean isExpense() {
        return type == TYPE_EXPENSE;
    }

    @Override
    public String toString() {
        return "Expense{" +
                "id=" + id +
                ", userId=" + userId +
                ", categoryId=" + categoryId +
                ", amount=" + amount +
                ", type=" + (type == TYPE_INCOME ? "INCOME" : "EXPENSE") +
                ", date=" + date +
                ", description='" + description + '\'' +
                '}';
    }
}