package com.example.campusexpensemanager.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.example.campusexpensemanager.R;
import com.example.campusexpensemanager.models.User;
import com.example.campusexpensemanager.utils.DatabaseHelper;
import com.example.campusexpensemanager.utils.SessionManager;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/**
 * ProfileActivity manages user profile viewing and editing
 * Features: Edit profile, Change password, Dark mode toggle, Logout
 */
public class ProfileActivity extends AppCompatActivity {

    private TextInputLayout tilName, tilEmail, tilAddress, tilPhone;
    private TextInputLayout tilOldPassword, tilNewPassword, tilConfirmNewPassword;
    private TextInputEditText etName, etEmail, etAddress, etPhone;
    private TextInputEditText etOldPassword, etNewPassword, etConfirmNewPassword;
    private SwitchMaterial switchDarkMode;
    private Button btnEditMode, btnSaveProfile, btnChangePassword, btnLogout;

    private DatabaseHelper dbHelper;
    private SessionManager sessionManager;
    private User currentUser;

    private boolean isEditMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize helpers first
        dbHelper = DatabaseHelper.getInstance(this);
        sessionManager = new SessionManager(this);

        // Check authentication BEFORE setContentView
        if (!sessionManager.isLoggedIn()) {
            navigateToLogin();
            return;
        }

        // Set content view
        setContentView(R.layout.activity_profile);

        // Initialize views
        initializeViews();

