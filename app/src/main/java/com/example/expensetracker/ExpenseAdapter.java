package com.example.expensetracker;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ExpenseAdapter extends RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder> {

    private List<Expense> expenses;
    private OnExpenseDeleteListener deleteListener;

    // Interface for delete callbacks
    public interface OnExpenseDeleteListener {
        void onExpenseDelete(int expenseId, int position);
    }

    public ExpenseAdapter(List<Expense> expenses) {
        this.expenses = expenses;
    }

    public ExpenseAdapter(List<Expense> expenses, OnExpenseDeleteListener listener) {
        this.expenses = expenses;
        this.deleteListener = listener;
    }

    @NonNull
    @Override
    public ExpenseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.expense_item, parent, false);
        return new ExpenseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ExpenseViewHolder holder, int position) {
        Expense expense = expenses.get(position);
        holder.categoryTextView.setText(expense.getCategory());
        // Use $ symbol instead of ₹
        holder.amountTextView.setText("$" + String.format("%.2f", expense.getAmount()));
        holder.descriptionTextView.setText(expense.getDescription());

        // Set up delete button if listener exists
        if (deleteListener != null) {
            holder.deleteButton.setVisibility(View.VISIBLE);
            holder.deleteButton.setOnClickListener(v -> {
                int adapterPosition = holder.getAdapterPosition();
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    deleteListener.onExpenseDelete(expense.getId(), adapterPosition);
                }
            });
        } else {
            holder.deleteButton.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return expenses.size();
    }

    // Remove an item from the adapter
    public void removeItem(int position) {
        if (position >= 0 && position < expenses.size()) {
            expenses.remove(position);
            notifyItemRemoved(position);
        }
    }

    public static class ExpenseViewHolder extends RecyclerView.ViewHolder {
        TextView categoryTextView;
        TextView amountTextView;
        TextView descriptionTextView;
        ImageButton deleteButton;

        public ExpenseViewHolder(@NonNull View itemView) {
            super(itemView);
            categoryTextView = itemView.findViewById(R.id.categoryTextView);
            amountTextView = itemView.findViewById(R.id.amountTextView);
            descriptionTextView = itemView.findViewById(R.id.descriptionTextView);
            deleteButton = itemView.findViewById(R.id.deleteExpenseButton);
        }
    }
}