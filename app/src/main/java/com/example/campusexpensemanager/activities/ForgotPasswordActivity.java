package com.example.campusexpensemanager.activities;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.campusexpensemanager.R;
import com.example.campusexpensemanager.models.User;
import com.example.campusexpensemanager.utils.DatabaseHelper;
import com.example.campusexpensemanager.utils.SessionManager;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;

/**
 * ForgotPasswordActivity - Firebase OTP-based password reset
 * Steps: Email → OTP → New Password
 */
public class ForgotPasswordActivity extends BaseActivity {

    // UI Components - Step 1: Email
    private MaterialCardView cardEmailStep;
    private TextInputLayout tilEmail;
    private TextInputEditText etEmail;
    private Button btnSendOtp;

    // UI Components - Step 2: OTP
    private MaterialCardView cardOtpStep;
    private TextView tvOtpSent;
    private TextInputLayout tilOtp;
    private TextInputEditText etOtp;
    private Button btnVerifyOtp;
    private TextView tvResendOtp;

    // UI Components - Step 3: Password
    private MaterialCardView cardPasswordStep;
    private TextInputLayout tilNewPassword, tilConfirmPassword;
    private TextInputEditText etNewPassword, etConfirmPassword;
    private Button btnResetPassword;

    private ImageButton btnBack;

    // Firebase
    private FirebaseAuth mAuth;
    private String verificationId;
    private PhoneAuthProvider.ForceResendingToken resendToken;

    // Database
    private DatabaseHelper dbHelper;
    private User currentUser;

    // Timer
    private CountDownTimer resendTimer;
    private boolean canResend = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        dbHelper = DatabaseHelper.getInstance(this);

