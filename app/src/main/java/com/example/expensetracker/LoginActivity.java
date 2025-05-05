package com.example.expensetracker;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    private EditText usernameInput;
    private EditText passwordInput;
    private Button loginButton;
    private TextView registerLink;
    private ExpenseDBHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Check if user is already logged in
        SharedPreferences prefs = getSharedPreferences("expense_tracker", MODE_PRIVATE);
        if (prefs.getInt("current_user_id", -1) != -1) {
            // User is already logged in, go to MainActivity
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
            return;
        }

        dbHelper = new ExpenseDBHelper(this);

        usernameInput = findViewById(R.id.usernameInput);
        passwordInput = findViewById(R.id.passwordInput);
        loginButton = findViewById(R.id.loginButton);
        registerLink = findViewById(R.id.registerLink);

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = usernameInput.getText().toString().trim();
                String password = passwordInput.getText().toString().trim();

                if (username.isEmpty() || password.isEmpty()) {
                    Toast.makeText(LoginActivity.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                    return;
                }

                User user = dbHelper.loginUser(username, password);

                if (user != null) {
                    // Save user session
                    SharedPreferences.Editor editor = getSharedPreferences("expense_tracker", MODE_PRIVATE).edit();
                    editor.putInt("current_user_id", user.getId());
                    editor.putString("current_username", user.getUsername());
                    editor.apply();

                    // Create default financial values if this is the first login for the user
                    initializeUserFinancialInfoIfNeeded(user.getId());

                    // Navigate to main activity
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(LoginActivity.this, "Invalid username or password", Toast.LENGTH_SHORT).show();
                }
            }
        });

        registerLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });
    }

    // Initialize default financial values for new users
    private void initializeUserFinancialInfoIfNeeded(int userId) {
        SharedPreferences prefs = getSharedPreferences("expense_tracker_user_" + userId, MODE_PRIVATE);
        if (!prefs.contains("income")) {
            // This appears to be the user's first login, set default values
            SharedPreferences.Editor editor = prefs.edit();
            editor.putFloat("income", 60000.0f);
            editor.putFloat("balance", 3200.0f);
            editor.putFloat("goal", 13000.0f);
            editor.apply();
        }
    }
}