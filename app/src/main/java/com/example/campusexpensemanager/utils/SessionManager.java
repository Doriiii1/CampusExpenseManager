package com.example.campusexpensemanager.utils;

import android.content.Context;
import android.content.SharedPreferences;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * SessionManager handles user session persistence and authentication state
 * Uses SharedPreferences to store user data and preferences
 */
public class SessionManager {

    private static final String PREF_NAME = "CampusExpenseSession";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_REMEMBER_ME = "remember_me";
    private static final String KEY_DARK_MODE = "dark_mode_enabled";
    private static final String KEY_LOGIN_ATTEMPTS = "login_attempts";
    private static final String KEY_LOCK_TIMESTAMP = "lock_timestamp";
    // ✅ NEW: Biometric authentication keys
    private static final String KEY_BIOMETRIC_ENABLED = "biometric_enabled";
    private static final String KEY_BIOMETRIC_EMAIL = "biometric_email"; // Email của user đã enable biometric

    private static final int MAX_LOGIN_ATTEMPTS = 3;
    private static final long LOCK_DURATION = 5 * 60 * 1000; // 5 minutes in milliseconds

    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;
    private Context context;

    public SessionManager(Context context) {
        this.context = context;
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }

    /**
     * Create login session
     * @param userId User ID from database
     * @param email User email
     * @param name User name
     * @param rememberMe Whether to persist session
     */
    public void createLoginSession(int userId, String email, String name, boolean rememberMe) {
        editor.putInt(KEY_USER_ID, userId);
        editor.putString(KEY_USER_EMAIL, email);
        editor.putString(KEY_USER_NAME, name);
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putBoolean(KEY_REMEMBER_ME, rememberMe);
        editor.putInt(KEY_LOGIN_ATTEMPTS, 0); // Reset attempts on successful login
        editor.putLong(KEY_LOCK_TIMESTAMP, 0); // Clear lock
        editor.apply();
    }

    /**
     * Check if user is logged in
     * @return true if logged in, false otherwise
     */
    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    /**
     * Get current user ID
     * @return User ID or -1 if not logged in
     */
    public int getUserId() {
        return prefs.getInt(KEY_USER_ID, -1);
    }

    /**
     * Get current user email
     * @return User email or null
     */
    public String getUserEmail() {
        return prefs.getString(KEY_USER_EMAIL, null);
    }

    /**
     * Get current user name
     * @return User name or null
     */
    public String getUserName() {
        return prefs.getString(KEY_USER_NAME, null);
    }

    /**
     * Check if Remember Me is enabled
     * @return true if enabled
     */
    public boolean isRememberMeEnabled() {
        return prefs.getBoolean(KEY_REMEMBER_ME, false);
    }

    /**
     * ✅ FIX: Logout user but keep settings (Remember Me, Biometric, Dark Mode)
     */
    public void logout() {
        // Xóa các thông tin phiên đăng nhập
        editor.remove(KEY_USER_ID);
        editor.remove(KEY_USER_NAME);
        editor.remove(KEY_IS_LOGGED_IN);
        editor.remove(KEY_LOGIN_ATTEMPTS);
        editor.remove(KEY_LOCK_TIMESTAMP);

        // Nếu không bật Remember Me thì mới xóa Email
        if (!isRememberMeEnabled()) {
            editor.remove(KEY_USER_EMAIL);
        }

        // QUAN TRỌNG: KHÔNG xóa KEY_BIOMETRIC_ENABLED, KEY_BIOMETRIC_EMAIL, KEY_DARK_MODE
        editor.apply();
    }

    /**
     * Update user name in session (for profile updates)
     * @param name New user name
     */
    public void updateUserName(String name) {
        editor.putString(KEY_USER_NAME, name);
        editor.apply();
    }

    // ============ Dark Mode Management ============

    /**
     * Check if dark mode is enabled
     * @return true if dark mode enabled
     */
    public boolean isDarkModeEnabled() {
        return prefs.getBoolean(KEY_DARK_MODE, false);
    }

    /**
     * Set dark mode preference
     * @param enabled true to enable dark mode
     */
    public void setDarkMode(boolean enabled) {
        editor.putBoolean(KEY_DARK_MODE, enabled);
        editor.apply();
    }

