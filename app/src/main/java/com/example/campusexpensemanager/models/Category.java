package com.example.campusexpensemanager.models;

/**
 * Category model class for expense categorization
 * Pre-populated categories like Food, Transport, Study, etc.
 */
public class Category {
    private int id;
    private String name;
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
        return name;
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

    @Override
    public String toString() {
        return name; // For Spinner display
    }
}