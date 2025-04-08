package com.example.expensetracker;

public class Expense {
    private int id;
    private double amount;
    private String category;
    private String description;

    public Expense(int id, double amount, String category, String description) {
        this.id = id;
        this.amount = amount;
        this.category = category;
        this.description = description;
    }

    public int getId() {
        return id;
    }

    public double getAmount() {
        return amount;
    }

    public String getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }
}