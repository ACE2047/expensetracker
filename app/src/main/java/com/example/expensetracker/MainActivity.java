package com.example.expensetracker;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.utils.MPPointF;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private TextView totalExpenseText, welcomeText;
    private PieChart pieChart;
    private RecyclerView topExpensesRecyclerView;
    private RecyclerView categoriesLegendRecyclerView; // ✅ Added legend RecyclerView
    private ExpenseAdapter expenseAdapter;
    private ExpenseDBHelper dbHelper;
    private TextView incomeText, balanceText, goalText;
    private Button setBudgetButton, addExpenseButton;
    private TextView viewAllText;
    private ImageButton logoutButton;
    private static final int EXPENSE_HISTORY_REQUEST_CODE = 1001;

    private static final String[] EXPENSE_CATEGORIES = {
            "Housing", "Groceries", "Transportation", "Utilities",
            "Healthcare", "Dining Out", "Entertainment", "Clothing",
            "Personal Care", "Education", "Travel", "Gifts",
            "Electronics", "Home Improvement", "Subscriptions", "Other"
    };

    private double totalIncome = 60000;
    private double totalBalance = 3200;
    private double totalGoal = 13000;

    private int currentUserId;
    private String currentUsername;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = getSharedPreferences("expense_tracker", MODE_PRIVATE);
        currentUserId = prefs.getInt("current_user_id", -1);
        currentUsername = prefs.getString("current_username", "");

        if (currentUserId == -1) {
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        dbHelper = new ExpenseDBHelper(this);

        welcomeText = findViewById(R.id.welcomeText);
        totalExpenseText = findViewById(R.id.totalExpenseText);
        pieChart = findViewById(R.id.pieChart);
        topExpensesRecyclerView = findViewById(R.id.topExpensesRecyclerView);
        incomeText = findViewById(R.id.incomeText);
        balanceText = findViewById(R.id.balanceText);
        goalText = findViewById(R.id.goalText);
        setBudgetButton = findViewById(R.id.setBudgetButton);
        addExpenseButton = findViewById(R.id.addExpenseButton);
        viewAllText = findViewById(R.id.viewAllText);
        logoutButton = findViewById(R.id.logoutButton);
        categoriesLegendRecyclerView = findViewById(R.id.categoriesLegendRecyclerView); // ✅ Init legend RecyclerView
        categoriesLegendRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        welcomeText.setText("Welcome, " + currentUsername + "!");

        topExpensesRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        logoutButton.setOnClickListener(v -> logout());

        loadData();

        addExpenseButton.setOnClickListener(v -> showAddExpenseDialog());
        setBudgetButton.setOnClickListener(v -> showSetBudgetDialog());

        viewAllText.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ExpenseHistoryActivity.class);
            intent.putExtra("user_id", currentUserId);
            startActivityForResult(intent, EXPENSE_HISTORY_REQUEST_CODE);
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_statistics) {
            startActivity(new Intent(MainActivity.this, StatisticsActivity.class));
            return true;
        } else if (item.getItemId() == R.id.action_logout) {
            logout();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == EXPENSE_HISTORY_REQUEST_CODE && resultCode == RESULT_OK) {
            loadData();
        }
    }

    private void loadData() {
        // Load expenses from database
        List<Expense> expenses = dbHelper.getAllExpenses(currentUserId);

        // Calculate total expense
        double totalExpense = 0;
        for (Expense expense : expenses) {
            totalExpense += expense.getAmount();
        }

        // Set total expense text
        totalExpenseText.setText("Total Spent: $" + String.format("%.2f", totalExpense));

        // Set other financial info
        incomeText.setText("Income: $" + String.format("%.2f", totalIncome));
        balanceText.setText("Balance: $" + String.format("%.2f", totalBalance));
        goalText.setText("Goal: $" + String.format("%.2f", totalGoal));

        // Set up pie chart
        setupPieChart(expenses);

        // Show top expenses (limit to 5)
        List<Expense> topExpenses = expenses.size() > 5 ?
                expenses.subList(0, 5) : expenses;

        expenseAdapter = new ExpenseAdapter(topExpenses);
        topExpensesRecyclerView.setAdapter(expenseAdapter);
    }

    private void showAddExpenseDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_expense, null);
        EditText amountInput = dialogView.findViewById(R.id.amountInput);
        Spinner categorySpinner = dialogView.findViewById(R.id.categorySpinner);
        EditText descriptionInput = dialogView.findViewById(R.id.descriptionInput);

        // Set up category spinner
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_dropdown_item, EXPENSE_CATEGORIES);
        categorySpinner.setAdapter(categoryAdapter);

        new AlertDialog.Builder(this)
                .setTitle("Add Expense")
                .setView(dialogView)
                .setPositiveButton("Add", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            String amountText = amountInput.getText().toString();
                            if (amountText.isEmpty()) {
                                Toast.makeText(MainActivity.this, "Please enter an amount", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            double amount = Double.parseDouble(amountText);
                            String category = categorySpinner.getSelectedItem().toString();
                            String description = descriptionInput.getText().toString();

                            // Create new expense object (using -1 as temp ID)
                            Expense expense = new Expense(-1, amount, category, description);

                            // Add to database
                            dbHelper.addExpense(expense, currentUserId);

                            // Reload data
                            loadData();

                            Toast.makeText(MainActivity.this, "Expense added successfully", Toast.LENGTH_SHORT).show();
                        } catch (NumberFormatException e) {
                            Toast.makeText(MainActivity.this, "Invalid amount", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showSetBudgetDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_set_budget, null);
        EditText incomeInput = dialogView.findViewById(R.id.incomeInput);
        EditText balanceInput = dialogView.findViewById(R.id.balanceInput);
        EditText goalInput = dialogView.findViewById(R.id.goalInput);

        // Pre-fill current values
        incomeInput.setText(String.format("%.2f", totalIncome));
        balanceInput.setText(String.format("%.2f", totalBalance));
        goalInput.setText(String.format("%.2f", totalGoal));

        new AlertDialog.Builder(this)
                .setTitle("Set Financial Info")
                .setView(dialogView)
                .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            totalIncome = Double.parseDouble(incomeInput.getText().toString());
                            totalBalance = Double.parseDouble(balanceInput.getText().toString());
                            totalGoal = Double.parseDouble(goalInput.getText().toString());

                            // Save to SharedPreferences
                            SharedPreferences.Editor editor = getSharedPreferences("expense_tracker", MODE_PRIVATE).edit();
                            editor.putFloat("income", (float) totalIncome);
                            editor.putFloat("balance", (float) totalBalance);
                            editor.putFloat("goal", (float) totalGoal);
                            editor.apply();

                            // Update UI
                            incomeText.setText("Income: $" + String.format("%.2f", totalIncome));
                            balanceText.setText("Balance: $" + String.format("%.2f", totalBalance));
                            goalText.setText("Goal: $" + String.format("%.2f", totalGoal));

                            Toast.makeText(MainActivity.this, "Financial info updated", Toast.LENGTH_SHORT).show();
                        } catch (NumberFormatException e) {
                            Toast.makeText(MainActivity.this, "Invalid values", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void logout() {
        SharedPreferences.Editor editor = getSharedPreferences("expense_tracker", MODE_PRIVATE).edit();
        editor.remove("current_user_id");
        editor.remove("current_username");
        editor.apply();
        startActivity(new Intent(MainActivity.this, LoginActivity.class));
        finish();
    }

    // ✅ REPLACED setupPieChart
    private void setupPieChart(List<Expense> expenses) {
        pieChart.clear();
        pieChart.setUsePercentValues(true);
        pieChart.getDescription().setEnabled(false);
        pieChart.setExtraOffsets(5, 10, 5, 5);
        pieChart.setDragDecelerationFrictionCoef(0.95f);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(Color.parseColor("#121212"));
        pieChart.setTransparentCircleColor(Color.WHITE);
        pieChart.setTransparentCircleAlpha(110);
        pieChart.setHoleRadius(45f);
        pieChart.setTransparentCircleRadius(48f);
        pieChart.setCenterText("Expenses");
        pieChart.setCenterTextSize(18f);
        pieChart.setCenterTextColor(Color.WHITE);
        pieChart.getLegend().setEnabled(false);
        pieChart.setRotationAngle(0);
        pieChart.setRotationEnabled(true);
        pieChart.setHighlightPerTapEnabled(true);

        Map<String, Float> categoryTotals = new HashMap<>();
        for (Expense expense : expenses) {
            categoryTotals.merge(expense.getCategory(), (float) expense.getAmount(), Float::sum);
        }

        if (categoryTotals.isEmpty()) {
            categoryTotals.put("Housing", 1800f);
            categoryTotals.put("Groceries", 1000f);
            categoryTotals.put("Transportation", 800f);
            categoryTotals.put("Utilities", 500f);
            categoryTotals.put("Healthcare", 400f);
            categoryTotals.put("Dining Out", 600f);
            categoryTotals.put("Entertainment", 400f);
        }

        List<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Float> entry : categoryTotals.entrySet()) {
            entries.add(new PieEntry(entry.getValue(), entry.getKey()));
        }

        ArrayList<Integer> colors = new ArrayList<>();
        colors.add(Color.rgb(64, 89, 128));
        colors.add(Color.rgb(149, 165, 124));
        colors.add(Color.rgb(217, 184, 162));
        colors.add(Color.rgb(191, 134, 134));
        colors.add(Color.rgb(179, 48, 80));
        colors.add(Color.rgb(193, 37, 82));
        colors.add(Color.rgb(255, 102, 0));
        colors.add(Color.rgb(245, 199, 0));
        colors.add(Color.rgb(106, 150, 31));
        colors.add(Color.rgb(179, 100, 53));

        while (colors.size() < categoryTotals.size()) {
            int index = colors.size() % 10;
            colors.add(Color.rgb(
                    (colors.get(index) >> 16) & 0xff,
                    ((colors.get(index) >> 8) & 0xff + 20) % 256,
                    (colors.get(index) & 0xff + 40) % 256
            ));
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setSliceSpace(3f);
        dataSet.setIconsOffset(new MPPointF(0, 40));
        dataSet.setSelectionShift(5f);
        dataSet.setColors(colors);
        dataSet.setValueLinePart1OffsetPercentage(80.f);
        dataSet.setValueLinePart1Length(0.2f);
        dataSet.setValueLinePart2Length(0.4f);
        dataSet.setYValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);

        PieData data = new PieData(dataSet);
        data.setValueFormatter(new PercentFormatter(pieChart));
        data.setValueTextSize(14f);
        data.setValueTextColor(Color.WHITE);
        pieChart.setData(data);
        pieChart.setEntryLabelColor(Color.WHITE);
        pieChart.animateY(1400, Easing.EaseInOutQuad);

        List<CategoryLegendAdapter.CategoryLegendItem> legendItems = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) {
            legendItems.add(new CategoryLegendAdapter.CategoryLegendItem(
                    entries.get(i).getLabel(),
                    colors.get(i % colors.size())
            ));
        }

        CategoryLegendAdapter legendAdapter = new CategoryLegendAdapter(legendItems);
        categoriesLegendRecyclerView.setAdapter(legendAdapter);

        pieChart.invalidate();
    }
}
