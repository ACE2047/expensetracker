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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private TextView totalExpenseText, welcomeText;
    private AnimatedPieChartView pieChart;
    private RecyclerView topExpensesRecyclerView;
    private RecyclerView categoriesLegendRecyclerView;
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

        // Check if user is logged in
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

        // Initialize views
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
        categoriesLegendRecyclerView = findViewById(R.id.categoriesLegendRecyclerView);
        categoriesLegendRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        welcomeText.setText("Welcome, " + currentUsername + "!");

        topExpensesRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Set up logout button
        logoutButton.setOnClickListener(v -> logout());

        // Load user financial info
        loadUserFinancialInfo();
        loadData();

        // Set up button listeners
        addExpenseButton.setOnClickListener(v -> showAddExpenseDialog());
        setBudgetButton.setOnClickListener(v -> showSetBudgetDialog());

        viewAllText.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ExpenseHistoryActivity.class);
            intent.putExtra("user_id", currentUserId);
            startActivityForResult(intent, EXPENSE_HISTORY_REQUEST_CODE);
        });
    }

    // Load user-specific financial information
    private void loadUserFinancialInfo() {
        SharedPreferences prefs = getSharedPreferences("expense_tracker_user_" + currentUserId, MODE_PRIVATE);
        totalIncome = prefs.getFloat("income", 60000.0f);
        totalBalance = prefs.getFloat("balance", 3200.0f);
        totalGoal = prefs.getFloat("goal", 13000.0f);

        // Update UI with the loaded values
        incomeText.setText("Income: $" + String.format("%.2f", totalIncome));
        balanceText.setText("Balance: $" + String.format("%.2f", totalBalance));
        goalText.setText("Goal: $" + String.format("%.2f", totalGoal));
    }

    // Save user-specific financial information
    private void saveUserFinancialInfo() {
        // Read current values from UI instead of using class variables
        try {
            String incomeStr = incomeText.getText().toString().replace("Income: $", "").trim();
            String balanceStr = balanceText.getText().toString().replace("Balance: $", "").trim();
            String goalStr = goalText.getText().toString().replace("Goal: $", "").trim();

            totalIncome = Double.parseDouble(incomeStr);
            totalBalance = Double.parseDouble(balanceStr);
            totalGoal = Double.parseDouble(goalStr);
        } catch (Exception e) {
            // In case of parsing errors, use the class variables
        }

        // Save to user-specific SharedPreferences
        SharedPreferences.Editor editor = getSharedPreferences("expense_tracker_user_" + currentUserId, MODE_PRIVATE).edit();
        editor.putFloat("income", (float) totalIncome);
        editor.putFloat("balance", (float) totalBalance);
        editor.putFloat("goal", (float) totalGoal);
        editor.apply();
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

    // Called when the app is being closed or put in background
    @Override
    protected void onPause() {
        super.onPause();
        // Save financial info when the app is paused
        saveUserFinancialInfo();
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Also save when the app is stopped
        saveUserFinancialInfo();
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

        // Set up simplified pie chart representation
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

                            // Update UI
                            incomeText.setText("Income: $" + String.format("%.2f", totalIncome));
                            balanceText.setText("Balance: $" + String.format("%.2f", totalBalance));
                            goalText.setText("Goal: $" + String.format("%.2f", totalGoal));

                            // Save to user-specific SharedPreferences immediately
                            saveUserFinancialInfo();

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
        // Force save the user's financial info before logging out
        saveUserFinancialInfo();

        // Double-check save was successful by verifying the SharedPreferences
        SharedPreferences prefs = getSharedPreferences("expense_tracker_user_" + currentUserId, MODE_PRIVATE);
        float savedIncome = prefs.getFloat("income", -1f);

        // If save failed, try once more
        if (savedIncome < 0) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putFloat("income", (float) totalIncome);
            editor.putFloat("balance", (float) totalBalance);
            editor.putFloat("goal", (float) totalGoal);
            editor.apply();
        }

        // Clear current user session
        SharedPreferences.Editor editor = getSharedPreferences("expense_tracker", MODE_PRIVATE).edit();
        editor.remove("current_user_id");
        editor.remove("current_username");
        editor.apply();

        startActivity(new Intent(MainActivity.this, LoginActivity.class));
        finish();
    }

    private void setupPieChart(List<Expense> expenses) {
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

        // Create colors for the chart
        ArrayList<Integer> colors = new ArrayList<>();
        colors.add(Color.rgb(255, 102, 0));    // Housing - Orange
        colors.add(Color.rgb(149, 165, 124));  // Groceries - Sage Green
        colors.add(Color.rgb(191, 134, 134));  // Transportation - Dusty Rose
        colors.add(Color.rgb(64, 89, 128));    // Utilities - Navy Blue
        colors.add(Color.rgb(179, 48, 80));    // Healthcare - Deep Red
        colors.add(Color.rgb(106, 150, 31));   // Dining Out - Green
        colors.add(Color.rgb(217, 184, 162));  // Entertainment - Light Beige
        colors.add(Color.rgb(245, 199, 0));    // Yellow
        colors.add(Color.rgb(193, 37, 82));    // Raspberry
        colors.add(Color.rgb(179, 100, 53));   // Brown

        while (colors.size() < categoryTotals.size()) {
            int index = colors.size() % 10;
            colors.add(Color.rgb(
                    (colors.get(index) >> 16) & 0xff,
                    ((colors.get(index) >> 8) & 0xff + 20) % 256,
                    (colors.get(index) & 0xff + 40) % 256
            ));
        }

        // Create list of pie slices for the chart
        ArrayList<String> categories = new ArrayList<>(categoryTotals.keySet());
        List<AnimatedPieChartView.PieSlice> pieSlices = new ArrayList<>();
        List<CategoryLegendAdapter.CategoryLegendItem> legendItems = new ArrayList<>();

        // Calculate total for percentage calculation
        float totalAmount = 0f;
        for (String category : categories) {
            totalAmount += categoryTotals.get(category);
        }

        for (int i = 0; i < categories.size(); i++) {
            String category = categories.get(i);
            float value = categoryTotals.get(category);
            int color = colors.get(i % colors.size());

            // Calculate percentage for display in legend
            float percentage = (value / totalAmount) * 100;
            String displayName = category + " (" + String.format("%.0f", percentage) + "%)";

            pieSlices.add(new AnimatedPieChartView.PieSlice(
                    category,
                    value,
                    color
            ));

            legendItems.add(new CategoryLegendAdapter.CategoryLegendItem(
                    displayName,
                    color
            ));
        }

        // Set the data for the pie chart
        pieChart.setSlices(pieSlices);

        // Set up the legend adapter
        CategoryLegendAdapter legendAdapter = new CategoryLegendAdapter(legendItems);
        categoriesLegendRecyclerView.setAdapter(legendAdapter);
    }
}