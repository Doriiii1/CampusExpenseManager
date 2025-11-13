package com.example.campusexpensemanager.models;

/**
 * User model class representing a user in the database
 * Stores personal information and authentication data
 */
public class User {
    private int id;
    private String email;
    private String passwordHash;
    private String name;
    private String address;
    private String phone;
    private String avatarPath;
    private boolean darkModeEnabled;
    private long createdAt;

    // Default constructor
    public User() {
        this.createdAt = System.currentTimeMillis();
        this.darkModeEnabled = false;
    }

    // Constructor with essential fields
    public User(String email, String passwordHash, String name, String address, String phone) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.name = name;
        this.address = address;
        this.phone = phone;
        this.createdAt = System.currentTimeMillis();
        this.darkModeEnabled = false;
    }

    // Full constructor
    public User(int id, String email, String passwordHash, String name, String address,
                String phone, String avatarPath, boolean darkModeEnabled, long createdAt) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.name = name;
        this.address = address;
        this.phone = phone;
        this.avatarPath = avatarPath;
        this.darkModeEnabled = darkModeEnabled;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAvatarPath() {
        return avatarPath;
    }

    public void setAvatarPath(String avatarPath) {
        this.avatarPath = avatarPath;
    }

    public boolean isDarkModeEnabled() {
        return darkModeEnabled;
    }

    public void setDarkModeEnabled(boolean darkModeEnabled) {
        this.darkModeEnabled = darkModeEnabled;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", email='" + email + '\'' +
                ", name='" + name + '\'' +
                ", address='" + address + '\'' +
                ", phone='" + phone + '\'' +
                ", darkModeEnabled=" + darkModeEnabled +
                '}';
    }
}