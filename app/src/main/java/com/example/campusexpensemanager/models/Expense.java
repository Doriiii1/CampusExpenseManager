package com.example.campusexpensemanager.models;

/**
 * Expense model class representing a single expense transaction
 * Links to User, Category, and Currency tables
 */
public class Expense {
    private int id;
    private int userId;
    private int categoryId;
    private int currencyId;
    private double amount;
    private long date; // Unix timestamp in milliseconds
    private String description;
    private String receiptPath;
    private long createdAt;

    // Default constructor
    public Expense() {
        this.createdAt = System.currentTimeMillis();
        this.date = System.currentTimeMillis();
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

    @Override
    public String toString() {
        return "Expense{" +
                "id=" + id +
                ", userId=" + userId +
                ", categoryId=" + categoryId +
                ", amount=" + amount +
                ", date=" + date +
                ", description='" + description + '\'' +
                '}';
    }
}