        initializeViews();
        setupClickListeners();
    }

    private void initializeViews() {
        // Step 1: Email
        cardEmailStep = findViewById(R.id.card_email_step);
        tilEmail = findViewById(R.id.til_email);
        etEmail = findViewById(R.id.et_email);
        btnSendOtp = findViewById(R.id.btn_send_otp);

        // Step 2: OTP
        cardOtpStep = findViewById(R.id.card_otp_step);
        tvOtpSent = findViewById(R.id.tv_otp_sent);
        tilOtp = findViewById(R.id.til_otp);
        etOtp = findViewById(R.id.et_otp);
        btnVerifyOtp = findViewById(R.id.btn_verify_otp);
        tvResendOtp = findViewById(R.id.tv_resend_otp);

        // Step 3: Password
        cardPasswordStep = findViewById(R.id.card_password_step);
        tilNewPassword = findViewById(R.id.til_new_password);
        tilConfirmPassword = findViewById(R.id.til_confirm_password);
        etNewPassword = findViewById(R.id.et_new_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        btnResetPassword = findViewById(R.id.btn_reset_password);

        btnBack = findViewById(R.id.btn_back);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnSendOtp.setOnClickListener(v -> handleSendOtp());
        btnVerifyOtp.setOnClickListener(v -> handleVerifyOtp());
        btnResetPassword.setOnClickListener(v -> handleResetPassword());
        tvResendOtp.setOnClickListener(v -> handleResendOtp());
    }

    // ============ STEP 1: Send OTP ============

    private void handleSendOtp() {
        String email = etEmail.getText().toString().trim();

        // Validate email
        if (email.isEmpty()) {
            tilEmail.setError(getString(R.string.error_empty_field));
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError(getString(R.string.error_invalid_email));
            return;
        }

        tilEmail.setError(null);

        // Check if email exists in database
        currentUser = dbHelper.getUserByEmail(email);
        if (currentUser == null) {
            tilEmail.setError(getString(R.string.forgot_password_email_not_found));
            return;
        }

        // Get phone number (required for Firebase Phone Auth)
        String phoneNumber = currentUser.getPhone();
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            Toast.makeText(this, "No phone number registered for this account", Toast.LENGTH_LONG).show();
            return;
        }

        // Convert phone to international format (+84...)
        String internationalPhone = convertToInternationalFormat(phoneNumber);

        btnSendOtp.setEnabled(false);
        btnSendOtp.setText(getString(R.string.loading));

        // Send OTP via Firebase Phone Auth
        sendOtpToPhone(internationalPhone);
    }

    private String convertToInternationalFormat(String phone) {
        // Convert 0xxxxxxxxx to +84xxxxxxxxx
        if (phone.startsWith("0")) {
            return "+84" + phone.substring(1);
        }
        return phone;
    }

    private void sendOtpToPhone(String phoneNumber) {
        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(mAuth)
                .setPhoneNumber(phoneNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                        // Auto-verification (instant or SMS retrieval)
                        String code = credential.getSmsCode();
                        if (code != null) {
                            etOtp.setText(code);
                            verifyOtpCode(credential);
                        }
                    }

                    @Override
                    public void onVerificationFailed(@NonNull FirebaseException e) {
                        Toast.makeText(ForgotPasswordActivity.this,
                                "Verification failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        btnSendOtp.setEnabled(true);
                        btnSendOtp.setText(getString(R.string.forgot_password_send_otp));
                    }

                    @Override
                    public void onCodeSent(@NonNull String vId,
                                           @NonNull PhoneAuthProvider.ForceResendingToken token) {
                        verificationId = vId;
                        resendToken = token;

                        // Move to Step 2: OTP Input
                        showOtpStep();
                        startResendTimer();

                        Toast.makeText(ForgotPasswordActivity.this,
                                getString(R.string.forgot_password_otp_sent), Toast.LENGTH_SHORT).show();
                    }
                })
                .build();

        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private void showOtpStep() {
        cardEmailStep.setVisibility(View.GONE);
        cardOtpStep.setVisibility(View.VISIBLE);
        cardPasswordStep.setVisibility(View.GONE);

        tvOtpSent.setText(String.format("OTP sent to %s", currentUser.getPhone()));
    }

    private void startResendTimer() {
        canResend = false;
        tvResendOtp.setEnabled(false);
        tvResendOtp.setAlpha(0.5f);

        resendTimer = new CountDownTimer(60000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                tvResendOtp.setText(String.format("Resend in %ds", millisUntilFinished / 1000));
            }

            @Override
            public void onFinish() {
                canResend = true;
                tvResendOtp.setEnabled(true);
                tvResendOtp.setAlpha(1.0f);
                tvResendOtp.setText(getString(R.string.forgot_password_resend_otp));
            }
        }.start();
    }

    // ============ STEP 2: Verify OTP ============

    private void handleVerifyOtp() {
        String code = etOtp.getText().toString().trim();

        if (code.isEmpty() || code.length() != 6) {
            tilOtp.setError("Please enter 6-digit OTP");
            return;
        }

        tilOtp.setError(null);
        btnVerifyOtp.setEnabled(false);
        btnVerifyOtp.setText(getString(R.string.loading));

        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);
        verifyOtpCode(credential);
    }

    private void verifyOtpCode(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // OTP verified successfully
                        showPasswordStep();
                        Toast.makeText(this, "OTP verified!", Toast.LENGTH_SHORT).show();
                    } else {
                        // Verification failed
                        tilOtp.setError(getString(R.string.forgot_password_invalid_otp));
                        btnVerifyOtp.setEnabled(true);
                        btnVerifyOtp.setText(getString(R.string.forgot_password_verify_otp));
                    }
                });
    }

    private void showPasswordStep() {
        cardEmailStep.setVisibility(View.GONE);
        cardOtpStep.setVisibility(View.GONE);
        cardPasswordStep.setVisibility(View.VISIBLE);
    }

    private void handleResendOtp() {
        if (!canResend) {
            return;
        }

        String phoneNumber = convertToInternationalFormat(currentUser.getPhone());

        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(mAuth)
                .setPhoneNumber(phoneNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                        String code = credential.getSmsCode();
                        if (code != null) {
                            etOtp.setText(code);
                        }
                    }

                    @Override
                    public void onVerificationFailed(@NonNull FirebaseException e) {
                        Toast.makeText(ForgotPasswordActivity.this,
                                "Failed to resend: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onCodeSent(@NonNull String vId,
                                           @NonNull PhoneAuthProvider.ForceResendingToken token) {
                        verificationId = vId;
                        resendToken = token;
                        startResendTimer();
                        Toast.makeText(ForgotPasswordActivity.this,
                                "OTP resent successfully", Toast.LENGTH_SHORT).show();
                    }
                })
                .setForceResendingToken(resendToken)
                .build();

        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    // ============ STEP 3: Reset Password ============

    private void handleResetPassword() {
        String newPassword = etNewPassword.getText().toString();
        String confirmPassword = etConfirmPassword.getText().toString();

        // Validation
        if (newPassword.isEmpty()) {
            tilNewPassword.setError(getString(R.string.error_empty_field));
            return;
        }

        if (newPassword.length() < 8) {
            tilNewPassword.setError(getString(R.string.error_password_short));
            return;
        }

        if (!confirmPassword.equals(newPassword)) {
            tilConfirmPassword.setError(getString(R.string.error_password_mismatch));
            return;
        }

        tilNewPassword.setError(null);
        tilConfirmPassword.setError(null);

        // Hash new password
        String newPasswordHash = SessionManager.hashPassword(newPassword);
        currentUser.setPasswordHash(newPasswordHash);

        // Update in database
        int rowsAffected = dbHelper.updateUser(currentUser);

        if (rowsAffected > 0) {
            Toast.makeText(this, getString(R.string.forgot_password_success),
                    Toast.LENGTH_LONG).show();

            // Sign out from Firebase
            mAuth.signOut();

            // Return to login
            finish();
        } else {
            Toast.makeText(this, "Failed to reset password", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (resendTimer != null) {
            resendTimer.cancel();
        }
    }
}