package com.example.campusexpensemanager.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.campusexpensemanager.R;
import com.example.campusexpensemanager.models.User;
import com.example.campusexpensemanager.utils.DatabaseHelper;
import com.example.campusexpensemanager.utils.SessionManager;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/**
 * LoginActivity handles user authentication
 * Features: Password visibility toggle, Remember Me, Account lockout after 3 failed attempts
 */
public class LoginActivity extends BaseActivity {

    private TextInputLayout tilEmail, tilPassword;
    private TextInputEditText etEmail, etPassword;
    private ImageButton btnTogglePassword;
    private CheckBox cbRememberMe;
    private Button btnLogin;
    private TextView tvRegisterLink;

    private DatabaseHelper dbHelper;
    private SessionManager sessionManager;

    private boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize helpers
        dbHelper = DatabaseHelper.getInstance(this);
        sessionManager = new SessionManager(this);

        // Check if already logged in
        if (sessionManager.isLoggedIn()) {
            navigateToMain();
            return;
        }

        // Initialize views
        initializeViews();

        // Pre-fill email if coming from registration
        String prefilledEmail = getIntent().getStringExtra("email");
        if (prefilledEmail != null) {
            etEmail.setText(prefilledEmail);
        }

        // Restore Remember Me state
        if (sessionManager.isRememberMeEnabled()) {
            cbRememberMe.setChecked(true);
            String savedEmail = sessionManager.getUserEmail();
            if (savedEmail != null) {
                etEmail.setText(savedEmail);
            }
        }

        // Setup click listeners
        setupClickListeners();
    }

    private void initializeViews() {
        tilEmail = findViewById(R.id.til_login_email);
        tilPassword = findViewById(R.id.til_login_password);
        etEmail = findViewById(R.id.et_login_email);
        etPassword = findViewById(R.id.et_login_password);
        btnTogglePassword = findViewById(R.id.btn_toggle_password);
        cbRememberMe = findViewById(R.id.cb_remember_me);
        btnLogin = findViewById(R.id.btn_login);
        tvRegisterLink = findViewById(R.id.tv_register_link);
    }

    private void setupClickListeners() {
        // Login button
        btnLogin.setOnClickListener(v -> handleLogin());

        // Password visibility toggle
        btnTogglePassword.setOnClickListener(v -> togglePasswordVisibility());

        // Register link
        tvRegisterLink.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    /**
     * Toggle password visibility with eye icon animation
     */
    private void togglePasswordVisibility() {
        if (isPasswordVisible) {
            // Hide password
            etPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
            btnTogglePassword.setImageResource(R.drawable.ic_eye);
            isPasswordVisible = false;
        } else {
            // Show password
            etPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            btnTogglePassword.setImageResource(R.drawable.ic_eye_off);
            isPasswordVisible = true;
        }

        // Move cursor to end
        etPassword.setSelection(etPassword.getText().length());

        // Animate icon (rotate 180 degrees)
        btnTogglePassword.animate()
                .rotationBy(180f)
                .setDuration(200)
                .start();
    }

    /**
     * Handle login authentication
     */
    private void handleLogin() {
        // Check if account is locked
        if (sessionManager.isAccountLocked()) {
            long remainingSeconds = sessionManager.getRemainingLockTime();
            long minutes = remainingSeconds / 60;
            long seconds = remainingSeconds % 60;

            String lockMessage = String.format("Account locked. Try again in %d:%02d", minutes, seconds);
            Toast.makeText(this, lockMessage, Toast.LENGTH_LONG).show();
            return;
        }

        // Get input values
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();

        // Validate inputs
        if (email.isEmpty()) {
            tilEmail.setError(getString(R.string.error_empty_field));
            return;
        }

        if (password.isEmpty()) {
            tilPassword.setError(getString(R.string.error_empty_field));
            return;
        }

        tilEmail.setError(null);
        tilPassword.setError(null);

        // Disable button during authentication
        btnLogin.setEnabled(false);
        btnLogin.setText("Signing In...");

        // Query user from database
        User user = dbHelper.getUserByEmail(email);

        if (user == null) {
            handleLoginFailure("Invalid email or password");
            return;
        }

        // Verify password
        String passwordHash = SessionManager.hashPassword(password);

        if (!passwordHash.equals(user.getPasswordHash())) {
            handleLoginFailure("Invalid email or password");
            return;
        }

        // Login successful
        handleLoginSuccess(user);
    }

    /**
     * Handle successful login
     */
    private void handleLoginSuccess(User user) {
        // Reset login attempts
        sessionManager.resetLoginAttempts();

        // Create session
        boolean rememberMe = cbRememberMe.isChecked();
        sessionManager.createLoginSession(user.getId(), user.getEmail(), user.getName(), rememberMe);

        // Update dark mode preference from user's DB setting
        sessionManager.setDarkMode(user.isDarkModeEnabled());

        Toast.makeText(this, "Welcome, " + user.getName() + "!", Toast.LENGTH_SHORT).show();

        // Navigate to main activity
        navigateToMain();
    }

    /**
     * Handle failed login attempt
     */
    private void handleLoginFailure(String message) {
        // Increment failed attempts
        sessionManager.incrementLoginAttempts();

        int attempts = sessionManager.getLoginAttempts();
        int remainingAttempts = 3 - attempts;

        if (remainingAttempts > 0) {
            String errorMessage = message + ". " + remainingAttempts + " attempts remaining.";
            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, getString(R.string.error_account_locked), Toast.LENGTH_LONG).show();
        }

        // Re-enable button
        btnLogin.setEnabled(true);
        btnLogin.setText(getString(R.string.auth_login));

        // Clear password field
        etPassword.setText("");
    }

    /**
     * Navigate to MainActivity
     */
    private void navigateToMain() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}