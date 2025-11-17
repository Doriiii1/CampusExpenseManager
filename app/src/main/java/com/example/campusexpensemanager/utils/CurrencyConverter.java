package com.example.campusexpensemanager.utils;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.campusexpensemanager.models.Currency;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * CurrencyConverter - Sprint 6 Complete
 * Handles currency conversion with proper DB queries and caching
 */
public class CurrencyConverter {

    private DatabaseHelper dbHelper;
    private static final String TAG = "CurrencyConverter";

    // Cache currencies to avoid repeated DB queries
    private Map<Integer, Currency> currencyCache = new HashMap<>();
    private long lastCacheUpdate = 0;
    private static final long CACHE_DURATION = 5 * 60 * 1000; // 5 minutes

    public CurrencyConverter(Context context) {
        this.dbHelper = DatabaseHelper.getInstance(context);
        loadCurrenciesFromDB();
    }

    /**
     * Load all currencies from database into cache
     */
    private void loadCurrenciesFromDB() {
        long currentTime = System.currentTimeMillis();

        // Refresh cache if expired
        if (currentTime - lastCacheUpdate < CACHE_DURATION && !currencyCache.isEmpty()) {
            return;
        }

        currencyCache.clear();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query("currencies", null, null, null, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                String code = cursor.getString(cursor.getColumnIndexOrThrow("code"));
                double rate = cursor.getDouble(cursor.getColumnIndexOrThrow("rate_to_vnd"));
                long updated = cursor.getLong(cursor.getColumnIndexOrThrow("last_updated"));

                Currency currency = new Currency(id, code, rate, updated);
                currencyCache.put(id, currency);
            } while (cursor.moveToNext());
            cursor.close();
        }

        lastCacheUpdate = currentTime;
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

        loadCurrenciesFromDB(); // Refresh cache if needed

        Currency fromCurrency = currencyCache.get(fromCurrencyId);
        Currency toCurrency = currencyCache.get(toCurrencyId);

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
        loadCurrenciesFromDB();
        Currency currency = currencyCache.get(currencyId);

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
     * @param isIncome true for income (+), false for expense (-)
     * @return Formatted string with sign (e.g., "+1,000đ" or "-$500")
     */
    public String formatWithSign(double amount, int currencyId, boolean isIncome) {
        String formatted = format(amount, currencyId);
        return (isIncome ? "+" : "-") + formatted;
    }

    /**
     * Get currency by ID with caching
     */
    public Currency getCurrencyById(int currencyId) {
        loadCurrenciesFromDB();
        return currencyCache.get(currencyId);
    }

    /**
     * Get all available currencies
     */
    public List<Currency> getAllCurrencies() {
        loadCurrenciesFromDB();
        return new ArrayList<>(currencyCache.values());
    }

    /**
     * Update currency exchange rate in database
     */
    public void updateExchangeRate(int currencyId, double newRate) {
        Currency currency = currencyCache.get(currencyId);
        if (currency != null) {
            currency.setRateToVnd(newRate);
            currency.setLastUpdated(System.currentTimeMillis());

            // Update in DB
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            db.execSQL("UPDATE currencies SET rate_to_vnd = ?, last_updated = ? WHERE id = ?",
                    new Object[]{newRate, currency.getLastUpdated(), currencyId});

            // Refresh cache
            lastCacheUpdate = 0;
        }
    }

    /**
     * Clear cache (useful for testing or forcing refresh)
     */
    public void clearCache() {
        currencyCache.clear();
        lastCacheUpdate = 0;
    }
}