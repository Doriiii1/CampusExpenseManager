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
                boolean isEnded = false; // Cờ đánh dấu đã kết thúc chưa
                while (nextOcc <= currentTime) {

                    //  Kiểm tra ngày kết thúc
                    // Nếu có ngày kết thúc ( > 0) VÀ ngày tiếp theo vượt quá ngày đó
                    if (expense.getRecurringEndDate() > 0 && nextOcc > expense.getRecurringEndDate()) {
                        Log.d(TAG, "Recurring expense ended: " + expense.getId());

                        // Tắt lặp lại
                        expense.setIsRecurring(false);

                        // Cập nhật ngày tiếp theo (để lưu trạng thái cuối)
                        expense.setNextOccurrenceDate(nextOcc);

                        // Lưu vào DB ngay lập tức
                        dbHelper.updateExpense(expense);
                        isEnded = true;
                        break; // Thoát khỏi vòng lặp tạo mới
                    }

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
                // Nếu chưa kết thúc thì cập nhật ngày tiếp theo cho lần chạy sau
                if (!isEnded) {
                    expense.setNextOccurrenceDate(nextOcc);
                    // Kiểm tra phụ: Nếu ngày tiếp theo (vừa tính xong) đã vượt quá End Date
                    // thì tắt luôn bây giờ để đỡ tốn công lần sau quét lại
                    if (expense.getRecurringEndDate() > 0 && nextOcc > expense.getRecurringEndDate()) {
                        expense.setIsRecurring(false);
                    }
                    dbHelper.updateExpense(expense);
                }
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

        if (period == null) return currentDate;

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