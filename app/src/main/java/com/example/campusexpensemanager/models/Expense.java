package com.example.campusexpensemanager.models;

/**
 * Expense model - Enhanced for Sprint 5
 * NEW: Recurring expenses, Income tracking
 */
public class Expense {
    // Transaction types
    public static final int TYPE_EXPENSE = 0; // Chi tiêu (red)
    public static final int TYPE_INCOME = 1;  // Thu nhập (green)

    // Recurrence periods
    public static final String PERIOD_DAILY = "daily";
    public static final String PERIOD_WEEKLY = "weekly";
    public static final String PERIOD_MONTHLY = "monthly";

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

    // NEW: Recurring fields
    private boolean isRecurring;
    private String recurrencePeriod; // daily/weekly/monthly
    private long nextOccurrenceDate; // Unix timestamp

    // Default constructor
    public Expense() {
        this.createdAt = System.currentTimeMillis();
        this.date = System.currentTimeMillis();
        this.type = TYPE_EXPENSE;
        this.currencyId = 1; // Default VND
        this.isRecurring = false;
    }

    // Constructor with essential fields
    public Expense(int userId, int categoryId, double amount, long date, String description) {
        this.userId = userId;
        this.categoryId = categoryId;
        this.currencyId = 1; // Default VND
        this.amount = amount;
        this.date = date;
        this.description = description;
        this.createdAt = System.currentTimeMillis();
        this.type = TYPE_EXPENSE;
        this.isRecurring = false;
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
        this.isRecurring = false;
    }

    // Full constructor (from database)
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
        this.isRecurring = false;
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

    // NEW: Recurring getters/setters
    public boolean isRecurring() {
        return isRecurring;
    }

    public void setIsRecurring(boolean isRecurring) {
        this.isRecurring = isRecurring;
    }

    public String getRecurrencePeriod() {
        return recurrencePeriod;
    }

    public void setRecurrencePeriod(String recurrencePeriod) {
        this.recurrencePeriod = recurrencePeriod;
    }

    public long getNextOccurrenceDate() {
        return nextOccurrenceDate;
    }

    public void setNextOccurrenceDate(long nextOccurrenceDate) {
        this.nextOccurrenceDate = nextOccurrenceDate;
    }

    // Helper methods
    public boolean isIncome() {
        return type == TYPE_INCOME;
    }

    public boolean isExpense() {
        return type == TYPE_EXPENSE;
    }

    /**
     * Get formatted amount with sign
     * @return "+1,000đ" for income, "-500đ" for expense
     */
    public String getFormattedAmount() {
        java.text.NumberFormat format = java.text.NumberFormat.getInstance(new java.util.Locale("vi", "VN"));
        String amountStr = format.format(amount) + "đ";

        if (isIncome()) {
            return "+" + amountStr;
        } else {
            return "-" + amountStr;
        }
    }

    /**
     * Get color resource based on type
     * @return Color res ID
     */
    public int getAmountColor(android.content.Context context) {
        if (isIncome()) {
            return context.getResources().getColor(
                    context.getResources().getIdentifier("success", "color", context.getPackageName())
            );
        } else {
            return context.getResources().getColor(
                    context.getResources().getIdentifier("error", "color", context.getPackageName())
            );
        }
    }

    @Override
    public String toString() {
        return "Expense{" +
                "id=" + id +
                ", userId=" + userId +
                ", categoryId=" + categoryId +
                ", amount=" + amount +
                ", type=" + (type == TYPE_INCOME ? "INCOME" : "EXPENSE") +
                ", recurring=" + isRecurring +
                ", period=" + recurrencePeriod +
                ", date=" + date +
                '}';
    }
}