        // Load current user (with error handling)
        try {
            loadCurrentUser();

            // Only proceed if user loaded successfully
            if (currentUser != null) {
                // Display user info FIRST (without calling setEditMode)
                populateFields();

                // Setup dark mode
                setupDarkMode();

                // Setup click listeners
                setupClickListeners();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error loading profile: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
            finish();
        }

        // Setup back pressed callback
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (isEditMode) {
                    // Cancel edit mode instead of going back
                    cancelEdit();
                } else {
                    // Allow default back behavior
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    private void initializeViews() {
        // Profile fields
        tilName = findViewById(R.id.til_profile_name);
        tilEmail = findViewById(R.id.til_profile_email);
        tilAddress = findViewById(R.id.til_profile_address);
        tilPhone = findViewById(R.id.til_profile_phone);

        etName = findViewById(R.id.et_profile_name);
        etEmail = findViewById(R.id.et_profile_email);
        etAddress = findViewById(R.id.et_profile_address);
        etPhone = findViewById(R.id.et_profile_phone);

        // Password change fields
        tilOldPassword = findViewById(R.id.til_old_password);
        tilNewPassword = findViewById(R.id.til_new_password);
        tilConfirmNewPassword = findViewById(R.id.til_confirm_new_password);

        etOldPassword = findViewById(R.id.et_old_password);
        etNewPassword = findViewById(R.id.et_new_password);
        etConfirmNewPassword = findViewById(R.id.et_confirm_new_password);

        // Controls
        switchDarkMode = findViewById(R.id.switch_dark_mode);
        btnEditMode = findViewById(R.id.btn_edit_mode);
        btnSaveProfile = findViewById(R.id.btn_save_profile);
        btnChangePassword = findViewById(R.id.btn_change_password);
        btnLogout = findViewById(R.id.btn_logout);
    }

    private void loadCurrentUser() {
        try {
            int userId = sessionManager.getUserId();
            currentUser = dbHelper.getUserById(userId);

            if (currentUser == null) {
                Toast.makeText(this, "Error loading profile", Toast.LENGTH_SHORT).show();
                navigateToLogin();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Database error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            navigateToLogin();
        }
    }

    /**
     * Populate fields WITHOUT calling setEditMode (avoid recursion)
     */
    private void populateFields() {
        if (currentUser == null) {
            return;
        }

        // Set values
        etName.setText(currentUser.getName() != null ? currentUser.getName() : "");
        etEmail.setText(currentUser.getEmail() != null ? currentUser.getEmail() : "");
        etAddress.setText(currentUser.getAddress() != null ? currentUser.getAddress() : "");
        etPhone.setText(currentUser.getPhone() != null ? currentUser.getPhone() : "");

        // Email is always disabled
        etEmail.setEnabled(false);

        // Set initial state to VIEW mode (all disabled except email)
        etName.setEnabled(false);
        etAddress.setEnabled(false);
        etPhone.setEnabled(false);

        btnEditMode.setText(getString(R.string.profile_edit));
        btnSaveProfile.setVisibility(View.GONE);
    }

    private void setupDarkMode() {
        // Set switch state from session
        boolean isDarkMode = sessionManager.isDarkModeEnabled();
        switchDarkMode.setChecked(isDarkMode);

        // Apply dark mode
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    private void setupClickListeners() {
        // Edit mode toggle
        btnEditMode.setOnClickListener(v -> {
            if (isEditMode) {
                cancelEdit();
            } else {
                enterEditMode();
            }
        });

        // Save profile
        btnSaveProfile.setOnClickListener(v -> saveProfile());

        // Change password
        btnChangePassword.setOnClickListener(v -> changePassword());

        // Dark mode toggle
        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonView.isPressed()) { // Only respond to user interaction
                toggleDarkMode(isChecked);
            }
        });

        // Logout
        btnLogout.setOnClickListener(v -> showLogoutDialog());
    }

    /**
     * Enter edit mode - enable fields
     */
    private void enterEditMode() {
        isEditMode = true;

        etName.setEnabled(true);
        etAddress.setEnabled(true);
        etPhone.setEnabled(true);

        btnEditMode.setText("Cancel");
        btnSaveProfile.setVisibility(View.VISIBLE);
    }

    /**
     * Cancel edit mode - restore original values
     */
    private void cancelEdit() {
        isEditMode = false;

        // Restore original values
        if (currentUser != null) {
            etName.setText(currentUser.getName() != null ? currentUser.getName() : "");
            etAddress.setText(currentUser.getAddress() != null ? currentUser.getAddress() : "");
            etPhone.setText(currentUser.getPhone() != null ? currentUser.getPhone() : "");
        }

        // Disable fields
        etName.setEnabled(false);
        etAddress.setEnabled(false);
        etPhone.setEnabled(false);

        btnEditMode.setText(getString(R.string.profile_edit));
        btnSaveProfile.setVisibility(View.GONE);
    }

    /**
     * Save profile changes
     */
    private void saveProfile() {
        // Validate inputs
        String name = etName.getText().toString().trim();
        String address = etAddress.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();

        if (name.isEmpty()) {
            tilName.setError(getString(R.string.error_empty_field));
            return;
        }

        if (address.isEmpty()) {
            tilAddress.setError(getString(R.string.error_empty_field));
            return;
        }

        if (phone.isEmpty()) {
            tilPhone.setError(getString(R.string.error_empty_field));
            return;
        }

        // Validate phone format
        if (!phone.matches("^0\\d{9}$")) {
            tilPhone.setError(getString(R.string.error_invalid_phone));
            return;
        }

        // Clear errors
        tilName.setError(null);
        tilAddress.setError(null);
        tilPhone.setError(null);

        // Update user object
        currentUser.setName(name);
        currentUser.setAddress(address);
        currentUser.setPhone(phone);

        // Save to database
        int rowsAffected = dbHelper.updateUser(currentUser);

        if (rowsAffected > 0) {
            // Update session
            sessionManager.updateUserName(name);

            Toast.makeText(this, getString(R.string.profile_updated), Toast.LENGTH_SHORT).show();

            // Exit edit mode
            cancelEdit();
        } else {
            Toast.makeText(this, "Failed to update profile", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Change password with validation
     */
    private void changePassword() {
        String oldPassword = etOldPassword.getText().toString();
        String newPassword = etNewPassword.getText().toString();
        String confirmNewPassword = etConfirmNewPassword.getText().toString();

        // Validate old password
        if (oldPassword.isEmpty()) {
            tilOldPassword.setError(getString(R.string.error_empty_field));
            return;
        }

        String oldPasswordHash = SessionManager.hashPassword(oldPassword);
        if (!oldPasswordHash.equals(currentUser.getPasswordHash())) {
            tilOldPassword.setError("Incorrect password");
            return;
        }

        // Validate new password
        if (newPassword.isEmpty()) {
            tilNewPassword.setError(getString(R.string.error_empty_field));
            return;
        }

        if (newPassword.length() < 8) {
            tilNewPassword.setError(getString(R.string.error_password_short));
            return;
        }

        // Validate confirm password
        if (!confirmNewPassword.equals(newPassword)) {
            tilConfirmNewPassword.setError(getString(R.string.error_password_mismatch));
            return;
        }

        // Clear errors
        tilOldPassword.setError(null);
        tilNewPassword.setError(null);
        tilConfirmNewPassword.setError(null);

        // Update password
        String newPasswordHash = SessionManager.hashPassword(newPassword);
        currentUser.setPasswordHash(newPasswordHash);

        int rowsAffected = dbHelper.updateUser(currentUser);

        if (rowsAffected > 0) {
            Toast.makeText(this, getString(R.string.profile_password_changed), Toast.LENGTH_SHORT).show();

            // Clear password fields
            etOldPassword.setText("");
            etNewPassword.setText("");
            etConfirmNewPassword.setText("");
        } else {
            Toast.makeText(this, "Failed to change password", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Toggle dark mode with smooth transition
     */
    private void toggleDarkMode(boolean enabled) {
        try {
            // Save preference
            sessionManager.setDarkMode(enabled);

            // Update database
            if (currentUser != null) {
                currentUser.setDarkModeEnabled(enabled);
                dbHelper.updateUser(currentUser);
            }

            // Apply dark mode with animation
            if (enabled) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }

            // Recreate activity with fade animation
            recreate();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error toggling dark mode: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Show logout confirmation dialog
     */
    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.confirm_logout_title))
                .setMessage(getString(R.string.confirm_logout_message))
                .setPositiveButton(getString(R.string.action_yes), (dialog, which) -> logout())
                .setNegativeButton(getString(R.string.action_no), null)
                .show();
    }

    /**
     * Logout user and navigate to login
     */
    private void logout() {
        sessionManager.logout();
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
        navigateToLogin();
    }

    /**
     * Navigate to LoginActivity
     */
    private void navigateToLogin() {
        Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}