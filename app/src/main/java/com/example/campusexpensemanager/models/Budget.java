package com.example.campusexpensemanager.models;

/**
 * Budget model class for budget tracking
 * Links to User and Category, tracks period and remaining amount
 */
public class Budget {
    private int id;
    private int userId;
    private int categoryId; // 0 for total budget, >0 for category-specific
    private double amount;
    private long periodStart; // Unix timestamp
    private long periodEnd; // Unix timestamp
    private long createdAt;

    // Default constructor
    public Budget() {
        this.createdAt = System.currentTimeMillis();
    }

    // Constructor with essential fields
    public Budget(int userId, int categoryId, double amount, long periodStart, long periodEnd) {
        this.userId = userId;
        this.categoryId = categoryId;
        this.amount = amount;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.createdAt = System.currentTimeMillis();
    }

    // Full constructor
    public Budget(int id, int userId, int categoryId, double amount, long periodStart,
                  long periodEnd, long createdAt) {
        this.id = id;
        this.userId = userId;
        this.categoryId = categoryId;
        this.amount = amount;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
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

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public long getPeriodStart() {
        return periodStart;
    }

    public void setPeriodStart(long periodStart) {
        this.periodStart = periodStart;
    }

    public long getPeriodEnd() {
        return periodEnd;
    }

    public void setPeriodEnd(long periodEnd) {
        this.periodEnd = periodEnd;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Calculate remaining budget based on spent amount
     * @param spent Total amount spent in this budget period
     * @return Remaining budget amount
     */
    public double calculateRemaining(double spent) {
        return amount - spent;
    }

    /**
     * Calculate percentage of budget spent
     * @param spent Total amount spent
     * @return Percentage (0-100+)
     */
    public double calculatePercentageSpent(double spent) {
        if (amount == 0) return 0;
        return (spent / amount) * 100;
    }

    @Override
    public String toString() {
        return "Budget{" +
                "id=" + id +
                ", userId=" + userId +
                ", categoryId=" + categoryId +
                ", amount=" + amount +
                ", periodStart=" + periodStart +
                ", periodEnd=" + periodEnd +
                '}';
    }
}