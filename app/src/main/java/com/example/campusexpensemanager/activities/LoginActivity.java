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

// ✅ NEW: Biometric imports
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.annotation.NonNull;
import java.util.concurrent.Executor;
import com.google.android.material.switchmaterial.SwitchMaterial;


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

    // ✅ NEW: Biometric components
    private ImageButton btnBiometric;
    private SwitchMaterial switchBiometric;
    private BiometricPrompt biometricPrompt;
    private BiometricPrompt.PromptInfo promptInfo;

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

        setupBiometric();
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
        // ✅ NEW: Biometric views
        btnBiometric = findViewById(R.id.btn_biometric);
        switchBiometric = findViewById(R.id.switch_biometric);
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
     * ✅ NEW: Setup biometric authentication
     */
    private void setupBiometric() {
        // Check if device supports biometric
        BiometricManager biometricManager = BiometricManager.from(this);

        int canAuthenticate = biometricManager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG |
                        BiometricManager.Authenticators.BIOMETRIC_WEAK
        );

        switch (canAuthenticate) {
            case BiometricManager.BIOMETRIC_SUCCESS:
                // Device supports biometric
                setupBiometricUI();
                setupBiometricPrompt();

                // Auto-trigger if enabled
                if (sessionManager.isBiometricEnabled() &&
                        sessionManager.getBiometricEmail() != null) {

                    // Pre-fill email
                    String savedEmail = sessionManager.getBiometricEmail();
                    etEmail.setText(savedEmail);

                    // Show biometric icon as active
                    btnBiometric.setVisibility(View.VISIBLE);
                    btnBiometric.setAlpha(1.0f);
                    switchBiometric.setVisibility(View.VISIBLE);
                    switchBiometric.setChecked(true);

                    // Auto-prompt if coming from fresh launch
                    if (getIntent().getBooleanExtra("auto_biometric", true)) {
                        // Delay to let UI settle
                        btnBiometric.postDelayed(this::showBiometricPrompt, 500);
                    }
                } else {
                    // Show biometric option but disabled
                    btnBiometric.setVisibility(View.VISIBLE);
                    btnBiometric.setAlpha(0.5f);
                    switchBiometric.setVisibility(View.VISIBLE);
                    switchBiometric.setChecked(false);
                }
                break;

            case BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE:
                // Device doesn't have biometric hardware
                btnBiometric.setVisibility(View.GONE);
                switchBiometric.setVisibility(View.GONE);
                break;

            case BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE:
                // Biometric hardware unavailable
                Toast.makeText(this, "Biometric hardware unavailable",
                        Toast.LENGTH_SHORT).show();
                btnBiometric.setVisibility(View.GONE);
                switchBiometric.setVisibility(View.GONE);
                break;

            case BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED:
                // User hasn't enrolled biometric
                btnBiometric.setVisibility(View.VISIBLE);
                btnBiometric.setAlpha(0.5f);
                switchBiometric.setVisibility(View.VISIBLE);
                switchBiometric.setChecked(false);
                switchBiometric.setEnabled(false);

                Toast.makeText(this,
                        "Please enroll fingerprint/face in device settings first",
                        Toast.LENGTH_LONG).show();
                break;
        }
    }

    /**
     * ✅ NEW: Setup biometric UI listeners
     */
    private void setupBiometricUI() {
        // Biometric icon click
        btnBiometric.setOnClickListener(v -> {
            if (sessionManager.isBiometricEnabled()) {
                showBiometricPrompt();
            } else {
                Toast.makeText(this,
                        "Enable biometric login first using the switch below",
                        Toast.LENGTH_SHORT).show();
            }
        });

        // Biometric switch
        switchBiometric.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!buttonView.isPressed()) {
                // Programmatic change, ignore
                return;
            }

            if (isChecked) {
                // User wants to enable biometric
                String email = etEmail.getText().toString().trim();

                if (email.isEmpty()) {
                    Toast.makeText(this,
                            "Please enter your email first",
                            Toast.LENGTH_SHORT).show();
                    switchBiometric.setChecked(false);
                    return;
                }

                // Verify email exists and authenticate first
                showEnableBiometricDialog(email);

            } else {
                // Disable biometric
                sessionManager.disableBiometric();
                btnBiometric.setAlpha(0.5f);
                Toast.makeText(this, "Biometric login disabled",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * ✅ NEW: Setup biometric prompt
     */
    private void setupBiometricPrompt() {
        Executor executor = ContextCompat.getMainExecutor(this);

        biometricPrompt = new BiometricPrompt(this, executor,
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                        super.onAuthenticationError(errorCode, errString);

                        if (errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                                errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                            // User canceled, do nothing
                            return;
                        }

                        Toast.makeText(LoginActivity.this,
                                "Authentication error: " + errString,
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                        super.onAuthenticationSucceeded(result);

                        // Biometric success - login with saved email
                        String email = sessionManager.getBiometricEmail();

                        if (email != null) {
                            User user = dbHelper.getUserByEmail(email);

                            if (user != null) {
                                handleLoginSuccess(user);
                            } else {
                                Toast.makeText(LoginActivity.this,
                                        "User not found. Please login with password.",
                                        Toast.LENGTH_LONG).show();
                                sessionManager.disableBiometric();
                                switchBiometric.setChecked(false);
                            }
                        }
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        super.onAuthenticationFailed();
                        Toast.makeText(LoginActivity.this,
                                "Authentication failed. Try again.",
                                Toast.LENGTH_SHORT).show();
                    }
                });

        // Setup prompt info
        promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Biometric Login")
                .setSubtitle("Use your fingerprint or face to login")
                .setDescription("Login to CampusExpense Manager")
                .setNegativeButtonText("Use Password")
                .setAllowedAuthenticators(
                        BiometricManager.Authenticators.BIOMETRIC_STRONG |
                                BiometricManager.Authenticators.BIOMETRIC_WEAK
                )
                .build();
    }

    /**
     * ✅ NEW: Show biometric prompt
     */
    private void showBiometricPrompt() {
        if (biometricPrompt != null && promptInfo != null) {
            biometricPrompt.authenticate(promptInfo);
        }
    }

    /**
     * ✅ NEW: Show dialog to enable biometric with password verification
     */
    private void showEnableBiometricDialog(String email) {
        // Create dialog to verify password
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Enable Biometric Login");
        builder.setMessage("Please enter your password to enable biometric authentication for: " + email);

        // Create input field
        final TextInputEditText input = new TextInputEditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT |
                android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        input.setHint("Password");

        android.widget.FrameLayout container = new android.widget.FrameLayout(this);
        android.widget.FrameLayout.LayoutParams params = new android.widget.FrameLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.leftMargin = 50;
        params.rightMargin = 50;
        input.setLayoutParams(params);
        container.addView(input);

        builder.setView(container);

        builder.setPositiveButton("Verify", (dialog, which) -> {
            String password = input.getText().toString();

            if (password.isEmpty()) {
                Toast.makeText(this, "Password required", Toast.LENGTH_SHORT).show();
                switchBiometric.setChecked(false);
                return;
            }

            // Verify credentials
            User user = dbHelper.getUserByEmail(email);

            if (user == null) {
                Toast.makeText(this, "Email not found", Toast.LENGTH_SHORT).show();
                switchBiometric.setChecked(false);
                return;
            }

            String passwordHash = SessionManager.hashPassword(password);

            if (!passwordHash.equals(user.getPasswordHash())) {
                Toast.makeText(this, "Incorrect password", Toast.LENGTH_SHORT).show();
                switchBiometric.setChecked(false);
                return;
            }

            // Password correct - enable biometric
            sessionManager.enableBiometric(email);
            btnBiometric.setAlpha(1.0f);
            Toast.makeText(this,
                    "✓ Biometric login enabled! You can now use fingerprint to login.",
                    Toast.LENGTH_LONG).show();
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> {
            switchBiometric.setChecked(false);
            dialog.cancel();
        });

        builder.show();
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