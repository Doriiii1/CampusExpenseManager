package com.example.campusexpensemanager.activities;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.campusexpensemanager.R;
import com.example.campusexpensemanager.models.Category;
import com.example.campusexpensemanager.models.Expense;
import com.example.campusexpensemanager.utils.DatabaseHelper;
import com.example.campusexpensemanager.utils.SessionManager;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.PercentFormatter;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * ExpenseOverviewActivity - Priority 2: Visual Charts
 * Displays Pie Chart (category breakdown) and Line Chart (6-month trend)
 */
public class ExpenseOverviewActivity extends BaseActivity {

    private PieChart pieChart;
    private LineChart lineChart;
    private TextView tvTotalSpent, tvPeriodRange;

    private DatabaseHelper dbHelper;
    private SessionManager sessionManager;
    private NumberFormat currencyFormat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expense_overview);

        dbHelper = DatabaseHelper.getInstance(this);
        sessionManager = new SessionManager(this);
        currencyFormat = NumberFormat.getInstance(new Locale("vi", "VN"));

        if (!sessionManager.isLoggedIn()) {
            finish();
            return;
        }

        initializeViews();
        loadChartData();
    }

    private void initializeViews() {
        pieChart = findViewById(R.id.pie_chart);
        lineChart = findViewById(R.id.line_chart);
        tvTotalSpent = findViewById(R.id.tv_total_spent);
        tvPeriodRange = findViewById(R.id.tv_period_range);
    }

    /**
     * Load and display chart data
     */
    private void loadChartData() {
        int userId = sessionManager.getUserId();
        List<Expense> allExpenses = dbHelper.getExpensesByUser(userId);

        // Calculate current month range
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        long monthStart = calendar.getTimeInMillis();

        calendar.add(Calendar.MONTH, 1);
        long monthEnd = calendar.getTimeInMillis();

        // Display period
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM yyyy", Locale.getDefault());
        tvPeriodRange.setText("Period: " + dateFormat.format(monthStart) + " - " +
                dateFormat.format(monthEnd));

        // Setup Pie Chart (Category Breakdown)
        setupPieChart(allExpenses, monthStart, monthEnd);

        // Setup Line Chart (6-Month Trend)
        setupLineChart(allExpenses);
    }

    /**
     * ✅ Setup Pie Chart - Category breakdown for current month
     */
    private void setupPieChart(List<Expense> expenses, long monthStart, long monthEnd) {
        Map<Integer, Double> categoryTotals = new HashMap<>();
        double totalSpent = 0;

        // Aggregate by category (only expenses, not income)
        for (Expense expense : expenses) {
            if (expense.getDate() >= monthStart && expense.getDate() < monthEnd) {
                if (expense.isExpense()) {
                    int categoryId = expense.getCategoryId();
                    double amount = expense.getAmount();
                    categoryTotals.put(categoryId,
                            categoryTotals.getOrDefault(categoryId, 0.0) + amount);
                    totalSpent += amount;
                }
            }
        }

        // Display total
        tvTotalSpent.setText("Total Spent: " + currencyFormat.format(totalSpent) + "đ");

        if (categoryTotals.isEmpty()) {
            pieChart.setNoDataText("No expenses this month");
            pieChart.invalidate();
            return;
        }

        // Create Pie Chart entries
        List<PieEntry> pieEntries = new ArrayList<>();
        int[] colors = getSeraUIColors();

        for (Map.Entry<Integer, Double> entry : categoryTotals.entrySet()) {
            Category category = dbHelper.getCategoryById(entry.getKey());
            String categoryName = category != null ? category.getName() : "Unknown";
            float value = entry.getValue().floatValue();
            pieEntries.add(new PieEntry(value, categoryName));
        }

        // Setup Pie Chart styling (Sera UI)
        PieDataSet dataSet = new PieDataSet(pieEntries, "Expense by Category");
        dataSet.setColors(colors);
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setSliceSpace(3f);
        dataSet.setValueFormatter(new PercentFormatter(pieChart));

        PieData pieData = new PieData(dataSet);
        pieChart.setData(pieData);

        // Chart appearance
        pieChart.setUsePercentValues(true);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(Color.WHITE);
        pieChart.setHoleRadius(40f);
        pieChart.setTransparentCircleRadius(45f);
        pieChart.setDrawCenterText(true);
        pieChart.setCenterText("Expenses\nby Category");
        pieChart.setCenterTextSize(14f);
        pieChart.setRotationEnabled(true);
        pieChart.setHighlightPerTapEnabled(true);

        // Description
        Description description = new Description();
        description.setText("");
        pieChart.setDescription(description);

        // Legend
        Legend legend = pieChart.getLegend();
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setDrawInside(false);
        legend.setWordWrapEnabled(true);
        legend.setTextSize(10f);

        pieChart.animateY(1000);
        pieChart.invalidate();
    }

    /**
     * ✅ Setup Line Chart - 6-month spending trend
     */
    private void setupLineChart(List<Expense> expenses) {
        // Calculate last 6 months
        Calendar calendar = Calendar.getInstance();
        List<String> monthLabels = new ArrayList<>();
        List<Entry> lineEntries = new ArrayList<>();

        // Start from 5 months ago
        calendar.add(Calendar.MONTH, -5);

        for (int i = 0; i < 6; i++) {
            // Get month boundaries
            calendar.set(Calendar.DAY_OF_MONTH, 1);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            long monthStart = calendar.getTimeInMillis();

            calendar.add(Calendar.MONTH, 1);
            long monthEnd = calendar.getTimeInMillis();

            // Calculate total for this month
            double monthTotal = 0;
            for (Expense expense : expenses) {
                if (expense.getDate() >= monthStart && expense.getDate() < monthEnd) {
                    if (expense.isExpense()) {
                        monthTotal += expense.getAmount();
                    }
                }
            }

            // Add to chart
            lineEntries.add(new Entry(i, (float) monthTotal));

            // Format month label
            SimpleDateFormat monthFormat = new SimpleDateFormat("MMM", Locale.getDefault());
            calendar.add(Calendar.MONTH, -1); // Go back to display month name
            monthLabels.add(monthFormat.format(calendar.getTime()));
            calendar.add(Calendar.MONTH, 1); // Move to next month
        }

        // Create Line Chart dataset
        LineDataSet dataSet = new LineDataSet(lineEntries, "Monthly Spending");
        dataSet.setColor(ContextCompat.getColor(this, R.color.primary_blue));
        dataSet.setCircleColor(ContextCompat.getColor(this, R.color.primary_blue));
        dataSet.setCircleRadius(5f);
        dataSet.setLineWidth(3f);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(ContextCompat.getColor(this, R.color.primary_blue_light));
        dataSet.setFillAlpha(50);
        dataSet.setValueTextSize(10f);
        dataSet.setValueTextColor(ContextCompat.getColor(this, R.color.primary_blue_dark));
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setDrawValues(false); // Hide values on points for cleaner look

        LineData lineData = new LineData(dataSet);
        lineChart.setData(lineData);

        // X-Axis (Month labels)
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(monthLabels));
        xAxis.setTextSize(10f);
        xAxis.setDrawGridLines(false);

        // Y-Axis
        lineChart.getAxisLeft().setTextSize(10f);
        lineChart.getAxisLeft().setDrawGridLines(true);
        lineChart.getAxisRight().setEnabled(false);

        // Chart appearance
        Description description = new Description();
        description.setText("6-Month Spending Trend");
        description.setTextSize(12f);
        lineChart.setDescription(description);

        // Legend
        Legend legend = lineChart.getLegend();
        legend.setEnabled(false);

        lineChart.setTouchEnabled(true);
        lineChart.setDragEnabled(true);
        lineChart.setScaleEnabled(false);
        lineChart.setPinchZoom(false);
        lineChart.setDrawGridBackground(false);
        lineChart.animateX(1000);
        lineChart.invalidate();
    }

    /**
     * Get Sera UI color palette for charts
     */
    private int[] getSeraUIColors() {
        return new int[]{
                ContextCompat.getColor(this, R.color.primary_blue),
                ContextCompat.getColor(this, R.color.secondary_teal),
                ContextCompat.getColor(this, R.color.accent_orange),
                ContextCompat.getColor(this, R.color.accent_green),
                ContextCompat.getColor(this, R.color.accent_red),
                ContextCompat.getColor(this, R.color.accent_yellow),
                ContextCompat.getColor(this, R.color.primary_blue_light),
                ContextCompat.getColor(this, R.color.secondary_teal_light),
                ContextCompat.getColor(this, R.color.budget_warning),
                ContextCompat.getColor(this, R.color.budget_danger)
        };
    }
}