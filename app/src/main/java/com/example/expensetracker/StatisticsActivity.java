package com.example.expensetracker;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StatisticsActivity extends AppCompatActivity {

    private ExpenseDBHelper dbHelper;
    private TextView totalWeeklyExpenseText;
    private TextView totalMonthlyExpenseText;
    private BarChart weeklyChart;
    private BarChart monthlyChart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);

        dbHelper = new ExpenseDBHelper(this);

        totalWeeklyExpenseText = findViewById(R.id.totalWeeklyExpenseText);
        totalMonthlyExpenseText = findViewById(R.id.totalMonthlyExpenseText);
        weeklyChart = findViewById(R.id.weeklyChart);
        monthlyChart = findViewById(R.id.monthlyChart);

        setupWeeklyChart();
        setupMonthlyChart();
    }

    private void setupWeeklyChart() {
        // Get expenses for the current week
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek());

        // Create sample data or get from database
        ArrayList<BarEntry> entries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();

        // Get day names
        SimpleDateFormat sdf = new SimpleDateFormat("EEE", Locale.getDefault());

        for (int i = 0; i < 7; i++) {
            calendar.set(Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek() + i);
            String dayName = sdf.format(calendar.getTime());
            labels.add(dayName);

            // Get expenses for this day (sample data)
            float value = (float) (Math.random() * 1000);
            entries.add(new BarEntry(i, value));
        }

        BarDataSet dataSet = new BarDataSet(entries, "Weekly Expenses");
        dataSet.setColor(getResources().getColor(R.color.colorPrimary));

        BarData data = new BarData(dataSet);
        data.setBarWidth(0.6f);

        weeklyChart.setData(data);
        weeklyChart.getDescription().setEnabled(false);
        weeklyChart.setDrawGridBackground(false);

        // Configure X-axis
        XAxis xAxis = weeklyChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);

        weeklyChart.getAxisLeft().setDrawGridLines(false);
        weeklyChart.getAxisRight().setEnabled(false);
        weeklyChart.getLegend().setEnabled(false);

        weeklyChart.invalidate();

        // Set total weekly expense
        float totalWeeklyExpense = 0;
        for (BarEntry entry : entries) {
            totalWeeklyExpense += entry.getY();
        }

        totalWeeklyExpenseText.setText("$" + String.format("%.2f", totalWeeklyExpense));
    }

    private void setupMonthlyChart() {
        // Get expenses for the current month
        // Similar implementation to weekly chart but with monthly data
        ArrayList<BarEntry> entries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();

        for (int i = 0; i < 12; i++) {
            String monthName = new SimpleDateFormat("MMM", Locale.getDefault()).format(new java.util.Date(0, i, 1));
            labels.add(monthName);

            // Get expenses for this month (sample data)
            float value = (float) (Math.random() * 5000);
            entries.add(new BarEntry(i, value));
        }

        BarDataSet dataSet = new BarDataSet(entries, "Monthly Expenses");
        dataSet.setColor(getResources().getColor(R.color.colorAccent));

        BarData data = new BarData(dataSet);
        data.setBarWidth(0.6f);

        monthlyChart.setData(data);
        monthlyChart.getDescription().setEnabled(false);
        monthlyChart.setDrawGridBackground(false);

        // Configure X-axis
        XAxis xAxis = monthlyChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);

        monthlyChart.getAxisLeft().setDrawGridLines(false);
        monthlyChart.getAxisRight().setEnabled(false);
        monthlyChart.getLegend().setEnabled(false);

        monthlyChart.invalidate();

        // Set total monthly expense
        float totalMonthlyExpense = 0;
        for (BarEntry entry : entries) {
            totalMonthlyExpense += entry.getY();
        }

        totalMonthlyExpenseText.setText("$" + String.format("%.2f", totalMonthlyExpense));
    }
}