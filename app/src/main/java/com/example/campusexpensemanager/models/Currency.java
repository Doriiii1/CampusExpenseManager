package com.example.campusexpensemanager.models;

/**
 * Currency model for multi-currency support
 * Sprint 5: VND and USD support
 */
public class Currency {
    private int id;
    private String code; // VND, USD
    private double rateToVnd; // Conversion rate to VND
    private long lastUpdated;

    // Default constructor
    public Currency() {
    }

    // Constructor without ID
    public Currency(String code, double rateToVnd) {
        this.code = code;
        this.rateToVnd = rateToVnd;
        this.lastUpdated = System.currentTimeMillis();
    }

    // Full constructor
    public Currency(int id, String code, double rateToVnd, long lastUpdated) {
        this.id = id;
        this.code = code;
        this.rateToVnd = rateToVnd;
        this.lastUpdated = lastUpdated;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public double getRateToVnd() {
        return rateToVnd;
    }

    public void setRateToVnd(double rateToVnd) {
        this.rateToVnd = rateToVnd;
    }

    public long getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(long lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    /**
     * Get currency symbol
     */
    public String getSymbol() {
        switch (code) {
            case "USD":
                return "$";
            case "VND":
            default:
                return "₫";
        }
    }

    /**
     * Convert amount to VND
     */
    public double convertToVnd(double amount) {
        return amount * rateToVnd;
    }

    /**
     * Convert amount from VND
     */
    public double convertFromVnd(double vndAmount) {
        return vndAmount / rateToVnd;
    }

    /**
     * Format amount with currency
     */
    public String formatAmount(double amount) {
        java.text.NumberFormat format = java.text.NumberFormat.getInstance(new java.util.Locale("vi", "VN"));

        if (code.equals("VND")) {
            return format.format(amount) + "₫";
        } else {
            return "$" + String.format("%.2f", amount);
        }
    }

    @Override
    public String toString() {
        return code + " (" + getSymbol() + ")";
    }
}