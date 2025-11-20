package com.example.campusexpensemanager.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.campusexpensemanager.R;
import com.example.campusexpensemanager.models.User;
import com.example.campusexpensemanager.utils.DatabaseHelper;
import com.example.campusexpensemanager.utils.LocaleHelper;
import com.example.campusexpensemanager.utils.SessionManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.File;
import java.io.FileOutputStream;

/**
 * ProfileActivity - Fully Localized
 * Updated to use resources (getString) instead of hardcoded text.
 */
public class ProfileActivity extends BaseActivity {

    private static final int CAMERA_PERMISSION_CODE = 200;

    private ImageView ivAvatar;
    private FloatingActionButton fabChangeAvatar;
    private TextInputLayout tilName, tilEmail, tilAddress, tilPhone;
    private TextInputLayout tilOldPassword, tilNewPassword, tilConfirmNewPassword;
    private TextInputEditText etName, etEmail, etAddress, etPhone;
    private TextInputEditText etOldPassword, etNewPassword, etConfirmNewPassword;
    private SwitchMaterial switchDarkMode, switchBiometric;
    private Spinner spinnerLanguage;
    private Button btnEditMode, btnSaveProfile, btnChangePassword, btnLogout;

    private DatabaseHelper dbHelper;
    private SessionManager sessionManager;
    private User currentUser;

    private boolean isEditMode = false;
    private ActivityResultLauncher<Intent> avatarLauncher;
    private ActivityResultLauncher<Intent> galleryLauncher;

    private Button btnSendFeedback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        dbHelper = DatabaseHelper.getInstance(this);
        sessionManager = new SessionManager(this);

        if (!sessionManager.isLoggedIn()) {
            navigateToLogin();
            return;
        }

        setContentView(R.layout.activity_profile);

        initializeViews();
        setupAvatarLauncher();