    // ============ Login Attempt Management ============

    /**
     * Increment login attempt counter
     */
    public void incrementLoginAttempts() {
        int attempts = prefs.getInt(KEY_LOGIN_ATTEMPTS, 0);
        editor.putInt(KEY_LOGIN_ATTEMPTS, attempts + 1);

        // If max attempts reached, set lock timestamp
        if (attempts + 1 >= MAX_LOGIN_ATTEMPTS) {
            editor.putLong(KEY_LOCK_TIMESTAMP, System.currentTimeMillis());
        }

        editor.apply();
    }

    /**
     * Get current login attempt count
     * @return Number of failed attempts
     */
    public int getLoginAttempts() {
        return prefs.getInt(KEY_LOGIN_ATTEMPTS, 0);
    }

    /**
     * Reset login attempts counter
     */
    public void resetLoginAttempts() {
        editor.putInt(KEY_LOGIN_ATTEMPTS, 0);
        editor.putLong(KEY_LOCK_TIMESTAMP, 0);
        editor.apply();
    }

    /**
     * Check if account is locked due to failed attempts
     * @return true if locked, false otherwise
     */
    public boolean isAccountLocked() {
        long lockTimestamp = prefs.getLong(KEY_LOCK_TIMESTAMP, 0);

        if (lockTimestamp == 0) {
            return false;
        }

        long currentTime = System.currentTimeMillis();
        long timePassed = currentTime - lockTimestamp;

        // If lock duration passed, unlock account
        if (timePassed >= LOCK_DURATION) {
            resetLoginAttempts();
            return false;
        }

        return true;
    }

    /**
     * Get remaining lock time in seconds
     * @return Remaining seconds or 0 if not locked
     */
    public long getRemainingLockTime() {
        if (!isAccountLocked()) {
            return 0;
        }

        long lockTimestamp = prefs.getLong(KEY_LOCK_TIMESTAMP, 0);
        long currentTime = System.currentTimeMillis();
        long timePassed = currentTime - lockTimestamp;
        long remainingTime = LOCK_DURATION - timePassed;

        return remainingTime / 1000; // Convert to seconds
    }

    // ============ Password Hashing ============

    /**
     * Hash password using SHA-256
     * @param password Plain text password
     * @return Hashed password string
     */
    public static String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes());

            // Convert byte array to hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            // Fallback: return plain password (NOT RECOMMENDED for production)
            return password;
        }
    }

    /**
     * Verify password against hash
     * @param password Plain text password to verify
     * @param hashedPassword Stored hashed password
     * @return true if passwords match
     */
    public static boolean verifyPassword(String password, String hashedPassword) {
        String hashedInput = hashPassword(password);
        return hashedInput.equals(hashedPassword);
    }

    // ============ Biometric Authentication Management ============

    /**
     * Check if biometric authentication is enabled for current user
     * @return true if enabled
     */
    public boolean isBiometricEnabled() {
        return prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false);
    }

    /**
     * Enable biometric authentication for current user
     * @param email User email to associate with biometric
     */
    public void enableBiometric(String email) {
        editor.putBoolean(KEY_BIOMETRIC_ENABLED, true);
        editor.putString(KEY_BIOMETRIC_EMAIL, email);
        editor.apply();
    }

    /**
     * Disable biometric authentication
     */
    public void disableBiometric() {
        editor.putBoolean(KEY_BIOMETRIC_ENABLED, false);
        editor.putString(KEY_BIOMETRIC_EMAIL, null);
        editor.apply();
    }

    /**
     * Get email associated with biometric login
     * @return Email or null
     */
    public String getBiometricEmail() {
        return prefs.getString(KEY_BIOMETRIC_EMAIL, null);
    }

    /**
     * Check if biometric is available for this user
     * Requires: Biometric enabled + Email matches current session
     * @return true if available
     */
    public boolean isBiometricAvailable() {
        if (!isBiometricEnabled()) {
            return false;
        }

        String biometricEmail = getBiometricEmail();
        String currentEmail = getUserEmail();

        // If not logged in, check if saved email matches remembered email
        if (currentEmail == null && isRememberMeEnabled()) {
            currentEmail = getUserEmail();
        }

        return biometricEmail != null && biometricEmail.equals(currentEmail);
    }
}