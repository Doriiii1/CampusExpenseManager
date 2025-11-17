package com.example.campusexpensemanager.models;

/**
 * ExpenseTemplate model for quick-add expense templates
 * Sprint 5: Quick templates like "Tiền trọ", "Ăn sáng", etc.
 */
public class ExpenseTemplate {
    private int id;
    private String name;
    private int categoryId;
    private double defaultAmount;
    private String iconEmoji;

    // Default constructor
    public ExpenseTemplate() {
    }

    // Constructor without ID
    public ExpenseTemplate(String name, int categoryId, double defaultAmount, String iconEmoji) {
        this.name = name;
        this.categoryId = categoryId;
        this.defaultAmount = defaultAmount;
        this.iconEmoji = iconEmoji;
    }

    // Full constructor
    public ExpenseTemplate(int id, String name, int categoryId, double defaultAmount, String iconEmoji) {
        this.id = id;
        this.name = name;
        this.categoryId = categoryId;
        this.defaultAmount = defaultAmount;
        this.iconEmoji = iconEmoji;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }

    public double getDefaultAmount() {
        return defaultAmount;
    }

    public void setDefaultAmount(double defaultAmount) {
        this.defaultAmount = defaultAmount;
    }

    public String getIconEmoji() {
        return iconEmoji;
    }

    public void setIconEmoji(String iconEmoji) {
        this.iconEmoji = iconEmoji;
    }

    /**
     * Get display text for chip/button
     */
    public String getDisplayText() {
        return iconEmoji + " " + name;
    }

    @Override
    public String toString() {
        return "ExpenseTemplate{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", categoryId=" + categoryId +
                ", defaultAmount=" + defaultAmount +
                ", icon='" + iconEmoji + '\'' +
                '}';
    }
}