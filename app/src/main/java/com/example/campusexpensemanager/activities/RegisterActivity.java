package com.example.campusexpensemanager.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
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
 * RegisterActivity handles new user registration with validation
 * Requires: Email, Password, Confirm Password, Name, Address, Phone
 */
public class RegisterActivity extends AppCompatActivity {

    private TextInputLayout tilEmail, tilPassword, tilConfirmPassword;
    private TextInputLayout tilName, tilAddress, tilPhone;
    private TextInputEditText etEmail, etPassword, etConfirmPassword;
    private TextInputEditText etName, etAddress, etPhone;
    private Button btnRegister;
    private TextView tvLoginLink;

    private DatabaseHelper dbHelper;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Initialize helpers
        dbHelper = DatabaseHelper.getInstance(this);
        sessionManager = new SessionManager(this);

        // Initialize views
        initializeViews();

        // Setup real-time validation
        setupValidation();

        // Setup click listeners
        setupClickListeners();
    }

    private void initializeViews() {
        // TextInputLayouts
        tilEmail = findViewById(R.id.til_email);
        tilPassword = findViewById(R.id.til_password);
        tilConfirmPassword = findViewById(R.id.til_confirm_password);
        tilName = findViewById(R.id.til_name);
        tilAddress = findViewById(R.id.til_address);
        tilPhone = findViewById(R.id.til_phone);

        // EditTexts
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        etName = findViewById(R.id.et_name);
        etAddress = findViewById(R.id.et_address);
        etPhone = findViewById(R.id.et_phone);

        // Buttons
        btnRegister = findViewById(R.id.btn_register);
        tvLoginLink = findViewById(R.id.tv_login_link);
    }

    private void setupValidation() {
        // Email validation
        etEmail.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validateEmail();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Password validation
        etPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validatePassword();
                validateConfirmPassword(); // Re-validate confirm on password change
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Confirm Password validation
        etConfirmPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validateConfirmPassword();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Phone validation
        etPhone.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validatePhone();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Name validation
        etName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validateName();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupClickListeners() {
        btnRegister.setOnClickListener(v -> handleRegister());

        tvLoginLink.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }

    // ============ Validation Methods ============

    private boolean validateEmail() {
        String email = etEmail.getText().toString().trim();

        if (email.isEmpty()) {
            tilEmail.setError(getString(R.string.error_empty_field));
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError(getString(R.string.error_invalid_email));
            return false;
        }

        // Check if email already exists in database
        User existingUser = dbHelper.getUserByEmail(email);
        if (existingUser != null) {
            tilEmail.setError(getString(R.string.error_email_exists));
            return false;
        }

        tilEmail.setError(null);
        return true;
    }

    private boolean validatePassword() {
        String password = etPassword.getText().toString();

        if (password.isEmpty()) {
            tilPassword.setError(getString(R.string.error_empty_field));
            return false;
        }

        if (password.length() < 8) {
            tilPassword.setError(getString(R.string.error_password_short));
            return false;
        }

        tilPassword.setError(null);
        return true;
    }

    private boolean validateConfirmPassword() {
        String password = etPassword.getText().toString();
        String confirmPassword = etConfirmPassword.getText().toString();

        if (confirmPassword.isEmpty()) {
            tilConfirmPassword.setError(getString(R.string.error_empty_field));
            return false;
        }

        if (!confirmPassword.equals(password)) {
            tilConfirmPassword.setError(getString(R.string.error_password_mismatch));
            return false;
        }

        tilConfirmPassword.setError(null);
        tilConfirmPassword.setEndIconDrawable(R.drawable.ic_check); // Show checkmark
        return true;
    }

    private boolean validateName() {
        String name = etName.getText().toString().trim();

        if (name.isEmpty()) {
            tilName.setError(getString(R.string.error_empty_field));
            return false;
        }

        tilName.setError(null);
        return true;
    }

    private boolean validatePhone() {
        String phone = etPhone.getText().toString().trim();

        if (phone.isEmpty()) {
            tilPhone.setError(getString(R.string.error_empty_field));
            return false;
        }

        // Vietnam phone format: 10 digits starting with 0
        if (!phone.matches("^0\\d{9}$")) {
            tilPhone.setError(getString(R.string.error_invalid_phone));
            return false;
        }

        tilPhone.setError(null);
        return true;
    }

    private boolean validateAddress() {
        String address = etAddress.getText().toString().trim();

        if (address.isEmpty()) {
            tilAddress.setError(getString(R.string.error_empty_field));
            return false;
        }

        tilAddress.setError(null);
        return true;
    }

    // ============ Registration Handler ============

    private void handleRegister() {
        // Validate all fields
        boolean isEmailValid = validateEmail();
        boolean isPasswordValid = validatePassword();
        boolean isConfirmPasswordValid = validateConfirmPassword();
        boolean isNameValid = validateName();
        boolean isAddressValid = validateAddress();
        boolean isPhoneValid = validatePhone();

        if (!isEmailValid || !isPasswordValid || !isConfirmPasswordValid ||
                !isNameValid || !isAddressValid || !isPhoneValid) {
            Toast.makeText(this, "Please fix all errors", Toast.LENGTH_SHORT).show();
            return;
        }

        // Disable button to prevent double submission
        btnRegister.setEnabled(false);
        btnRegister.setText("Creating Account...");

        // Get form data
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();
        String name = etName.getText().toString().trim();
        String address = etAddress.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();

        // Hash password
        String passwordHash = SessionManager.hashPassword(password);

        // Create user object
        User newUser = new User(email, passwordHash, name, address, phone);

        // Insert into database
        long userId = dbHelper.insertUser(newUser);

        if (userId != -1) {
            Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show();

            // Navigate to login
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            intent.putExtra("email", email); // Pre-fill email on login screen
            startActivity(intent);
            finish();
        } else {
            Toast.makeText(this, "Registration failed. Please try again.", Toast.LENGTH_SHORT).show();
            btnRegister.setEnabled(true);
            btnRegister.setText(getString(R.string.auth_register));
        }
    }
}