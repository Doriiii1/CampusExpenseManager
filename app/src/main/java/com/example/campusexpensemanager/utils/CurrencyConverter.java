package com.example.campusexpensemanager.utils;

import android.content.Context;

import com.example.campusexpensemanager.models.Currency;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * CurrencyConverter - Sprint 6
 * Handles currency conversion and formatting
 */
public class CurrencyConverter {

    private DatabaseHelper dbHelper;
    private static final String TAG = "CurrencyConverter";

    public CurrencyConverter(Context context) {
        this.dbHelper = DatabaseHelper.getInstance(context);
    }

    /**
     * Convert amount from one currency to another
     * @param amount Amount in source currency
     * @param fromCurrencyId Source currency ID
     * @param toCurrencyId Target currency ID
     * @return Converted amount
     */
    public double convert(double amount, int fromCurrencyId, int toCurrencyId) {
        if (fromCurrencyId == toCurrencyId) {
            return amount;
        }

        // Get currencies from database
        Currency fromCurrency = getCurrencyById(fromCurrencyId);
        Currency toCurrency = getCurrencyById(toCurrencyId);

        if (fromCurrency == null || toCurrency == null) {
            return amount; // Return original if currencies not found
        }

        // Convert: amount -> VND -> target currency
        double amountInVnd = amount * fromCurrency.getRateToVnd();
        double convertedAmount = amountInVnd / toCurrency.getRateToVnd();

        return convertedAmount;
    }

    /**
     * Format amount with currency symbol
     * @param amount Amount to format
     * @param currencyId Currency ID
     * @return Formatted string (e.g., "1,000,000đ" or "$100.00")
     */
    public String format(double amount, int currencyId) {
        Currency currency = getCurrencyById(currencyId);

        if (currency == null) {
            // Default to VND
            NumberFormat format = NumberFormat.getInstance(new Locale("vi", "VN"));
            return format.format(amount) + "đ";
        }

        return currency.formatAmount(amount);
    }

    /**
     * Format amount with sign for income/expense
     * @param amount Amount to format
     * @param currencyId Currency ID
     * @param isIncome true for income, false for expense
     * @return Formatted string with sign (e.g., "+1,000đ" or "-500đ")
     */
    public String formatWithSign(double amount, int currencyId, boolean isIncome) {
        String formatted = format(amount, currencyId);
        return (isIncome ? "+" : "-") + formatted;
    }

    /**
     * Get currency by ID (with caching)
     */
    private Currency getCurrencyById(int currencyId) {
        // Mock implementation - replace with actual database query
        if (currencyId == 1) {
            return new Currency(1, "VND", 1.0, System.currentTimeMillis());
        } else if (currencyId == 2) {
            return new Currency(2, "USD", 24000.0, System.currentTimeMillis());
        }
        return null;
    }

    /**
     * Get all available currencies
     */
    public java.util.List<Currency> getAllCurrencies() {
        java.util.List<Currency> currencies = new java.util.ArrayList<>();
        currencies.add(new Currency(1, "VND", 1.0, System.currentTimeMillis()));
        currencies.add(new Currency(2, "USD", 24000.0, System.currentTimeMillis()));
        return currencies;
    }

    /**
     * Update currency exchange rate
     */
    public void updateExchangeRate(int currencyId, double newRate) {
        // TODO: Implement database update
        // dbHelper.updateCurrencyRate(currencyId, newRate);
    }
}