        try {
            loadCurrentUser();
            if (currentUser != null) {
                populateFields();
                setupDarkMode();
                setupLanguageSelector();
                setupClickListeners();
            }
        } catch (Exception e) {
            e.printStackTrace();
            // FIX: Use resource string with parameter
            Toast.makeText(this, getString(R.string.msg_error_loading_profile, e.getMessage()),
                    Toast.LENGTH_SHORT).show();
            finish();
        }

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (isEditMode) {
                    cancelEdit();
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    private void initializeViews() {
        ivAvatar = findViewById(R.id.iv_avatar);
        fabChangeAvatar = findViewById(R.id.fab_change_avatar);

        tilName = findViewById(R.id.til_profile_name);
        tilEmail = findViewById(R.id.til_profile_email);
        tilAddress = findViewById(R.id.til_profile_address);
        tilPhone = findViewById(R.id.til_profile_phone);

        etName = findViewById(R.id.et_profile_name);
        etEmail = findViewById(R.id.et_profile_email);
        etAddress = findViewById(R.id.et_profile_address);
        etPhone = findViewById(R.id.et_profile_phone);

        tilOldPassword = findViewById(R.id.til_old_password);
        tilNewPassword = findViewById(R.id.til_new_password);
        tilConfirmNewPassword = findViewById(R.id.til_confirm_new_password);

        etOldPassword = findViewById(R.id.et_old_password);
        etNewPassword = findViewById(R.id.et_new_password);
        etConfirmNewPassword = findViewById(R.id.et_confirm_new_password);

        switchDarkMode = findViewById(R.id.switch_dark_mode);
        spinnerLanguage = findViewById(R.id.spinner_language);
        btnEditMode = findViewById(R.id.btn_edit_mode);
        btnSaveProfile = findViewById(R.id.btn_save_profile);
        btnChangePassword = findViewById(R.id.btn_change_password);
        btnLogout = findViewById(R.id.btn_logout);

        btnSendFeedback = findViewById(R.id.btn_send_feedback);

        switchBiometric = findViewById(R.id.switch_profile_biometric);
    }

    private void setupAvatarLauncher() {
        avatarLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        try {
                            Bundle extras = result.getData().getExtras();
                            Bitmap imageBitmap = (Bitmap) extras.get("data");

                            // Scale and save avatar
                            Bitmap scaledBitmap = scaleAvatarImage(imageBitmap);
                            String avatarPath = saveAvatar(scaledBitmap);
                            if (avatarPath != null) {
                                currentUser.setAvatarPath(avatarPath);
                                dbHelper.updateUser(currentUser);
                                ivAvatar.setImageBitmap(scaledBitmap);
                                // FIX: Localized Toast
                                Toast.makeText(this, getString(R.string.msg_avatar_updated), Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            // FIX: Localized Toast
                            Toast.makeText(this, getString(R.string.msg_failed_update_avatar), Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );

        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        try {
                            Uri selectedImageUri = result.getData().getData();
                            Bitmap imageBitmap = MediaStore.Images.Media.getBitmap(
                                    getContentResolver(), selectedImageUri);

                            // Scale and save avatar
                            Bitmap scaledBitmap = scaleAvatarImage(imageBitmap);
                            String avatarPath = saveAvatar(scaledBitmap);
                            if (avatarPath != null) {
                                currentUser.setAvatarPath(avatarPath);
                                dbHelper.updateUser(currentUser);
                                ivAvatar.setImageBitmap(scaledBitmap);
                                // FIX: Localized Toast
                                Toast.makeText(this, getString(R.string.msg_avatar_updated), Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            // FIX: Localized Toast
                            Toast.makeText(this, getString(R.string.msg_failed_update_avatar), Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );
    }

    /**
     * Scale image to fit avatar circle (96dp = 96 * density pixels)
     */
    private Bitmap scaleAvatarImage(Bitmap original) {
        int targetSize = (int) (96 * getResources().getDisplayMetrics().density);
        return Bitmap.createScaledBitmap(original, targetSize, targetSize, true);
    }

    private void loadCurrentUser() {
        try {
            int userId = sessionManager.getUserId();
            currentUser = dbHelper.getUserById(userId);

            if (currentUser == null) {
                Toast.makeText(this, getString(R.string.msg_error_loading_profile, "User null"), Toast.LENGTH_SHORT).show();
                navigateToLogin();
            } else {
                loadAvatar();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, getString(R.string.msg_error_loading_profile, e.getMessage()), Toast.LENGTH_SHORT).show();
            navigateToLogin();
        }
    }

    private void loadAvatar() {
        if (currentUser.getAvatarPath() != null && !currentUser.getAvatarPath().isEmpty()) {
            try {
                File avatarFile = new File(currentUser.getAvatarPath());
                if (avatarFile.exists()) {
                    Uri avatarUri = Uri.fromFile(avatarFile);
                    ivAvatar.setImageURI(avatarUri);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private String saveAvatar(Bitmap bitmap) {
        try {
            File avatarDir = new File(getFilesDir(), "avatars");
            if (!avatarDir.exists()) {
                avatarDir.mkdirs();
            }

            String fileName = "avatar_" + currentUser.getId() + ".jpg";
            File avatarFile = new File(avatarDir, fileName);

            FileOutputStream fos = new FileOutputStream(avatarFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.close();

            return avatarFile.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void populateFields() {
        if (currentUser == null) {
            return;
        }

        etName.setText(currentUser.getName() != null ? currentUser.getName() : "");
        etEmail.setText(currentUser.getEmail() != null ? currentUser.getEmail() : "");
        etAddress.setText(currentUser.getAddress() != null ? currentUser.getAddress() : "");
        etPhone.setText(currentUser.getPhone() != null ? currentUser.getPhone() : "");

        etEmail.setEnabled(false);
        etName.setEnabled(false);
        etAddress.setEnabled(false);
        etPhone.setEnabled(false);

        // FIX: Use resource string for button text
        btnEditMode.setText(getString(R.string.profile_edit));
        btnSaveProfile.setVisibility(View.GONE);
    }

    private void setupDarkMode() {
        boolean isDarkMode = sessionManager.isDarkModeEnabled();
        switchDarkMode.setChecked(isDarkMode);

        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    /**
     * Setup Language Selector (EN/VI/ZH)
     */
    private void setupLanguageSelector() {
        String[] languages = LocaleHelper.getLanguageDisplayNames();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, R.layout.spinner_item, languages);
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinnerLanguage.setAdapter(adapter);

        String currentLang = LocaleHelper.getLanguage(this);
        int position = 0;
        switch (currentLang) {
            case "vi":
                position = 1;
                break;
            case "zh":
                position = 2;
                break;
            default:
                position = 0;
        }
        spinnerLanguage.setSelection(position);

        spinnerLanguage.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String[] langCodes = LocaleHelper.getAvailableLanguages();
                String selectedLang = langCodes[position];
                String currentLang = LocaleHelper.getLanguage(ProfileActivity.this);

                if (!selectedLang.equals(currentLang)) {
                    changeLanguage(selectedLang);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void setupClickListeners() {
        fabChangeAvatar.setOnClickListener(v -> showAvatarOptions());

        btnEditMode.setOnClickListener(v -> {
            if (isEditMode) {
                cancelEdit();
            } else {
                enterEditMode();
            }
        });

        btnSaveProfile.setOnClickListener(v -> saveProfile());
        btnChangePassword.setOnClickListener(v -> changePassword());

        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonView.isPressed()) {
                toggleDarkMode(isChecked);
            }
        });

        switchBiometric.setChecked(sessionManager.isBiometricEnabled());

        switchBiometric.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!buttonView.isPressed()) return; // Chỉ xử lý khi người dùng bấm thật

            if (isChecked) {
                // Nếu bật -> Cần xác nhận mật khẩu
                showEnableBiometricDialog();
            } else {
                // Nếu tắt -> Tắt luôn không cần hỏi
                sessionManager.disableBiometric();
                Toast.makeText(this, getString(R.string.biometric_disabled), Toast.LENGTH_SHORT).show();
            }
        });

        btnLogout.setOnClickListener(v -> showLogoutDialog());

        btnSendFeedback.setOnClickListener(v -> openFeedbackActivity());
    }

    private void openFeedbackActivity() {
        Intent intent = new Intent(ProfileActivity.this, FeedbackActivity.class);
        startActivity(intent);
    }

    // FIX: Totally refactored dialog using getString()
    private void showAvatarOptions() {
        String[] options = {
                getString(R.string.dialog_option_camera),
                getString(R.string.dialog_option_gallery),
                getString(R.string.dialog_option_remove)
        };

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.dialog_avatar_title))
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            captureAvatar();
                            break;
                        case 1:
                            chooseAvatarFromGallery();
                            break;
                        case 2:
                            removeAvatar();
                            break;
                    }
                })
                .show();
    }

    private void chooseAvatarFromGallery() {
        Intent pickPhotoIntent = new Intent(Intent.ACTION_PICK);
        pickPhotoIntent.setType("image/*");
        galleryLauncher.launch(pickPhotoIntent);
    }

    private void captureAvatar() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
            return;
        }

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            avatarLauncher.launch(takePictureIntent);
        }
    }

    private void removeAvatar() {
        ivAvatar.setImageResource(android.R.drawable.ic_menu_myplaces);
        currentUser.setAvatarPath(null);
        dbHelper.updateUser(currentUser);
        // FIX: Localized Toast
        Toast.makeText(this, getString(R.string.msg_avatar_removed), Toast.LENGTH_SHORT).show();
    }

    private void enterEditMode() {
        isEditMode = true;
        etName.setEnabled(true);
        etAddress.setEnabled(true);
        etPhone.setEnabled(true);
        // FIX: Use resource string
        btnEditMode.setText(getString(R.string.action_cancel));
        btnSaveProfile.setVisibility(View.VISIBLE);
    }

    private void cancelEdit() {
        isEditMode = false;
        if (currentUser != null) {
            etName.setText(currentUser.getName() != null ? currentUser.getName() : "");
            etAddress.setText(currentUser.getAddress() != null ? currentUser.getAddress() : "");
            etPhone.setText(currentUser.getPhone() != null ? currentUser.getPhone() : "");
        }
        etName.setEnabled(false);
        etAddress.setEnabled(false);
        etPhone.setEnabled(false);
        // FIX: Use resource string
        btnEditMode.setText(getString(R.string.profile_edit));
        btnSaveProfile.setVisibility(View.GONE);
    }

    private void saveProfile() {
        String name = etName.getText().toString().trim();
        String address = etAddress.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();

        // FIX: Use resource strings for errors
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

        if (!phone.matches("^0\\d{9}$")) {
            tilPhone.setError(getString(R.string.error_invalid_phone));
            return;
        }

        tilName.setError(null);
        tilAddress.setError(null);
        tilPhone.setError(null);

        currentUser.setName(name);
        currentUser.setAddress(address);
        currentUser.setPhone(phone);

        int rowsAffected = dbHelper.updateUser(currentUser);

        if (rowsAffected > 0) {
            sessionManager.updateUserName(name);
            // FIX: Localized Toast
            Toast.makeText(this, getString(R.string.profile_updated), Toast.LENGTH_SHORT).show();
            cancelEdit();
        } else {
            Toast.makeText(this, getString(R.string.msg_failed_update_profile), Toast.LENGTH_SHORT).show();
        }
    }

    private void changePassword() {
        String oldPassword = etOldPassword.getText().toString();
        String newPassword = etNewPassword.getText().toString();
        String confirmNewPassword = etConfirmNewPassword.getText().toString();

        if (oldPassword.isEmpty()) {
            tilOldPassword.setError(getString(R.string.error_empty_field));
            return;
        }

        String oldPasswordHash = SessionManager.hashPassword(oldPassword);
        if (!oldPasswordHash.equals(currentUser.getPasswordHash())) {
            // FIX: Localized Error
            tilOldPassword.setError(getString(R.string.msg_incorrect_password));
            return;
        }

        if (newPassword.isEmpty()) {
            tilNewPassword.setError(getString(R.string.error_empty_field));
            return;
        }

        if (newPassword.length() < 8) {
            tilNewPassword.setError(getString(R.string.error_password_short));
            return;
        }

        if (!confirmNewPassword.equals(newPassword)) {
            tilConfirmNewPassword.setError(getString(R.string.error_password_mismatch));
            return;
        }

        tilOldPassword.setError(null);
        tilNewPassword.setError(null);
        tilConfirmNewPassword.setError(null);

        String newPasswordHash = SessionManager.hashPassword(newPassword);
        currentUser.setPasswordHash(newPasswordHash);

        int rowsAffected = dbHelper.updateUser(currentUser);

        if (rowsAffected > 0) {
            // FIX: Localized Toast
            Toast.makeText(this, getString(R.string.profile_password_changed), Toast.LENGTH_SHORT).show();
            etOldPassword.setText("");
            etNewPassword.setText("");
            etConfirmNewPassword.setText("");
        } else {
            Toast.makeText(this, getString(R.string.msg_failed_change_password), Toast.LENGTH_SHORT).show();
        }
    }

    private void toggleDarkMode(boolean enabled) {
        try {
            sessionManager.setDarkMode(enabled);

            if (currentUser != null) {
                currentUser.setDarkModeEnabled(enabled);
                dbHelper.updateUser(currentUser);
            }

            if (enabled) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }

            recreate();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, getString(R.string.msg_error_dark_mode, e.getMessage()), Toast.LENGTH_SHORT).show();
        }
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.confirm_logout_title))
                .setMessage(getString(R.string.confirm_logout_message))
                .setPositiveButton(getString(R.string.action_yes), (dialog, which) -> logout())
                .setNegativeButton(getString(R.string.action_no), null)
                .show();
    }

    private void logout() {
        sessionManager.logout();
        // FIX: Localized Toast
        Toast.makeText(this, getString(R.string.msg_logged_out), Toast.LENGTH_SHORT).show();
        navigateToLogin();
    }

    private void navigateToLogin() {
        Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                captureAvatar();
            } else {
                // FIX: Localized Toast
                Toast.makeText(this, getString(R.string.msg_permission_denied), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showEnableBiometricDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.biometric_enable));
        builder.setMessage(getString(R.string.biometric_verify_password));

        // Tạo ô nhập mật khẩu
        final TextInputEditText input = new TextInputEditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT |
                android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);

        // Tạo khung chứa để căn lề cho đẹp
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

        // Xử lý nút Verify
        builder.setPositiveButton(getString(R.string.biometric_verify), (dialog, which) -> {
            String password = input.getText().toString();

            User user = dbHelper.getUserById(sessionManager.getUserId());

            // Kiểm tra password nhập vào có đúng không
            if (user != null && SessionManager.verifyPassword(password, user.getPasswordHash())) {
                // Nếu đúng -> Bật tính năng vân tay
                sessionManager.enableBiometric(user.getEmail());
                Toast.makeText(this, getString(R.string.biometric_enabled), Toast.LENGTH_SHORT).show();
            } else {
                // Nếu sai -> Báo lỗi và gạt cần Switch về tắt
                Toast.makeText(this, getString(R.string.msg_incorrect_password), Toast.LENGTH_SHORT).show();
                switchBiometric.setChecked(false);
            }
        });

        // Xử lý nút Cancel
        builder.setNegativeButton(getText(R.string.action_cancel), (dialog, which) -> {
            switchBiometric.setChecked(false); // Trả về trạng thái tắt
            dialog.cancel();
        });

        builder.show();
    }
}