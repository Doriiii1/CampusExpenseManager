package com.example.campusexpensemanager.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.campusexpensemanager.R;
import com.example.campusexpensemanager.utils.DatabaseHelper;
import com.example.campusexpensemanager.utils.SessionManager;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * FeedbackActivity - Priority 4
 * Allows users to submit feedback and ratings
 */
public class FeedbackActivity extends BaseActivity {

    private RatingBar ratingBar;
    private TextView tvRatingText;
    private TextInputLayout tilFeedback;
    private TextInputEditText etFeedback;
    private ChipGroup chipGroupSuggestions;
    private Button btnCancel, btnSubmit;

    private DatabaseHelper dbHelper;
    private SessionManager sessionManager;

    private static final String[] RATING_TEXTS = {
            "",
            "Poor",
            "Fair",
            "Good",
            "Very Good",
            "Excellent"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback);

        dbHelper = DatabaseHelper.getInstance(this);
        sessionManager = new SessionManager(this);

        initializeViews();
        setupListeners();
    }

    private void initializeViews() {
        ratingBar = findViewById(R.id.rating_bar);
        tvRatingText = findViewById(R.id.tv_rating_text);
        tilFeedback = findViewById(R.id.til_feedback);
        etFeedback = findViewById(R.id.et_feedback);
        chipGroupSuggestions = findViewById(R.id.chip_group_suggestions);
        btnCancel = findViewById(R.id.btn_cancel);
        btnSubmit = findViewById(R.id.btn_submit);

        // Set default rating text
        updateRatingText(5); // Default 5 stars
    }

    private void setupListeners() {
        // Rating bar change listener
        ratingBar.setOnRatingBarChangeListener((ratingBar, rating, fromUser) -> {
            if (fromUser) {
                updateRatingText((int) rating);
            }
        });

        // Chip click listeners (append suggestion to feedback)
        setupChipListeners();

        // Cancel button
        btnCancel.setOnClickListener(v -> finish());

        // Submit button
        btnSubmit.setOnClickListener(v -> submitFeedback());
    }

    private void setupChipListeners() {
        Chip chipUI = findViewById(R.id.chip_ui);
        Chip chipPerformance = findViewById(R.id.chip_performance);
        Chip chipFeatures = findViewById(R.id.chip_features);
        Chip chipBugs = findViewById(R.id.chip_bugs);

        chipUI.setOnClickListener(v -> appendSuggestion(getString(R.string.feedback_chip_ui)));
        chipPerformance.setOnClickListener(v -> appendSuggestion(getString(R.string.feedback_chip_performance)));
        chipFeatures.setOnClickListener(v -> appendSuggestion(getString(R.string.feedback_chip_features)));
        chipBugs.setOnClickListener(v -> appendSuggestion(getString(R.string.feedback_chip_bugs)));
    }

    private void appendSuggestion(String suggestion) {
        String currentText = etFeedback.getText() != null ? etFeedback.getText().toString() : "";

        if (currentText.isEmpty()) {
            etFeedback.setText(suggestion);
        } else if (!currentText.contains(suggestion)) {
            etFeedback.setText(currentText + "\n• " + suggestion);
        }

        // Move cursor to end
        if (etFeedback.getText() != null) {
            etFeedback.setSelection(etFeedback.getText().length());
        }
    }

    private void updateRatingText(int rating) {
        if (rating >= 1 && rating <= 5) {
            tvRatingText.setText(RATING_TEXTS[rating]);

            // Change color based on rating
            int color;
            if (rating >= 4) {
                color = getColor(R.color.success); // Green
            } else if (rating == 3) {
                color = getColor(R.color.warning); // Orange
            } else {
                color = getColor(R.color.error); // Red
            }
            tvRatingText.setTextColor(color);
        }
    }

    private void submitFeedback() {
        // Validate rating
        int rating = (int) ratingBar.getRating();
        if (rating == 0) {
            Toast.makeText(this, getString(R.string.feedback_error_no_rating),
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Get feedback content (optional)
        String content = etFeedback.getText() != null ?
                etFeedback.getText().toString().trim() : "";

        // Get selected chips as tags
        List<String> selectedChips = getSelectedChips();
        if (!selectedChips.isEmpty()) {
            String tags = "\n\nTags: " + String.join(", ", selectedChips);
            content += tags;
        }

        // Get current user
        int userId = sessionManager.getUserId();
        if (userId == -1) {
            Toast.makeText(this, getString(R.string.error_generic),
                    Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Insert feedback into database
        long feedbackId = dbHelper.insertFeedback(userId, rating, content);

        if (feedbackId != -1) {
            Toast.makeText(this, getString(R.string.feedback_success),
                    Toast.LENGTH_LONG).show();

            // Optional: Show thank you message based on rating
            if (rating >= 4) {
                Toast.makeText(this, getString(R.string.feedback_thank_you_positive),
                        Toast.LENGTH_SHORT).show();
            }

            finish();
        } else {
            Toast.makeText(this, getString(R.string.feedback_error_submit),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private List<String> getSelectedChips() {
        List<String> selected = new ArrayList<>();

        Chip chipUI = findViewById(R.id.chip_ui);
        Chip chipPerformance = findViewById(R.id.chip_performance);
        Chip chipFeatures = findViewById(R.id.chip_features);
        Chip chipBugs = findViewById(R.id.chip_bugs);

        if (chipUI.isChecked()) selected.add(chipUI.getText().toString());
        if (chipPerformance.isChecked()) selected.add(chipPerformance.getText().toString());
        if (chipFeatures.isChecked()) selected.add(chipFeatures.getText().toString());
        if (chipBugs.isChecked()) selected.add(chipBugs.getText().toString());

        return selected;
    }
}