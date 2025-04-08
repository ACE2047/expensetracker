package com.example.expensetracker;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public class ExpenseDBHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "expense_tracker.db";
    private static final int DATABASE_VERSION = 2;

    // Expenses table
    private static final String TABLE_EXPENSES = "expenses";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_AMOUNT = "amount";
    private static final String COLUMN_CATEGORY = "category";
    private static final String COLUMN_DESCRIPTION = "description";
    private static final String COLUMN_DATE = "date";
    private static final String COLUMN_USER_ID = "user_id";

    // Users table
    private static final String TABLE_USERS = "users";
    private static final String COLUMN_USER_USERNAME = "username";
    private static final String COLUMN_USER_EMAIL = "email";
    private static final String COLUMN_USER_PASSWORD = "password_hash";

    public ExpenseDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create users table
        String CREATE_USERS_TABLE = "CREATE TABLE " + TABLE_USERS + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_USER_USERNAME + " TEXT UNIQUE,"
                + COLUMN_USER_EMAIL + " TEXT UNIQUE,"
                + COLUMN_USER_PASSWORD + " TEXT"
                + ")";
        db.execSQL(CREATE_USERS_TABLE);

        // Create expenses table with user_id foreign key
        String CREATE_EXPENSES_TABLE = "CREATE TABLE " + TABLE_EXPENSES + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_AMOUNT + " REAL,"
                + COLUMN_CATEGORY + " TEXT,"
                + COLUMN_DESCRIPTION + " TEXT,"
                + COLUMN_DATE + " DATETIME DEFAULT CURRENT_TIMESTAMP,"
                + COLUMN_USER_ID + " INTEGER,"
                + "FOREIGN KEY(" + COLUMN_USER_ID + ") REFERENCES " + TABLE_USERS + "(" + COLUMN_ID + ")"
                + ")";
        db.execSQL(CREATE_EXPENSES_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Handle database upgrades
        if (oldVersion < 2) {
            // If upgrading from version 1, add the users table and update expenses table
            db.execSQL("CREATE TABLE " + TABLE_USERS + "("
                    + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + COLUMN_USER_USERNAME + " TEXT UNIQUE,"
                    + COLUMN_USER_EMAIL + " TEXT UNIQUE,"
                    + COLUMN_USER_PASSWORD + " TEXT"
                    + ")");

            // Add user_id column to expenses table
            db.execSQL("ALTER TABLE " + TABLE_EXPENSES + " ADD COLUMN " + COLUMN_USER_ID + " INTEGER");
        }
    }

    // User authentication methods

    public long registerUser(String username, String email, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(COLUMN_USER_USERNAME, username);
        values.put(COLUMN_USER_EMAIL, email);
        values.put(COLUMN_USER_PASSWORD, hashPassword(password));

        // Insert the new row, returning the primary key value of the new row
        long userId = db.insert(TABLE_USERS, null, values);
        db.close();
        return userId;
    }

    public User loginUser(String username, String password) {
        SQLiteDatabase db = this.getReadableDatabase();

        String[] columns = {
                COLUMN_ID,
                COLUMN_USER_USERNAME,
                COLUMN_USER_EMAIL,
                COLUMN_USER_PASSWORD
        };

        String selection = COLUMN_USER_USERNAME + " = ?";
        String[] selectionArgs = {username};

        Cursor cursor = db.query(TABLE_USERS, columns, selection, selectionArgs, null, null, null);

        User user = null;

        if (cursor.moveToFirst()) {
            String storedPasswordHash = cursor.getString(3);

            if (verifyPassword(password, storedPasswordHash)) {
                user = new User(
                        cursor.getInt(0),
                        cursor.getString(1),
                        cursor.getString(2),
                        storedPasswordHash
                );
            }
        }

        cursor.close();
        db.close();
        return user;
    }

    // Simple password hashing using SHA-256
    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes());

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            Log.e("ExpenseDBHelper", "Error hashing password", e);
            return null;
        }
    }

    private boolean verifyPassword(String password, String storedHash) {
        String inputHash = hashPassword(password);
        return inputHash != null && inputHash.equals(storedHash);
    }

    // Expense methods updated to include user ID

    public void addExpense(Expense expense, int userId) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COLUMN_AMOUNT, expense.getAmount());
        values.put(COLUMN_CATEGORY, expense.getCategory());
        values.put(COLUMN_DESCRIPTION, expense.getDescription());
        values.put(COLUMN_USER_ID, userId);

        db.insert(TABLE_EXPENSES, null, values);
        db.close();
    }

    public List<Expense> getAllExpenses(int userId) {
        List<Expense> expenses = new ArrayList<>();

        String selectQuery = "SELECT * FROM " + TABLE_EXPENSES +
                " WHERE " + COLUMN_USER_ID + " = ?" +
                " ORDER BY " + COLUMN_DATE + " DESC";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, new String[]{String.valueOf(userId)});

        if (cursor.moveToFirst()) {
            do {
                Expense expense = new Expense(
                        cursor.getInt(0),
                        cursor.getDouble(1),
                        cursor.getString(2),
                        cursor.getString(3)
                );
                expenses.add(expense);
            } while (cursor.moveToNext());
        }

        cursor.close();
        return expenses;
    }

    public double getTotalExpenses(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT SUM(" + COLUMN_AMOUNT + ") FROM " + TABLE_EXPENSES +
                        " WHERE " + COLUMN_USER_ID + " = ?",
                new String[]{String.valueOf(userId)});

        double total = 0;
        if (cursor.moveToFirst()) {
            total = cursor.getDouble(0);
        }

        cursor.close();
        return total;
    }

    // Delete expense by ID
    public boolean deleteExpense(int expenseId, int userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLE_EXPENSES,
                COLUMN_ID + " = ? AND " + COLUMN_USER_ID + " = ?",
                new String[]{String.valueOf(expenseId), String.valueOf(userId)}) > 0;
    }
}