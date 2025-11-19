package com.example.campusexpensemanager.models;

import android.content.Context;

import com.example.campusexpensemanager.utils.DatabaseHelper;

/**
 * Category model class for expense categorization
 * ✅ ENHANCED: Supports localization via category keys
 */
public class Category {
    private int id;
    private String name; // NOW STORES KEY like "cat_food"
    private String iconResource; // Resource name for icon (e.g., "ic_food")

    // Default constructor
    public Category() {
    }

    // Constructor without ID (for insertion)
    public Category(String name, String iconResource) {
        this.name = name;
        this.iconResource = iconResource;
    }

    // Full constructor
    public Category(int id, String name, String iconResource) {
        this.id = id;
        this.name = name;
        this.iconResource = iconResource;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name; // Returns KEY like "cat_food"
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIconResource() {
        return iconResource;
    }

    public void setIconResource(String iconResource) {
        this.iconResource = iconResource;
    }

    /**
     * ✅ NEW: Get localized display name
     * @param context Context for string resources
     * @return Localized name based on current locale
     */
    public String getLocalizedName(Context context) {
        return DatabaseHelper.getLocalizedCategoryName(context, this.name);
    }

    @Override
    public String toString() {
        // Used by Spinner - returns KEY (will be localized in Adapter)
        return name;
    }
}