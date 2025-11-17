package com.example.campusexpensemanager.workers;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.campusexpensemanager.models.Expense;
import com.example.campusexpensemanager.utils.DatabaseHelper;

import java.util.List;

/**
 * RecurringExpenseWorker - Background worker to create recurring expenses
 * Runs daily via WorkManager
 */
public class RecurringExpenseWorker extends Worker {

    private static final String TAG = "RecurringWorker";
    private DatabaseHelper dbHelper;

    public RecurringExpenseWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        dbHelper = DatabaseHelper.getInstance(context);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "RecurringExpenseWorker started");

        try {
            List<Expense> dueExpenses = dbHelper.getDueRecurringExpenses();

            if (dueExpenses.isEmpty()) {
                Log.d(TAG, "No recurring expenses due");
                return Result.success();
            }

            int created = 0;
            long currentTime = System.currentTimeMillis();

            for (Expense expense : dueExpenses) {
                // FIX: Catch up ALL missed occurrences
                long nextOcc = expense.getNextOccurrenceDate();

                while (nextOcc <= currentTime) {
                    // Create occurrence for this date
                    long newId = dbHelper.createRecurringOccurrence(expense);
                    if (newId != -1) {
                        created++;
                        Log.d(TAG, "Created recurring expense: " + newId +
                                " (Original: " + expense.getId() + ", Date: " + nextOcc + ")");
                    }

                    // Move to next occurrence
                    nextOcc = calculateNextOccurrence(nextOcc, expense.getRecurrencePeriod());
                }

                // Update the original expense with final next occurrence
                expense.setNextOccurrenceDate(nextOcc);
                dbHelper.updateExpense(expense);
            }

            Log.d(TAG, "RecurringExpenseWorker completed: " + created + " expenses created");
            return Result.success();

        } catch (Exception e) {
            Log.e(TAG, "Error in RecurringExpenseWorker: " + e.getMessage());
            e.printStackTrace();
            return Result.retry();
        }
    }

    /**
     * Calculate next occurrence date based on period
     */
    private long calculateNextOccurrence(long currentDate, String period) {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.setTimeInMillis(currentDate);

        switch (period.toLowerCase()) {
            case "daily":
                calendar.add(java.util.Calendar.DAY_OF_MONTH, 1);
                break;
            case "weekly":
                calendar.add(java.util.Calendar.WEEK_OF_YEAR, 1);
                break;
            case "monthly":
            default:
                calendar.add(java.util.Calendar.MONTH, 1);
                break;
        }

        return calendar.getTimeInMillis();
    }
}