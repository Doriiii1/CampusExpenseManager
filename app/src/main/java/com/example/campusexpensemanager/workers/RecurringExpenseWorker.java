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
            // Get all recurring expenses that are due
            List<Expense> dueExpenses = dbHelper.getDueRecurringExpenses();

            if (dueExpenses.isEmpty()) {
                Log.d(TAG, "No recurring expenses due");
                return Result.success();
            }

            int created = 0;
            for (Expense expense : dueExpenses) {
                long newId = dbHelper.createRecurringOccurrence(expense);
                if (newId != -1) {
                    created++;
                    Log.d(TAG, "Created recurring expense: " + newId +
                            " (Original: " + expense.getId() + ")");
                }
            }

            Log.d(TAG, "RecurringExpenseWorker completed: " + created + " expenses created");
            return Result.success();

        } catch (Exception e) {
            Log.e(TAG, "Error in RecurringExpenseWorker: " + e.getMessage());
            e.printStackTrace();
            return Result.retry(); // Retry on failure
        }
    }
}