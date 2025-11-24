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
    private TextView tvForgotPassword;

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
        tvForgotPassword = findViewById(R.id.tv_forgot_password);
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

        tvForgotPassword.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
            startActivity(intent);
        });
    }

    /**
     * ✅ FIX: Setup biometric with localized error messages
     */
    private void setupBiometric() {
        BiometricManager biometricManager = BiometricManager.from(this);

        int canAuthenticate = biometricManager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG |
                        BiometricManager.Authenticators.BIOMETRIC_WEAK
        );

        switch (canAuthenticate) {
            case BiometricManager.BIOMETRIC_SUCCESS:
                setupBiometricUI();
                setupBiometricPrompt();

                if (sessionManager.isBiometricEnabled() && sessionManager.getBiometricEmail() != null) {
                    String savedEmail = sessionManager.getBiometricEmail();
                    etEmail.setText(savedEmail);
                    btnBiometric.setVisibility(View.VISIBLE);
                    btnBiometric.setAlpha(1.0f);
                    switchBiometric.setVisibility(View.VISIBLE);
                    switchBiometric.setChecked(true);

                    if (getIntent().getBooleanExtra("auto_biometric", true)) {
                        btnBiometric.postDelayed(this::showBiometricPrompt, 500);
                    }
                } else {
                    btnBiometric.setVisibility(View.VISIBLE);
                    btnBiometric.setAlpha(0.5f);
                    switchBiometric.setVisibility(View.VISIBLE);
                    switchBiometric.setChecked(false);
                }
                break;

            case BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE:
                btnBiometric.setVisibility(View.GONE);
                switchBiometric.setVisibility(View.GONE);
                break;

            case BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE:
                Toast.makeText(this, getString(R.string.biometric_not_available), Toast.LENGTH_SHORT).show();
                btnBiometric.setVisibility(View.GONE);
                switchBiometric.setVisibility(View.GONE);
                break;

            case BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED:
                btnBiometric.setVisibility(View.VISIBLE);
                btnBiometric.setAlpha(0.5f);
                switchBiometric.setVisibility(View.VISIBLE);
                switchBiometric.setChecked(false);
                switchBiometric.setEnabled(false);

                Toast.makeText(this, getString(R.string.biometric_not_enrolled), Toast.LENGTH_LONG).show();
                break;
        }
    }

    /**
     * ✅ FIX: Setup biometric UI listeners with localized messages
     */
    private void setupBiometricUI() {
        btnBiometric.setOnClickListener(v -> {
            if (sessionManager.isBiometricEnabled()) {
                showBiometricPrompt();
            } else {
                Toast.makeText(this, getString(R.string.msg_biometric_toggle_instruction), Toast.LENGTH_SHORT).show();
            }
        });

        switchBiometric.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!buttonView.isPressed()) return;

            if (isChecked) {
                String email = etEmail.getText().toString().trim();
                if (email.isEmpty()) {
                    Toast.makeText(this, getString(R.string.msg_biometric_enable_first), Toast.LENGTH_SHORT).show();
                    switchBiometric.setChecked(false);
                    return;
                }
                showEnableBiometricDialog(email);
            } else {
                sessionManager.disableBiometric();
                btnBiometric.setAlpha(0.5f);
                Toast.makeText(this, getString(R.string.biometric_disabled), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * ✅ FIX: Setup biometric prompt with localized builder strings
     */
    private void setupBiometricPrompt() {
        java.util.concurrent.Executor executor = ContextCompat.getMainExecutor(this);

        biometricPrompt = new BiometricPrompt(this, executor,
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                        super.onAuthenticationError(errorCode, errString);
                        if (errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                                errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                            return;
                        }
                        Toast.makeText(LoginActivity.this,
                                getString(R.string.msg_biometric_auth_error, errString), Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                        super.onAuthenticationSucceeded(result);
                        String email = sessionManager.getBiometricEmail();
                        if (email != null) {
                            User user = dbHelper.getUserByEmail(email);
                            if (user != null) {
                                handleLoginSuccess(user);
                            } else {
                                Toast.makeText(LoginActivity.this,
                                        getString(R.string.msg_biometric_user_not_found), Toast.LENGTH_LONG).show();
                                sessionManager.disableBiometric();
                                switchBiometric.setChecked(false);
                            }
                        }
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        super.onAuthenticationFailed();
                        Toast.makeText(LoginActivity.this,
                                getString(R.string.msg_biometric_auth_failed), Toast.LENGTH_SHORT).show();
                    }
                });

        // Sử dụng string resource cho PromptInfo
        promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.biometric_prompt_title))
                .setSubtitle(getString(R.string.biometric_prompt_subtitle))
                .setDescription(getString(R.string.biometric_prompt_description))
                .setNegativeButtonText(getString(R.string.biometric_prompt_negative))
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG |
                        BiometricManager.Authenticators.BIOMETRIC_WEAK)
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
     * ✅ FIX: Localized dialog for enabling biometric
     */
    private void showEnableBiometricDialog(String email) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.biometric_enable)); // "Enable Biometric Login"

        // Format message: "Please enter your password... for: email@example.com"
        builder.setMessage(getString(R.string.dialog_biometric_enable_message, email));

        final TextInputEditText input = new TextInputEditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT |
                android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        input.setHint(getString(R.string.auth_password));

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

        builder.setPositiveButton(getString(R.string.biometric_verify), (dialog, which) -> {
            String password = input.getText().toString();

            if (password.isEmpty()) {
                Toast.makeText(this, getString(R.string.err_password_required), Toast.LENGTH_SHORT).show();
                switchBiometric.setChecked(false);
                return;
            }

            User user = dbHelper.getUserByEmail(email);
            if (user == null) {
                Toast.makeText(this, getString(R.string.err_email_not_found), Toast.LENGTH_SHORT).show();
                switchBiometric.setChecked(false);
                return;
            }

            String passwordHash = SessionManager.hashPassword(password);
            if (!passwordHash.equals(user.getPasswordHash())) {
                Toast.makeText(this, getString(R.string.msg_incorrect_password), Toast.LENGTH_SHORT).show();
                switchBiometric.setChecked(false);
                return;
            }

            sessionManager.enableBiometric(email);
            btnBiometric.setAlpha(1.0f);
            Toast.makeText(this, getString(R.string.msg_biometric_success_enabled), Toast.LENGTH_LONG).show();
        });

        builder.setNegativeButton(getString(R.string.action_cancel), (dialog, which) -> {
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
     * ✅ FIX: Login handler with localized errors and status
     */
    private void handleLogin() {
        if (sessionManager.isAccountLocked()) {
            long remainingSeconds = sessionManager.getRemainingLockTime();
            long minutes = remainingSeconds / 60;
            long seconds = remainingSeconds % 60;

            // Format: "Account locked. Try again in 4:59"
            String lockMessage = getString(R.string.msg_account_locked_timer, minutes, seconds);
            Toast.makeText(this, lockMessage, Toast.LENGTH_LONG).show();
            return;
        }

        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();

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

        btnLogin.setEnabled(false);
        btnLogin.setText(getString(R.string.msg_signing_in)); // "Signing In..."

        User user = dbHelper.getUserByEmail(email);

        if (user == null) {
            handleLoginFailure(getString(R.string.error_invalid_credentials));
            return;
        }

        String passwordHash = SessionManager.hashPassword(password);
        if (!passwordHash.equals(user.getPasswordHash())) {
            handleLoginFailure(getString(R.string.error_invalid_credentials));
            return;
        }

        handleLoginSuccess(user);
    }

    private void handleLoginSuccess(User user) {
        sessionManager.resetLoginAttempts();
        boolean rememberMe = cbRememberMe.isChecked();
        sessionManager.createLoginSession(user.getId(), user.getEmail(), user.getName(), rememberMe);
        sessionManager.setDarkMode(user.isDarkModeEnabled());

        Toast.makeText(this, getString(R.string.msg_welcome_user, user.getName()), Toast.LENGTH_SHORT).show();

        navigateToMain();
    }

    private void handleLoginFailure(String message) {
        sessionManager.incrementLoginAttempts();
        int attempts = sessionManager.getLoginAttempts();
        int remainingAttempts = 3 - attempts;

        if (remainingAttempts > 0) {
            // Format: "Invalid email... . 2 attempts remaining."
            String errorMessage = getString(R.string.msg_login_failed_attempts, message, remainingAttempts);
            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, getString(R.string.error_account_locked), Toast.LENGTH_LONG).show();
        }

        btnLogin.setEnabled(true);
        btnLogin.setText(getString(R.string.auth_login)); // Reset button text
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