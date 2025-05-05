package com.example.expensetracker;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class StatisticsActivity extends AppCompatActivity {

    private ExpenseDBHelper dbHelper;
    private TextView totalWeeklyExpenseText;
    private TextView totalMonthlyExpenseText;
    private RecyclerView weeklyStatsRecyclerView;
    private RecyclerView monthlyStatsRecyclerView;
    private int userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);

        // Set up back button in action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Statistics");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Get the current user ID
        SharedPreferences prefs = getSharedPreferences("expense_tracker", MODE_PRIVATE);
        userId = prefs.getInt("current_user_id", -1);
        if (userId == -1) {
            Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        dbHelper = new ExpenseDBHelper(this);

        // Reference views from layout
        // Note: You'll need to update your layout file to use RecyclerViews instead of Charts
        totalWeeklyExpenseText = findViewById(R.id.totalWeeklyExpenseText);
        totalMonthlyExpenseText = findViewById(R.id.totalMonthlyExpenseText);
        weeklyStatsRecyclerView = findViewById(R.id.weeklyChart); // Renamed from weeklyChart to weeklyStatsRecyclerView
        monthlyStatsRecyclerView = findViewById(R.id.monthlyChart); // Renamed from monthlyChart to monthlyStatsRecyclerView

        setupWeeklyStats();
        setupMonthlyStats();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void setupWeeklyStats() {
        // Get current week days
        ArrayList<StatItem> weeklyStats = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();
        float totalWeeklyExpense = 0;

        // Get day names
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek());
        SimpleDateFormat sdf = new SimpleDateFormat("EEE", Locale.getDefault());

        for (int i = 0; i < 7; i++) {
            calendar.set(Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek() + i);
            String dayName = sdf.format(calendar.getTime());

            // Get expenses for this day (sample data)
            // In a real app, you would query your database for expenses on this day
            float value = (float) (Math.random() * 1000);
            totalWeeklyExpense += value;

            weeklyStats.add(new StatItem(dayName, value));
        }

        // Set up RecyclerView
        weeklyStatsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        StatisticsAdapter weeklyAdapter = new StatisticsAdapter(weeklyStats);
        weeklyStatsRecyclerView.setAdapter(weeklyAdapter);

        // Set total weekly expense
        totalWeeklyExpenseText.setText("$" + String.format("%.2f", totalWeeklyExpense));
    }

    private void setupMonthlyStats() {
        ArrayList<StatItem> monthlyStats = new ArrayList<>();
        float totalMonthlyExpense = 0;

        // Get month names
        for (int i = 0; i < 12; i++) {
            String monthName = new SimpleDateFormat("MMM", Locale.getDefault()).format(new java.util.Date(0, i, 1));

            // Get expense for this month (sample data)
            // In a real app, you would query your database for monthly expenses
            float value = (float) (Math.random() * 5000);
            totalMonthlyExpense += value;

            monthlyStats.add(new StatItem(monthName, value));
        }

        // Set up RecyclerView
        monthlyStatsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        StatisticsAdapter monthlyAdapter = new StatisticsAdapter(monthlyStats);
        monthlyStatsRecyclerView.setAdapter(monthlyAdapter);

        // Set total monthly expense
        totalMonthlyExpenseText.setText("$" + String.format("%.2f", totalMonthlyExpense));
    }

    // Inner class for statistics item
    private static class StatItem {
        String label;
        float value;

        StatItem(String label, float value) {
            this.label = label;
            this.value = value;
        }
    }

    // Adapter for statistics RecyclerView
    private class StatisticsAdapter extends RecyclerView.Adapter<StatisticsAdapter.StatViewHolder> {
        private List<StatItem> statItems;

        StatisticsAdapter(List<StatItem> statItems) {
            this.statItems = statItems;
        }

        @NonNull
        @Override
        public StatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // Create a simple layout for each item
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_statistics, parent, false);
            return new StatViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull StatViewHolder holder, int position) {
            StatItem item = statItems.get(position);
            holder.labelText.setText(item.label);
            holder.valueText.setText("$" + String.format("%.2f", item.value));

            // Set bar width based on value
            // Find the maximum value for scaling
            float maxValue = 0;
            for (StatItem statItem : statItems) {
                if (statItem.value > maxValue) {
                    maxValue = statItem.value;
                }
            }

            // Calculate percentage of maximum
            int percentage = (int)((item.value / maxValue) * 100);
            ViewGroup.LayoutParams params = holder.barView.getLayoutParams();
            params.width = percentage * 2; // Width based on percentage
            holder.barView.setLayoutParams(params);
        }

        @Override
        public int getItemCount() {
            return statItems.size();
        }

        class StatViewHolder extends RecyclerView.ViewHolder {
            TextView labelText;
            TextView valueText;
            View barView;

            StatViewHolder(View itemView) {
                super(itemView);
                labelText = itemView.findViewById(R.id.statLabel);
                valueText = itemView.findViewById(R.id.statValue);
                barView = itemView.findViewById(R.id.statBar);
            }
        }
    }
}