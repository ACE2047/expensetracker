package com.example.expensetracker;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ExpenseHistoryActivity extends AppCompatActivity implements ExpenseAdapter.OnExpenseDeleteListener {

    private RecyclerView expenseHistoryRecyclerView;
    private ExpenseAdapter expenseAdapter;
    private ExpenseDBHelper dbHelper;
    private TextView totalExpenseText;
    private int userId;
    private List<Expense> expenses;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expense_history);

        // Set up action bar with back button
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Expense History");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        // Initialize variables
        dbHelper = new ExpenseDBHelper(this);
        expenseHistoryRecyclerView = findViewById(R.id.expenseHistoryRecyclerView);
        totalExpenseText = findViewById(R.id.totalExpenseHistoryText);

        // Get the user ID from the intent
        userId = getIntent().getIntExtra("user_id", -1);
        if (userId == -1) {
            finish();
            return;
        }

        // Set up RecyclerView
        expenseHistoryRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Load expense data
        loadExpenseHistory();
    }

    private void loadExpenseHistory() {
        // Get all expenses for the user
        expenses = dbHelper.getAllExpenses(userId);

        // Calculate total expenses
        double totalExpense = 0;
        for (Expense expense : expenses) {
            totalExpense += expense.getAmount();
        }

        // Set the total expense text - Using $ instead of ₹
        totalExpenseText.setText("Total Expenses: $" + String.format("%.2f", totalExpense));

        // Set up the adapter with delete functionality
        expenseAdapter = new ExpenseAdapter(expenses, this);
        expenseHistoryRecyclerView.setAdapter(expenseAdapter);
    }

    // Add to ExpenseHistoryActivity.java
    @Override
    public boolean onSupportNavigateUp() {
        // Set result and finish
        setResult(RESULT_OK);
        finish();
        return true;
    }

    @Override
    public void onBackPressed() {
        // Set result and finish
        setResult(RESULT_OK);
        super.onBackPressed();
    }

    @Override
    public void onExpenseDelete(int expenseId, int position) {
        // Delete expense from database
        boolean success = dbHelper.deleteExpense(expenseId, userId);

        if (success) {
            // Remove from UI
            expenseAdapter.removeItem(position);

            // Recalculate total
            double totalExpense = 0;
            for (Expense expense : expenses) {
                totalExpense += expense.getAmount();
            }
            totalExpenseText.setText("Total Expenses: $" + String.format("%.2f", totalExpense));

            // Set result to notify MainActivity that data changed
            setResult(RESULT_OK);

            Toast.makeText(this, "Expense deleted successfully", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Failed to delete expense", Toast.LENGTH_SHORT).show();
        }
    }
}