package com.example.campusexpensemanager.utils;

import android.content.Context;
import android.content.DialogInterface;

import androidx.appcompat.app.AlertDialog;

import com.example.campusexpensemanager.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/**
 * RecurringActionDialog - Priority 3
 * Helper class for showing dialogs when editing/deleting recurring expenses
 */
public class RecurringActionDialog {

    public interface OnActionSelectedListener {
        void onThisOnly();
        void onAllFuture();
        void onCancel();
    }

    /**
     * Show dialog asking user to edit "This only" or "All future occurrences"
     */
    public static void showEditDialog(Context context, OnActionSelectedListener listener) {
        new MaterialAlertDialogBuilder(context)
                .setTitle("🔁 Edit Recurring Transaction")
                .setMessage("This is a recurring transaction. What would you like to edit?")
                .setPositiveButton("✏️ This Only", (dialog, which) -> {
                    if (listener != null) {
                        listener.onThisOnly();
                    }
                })
                .setNegativeButton("📝 All Future", (dialog, which) -> {
                    if (listener != null) {
                        listener.onAllFuture();
                    }
                })
                .setNeutralButton("Cancel", (dialog, which) -> {
                    if (listener != null) {
                        listener.onCancel();
                    }
                })
                .setCancelable(true)
                .show();
    }

    /**
     * Show dialog asking user to delete "This only" or "All future occurrences"
     */
    public static void showDeleteDialog(Context context, OnActionSelectedListener listener) {
        new MaterialAlertDialogBuilder(context)
                .setTitle("🗑️ Delete Recurring Transaction")
                .setMessage("This is a recurring transaction. What would you like to delete?")
                .setPositiveButton("🗑️ This Only", (dialog, which) -> {
                    if (listener != null) {
                        listener.onThisOnly();
                    }
                })
                .setNegativeButton("❌ All Future", (dialog, which) -> {
                    if (listener != null) {
                        listener.onAllFuture();
                    }
                })
                .setNeutralButton("Cancel", (dialog, which) -> {
                    if (listener != null) {
                        listener.onCancel();
                    }
                })
                .setCancelable(true)
                .show();
    }

    /**
     * Show info dialog explaining recurring frequency
     */
    public static void showRecurringInfoDialog(Context context, String frequency) {
        String message;
        switch (frequency.toLowerCase()) {
            case "daily":
                message = "📅 Daily: This transaction will repeat every day automatically.";
                break;
            case "weekly":
                message = "📅 Weekly: This transaction will repeat every 7 days automatically.";
                break;
            case "monthly":
                message = "📅 Monthly: This transaction will repeat every month automatically.";
                break;
            default:
                message = "This transaction will repeat automatically.";
        }

        new MaterialAlertDialogBuilder(context)
                .setTitle("🔁 Recurring Transaction Info")
                .setMessage(message)
                .setPositiveButton("Got it", null)
                .show();
    }

    /**
     * Show confirmation before enabling recurring
     */
    public static void showEnableRecurringDialog(Context context,
                                                 DialogInterface.OnClickListener onConfirm) {
        new MaterialAlertDialogBuilder(context)
                .setTitle("🔁 Enable Recurring?")
                .setMessage("This transaction will be automatically created in the future based on the selected frequency.\n\nYou can edit or delete future occurrences anytime.")
                .setPositiveButton("Enable", onConfirm)
                .setNegativeButton("Cancel", null)
                .show();
    }
}