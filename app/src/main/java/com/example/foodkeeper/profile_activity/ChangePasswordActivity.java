package com.example.foodkeeper.profile_activity;

import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.foodkeeper.FoodkeeperUtils.Database;
import com.example.foodkeeper.R;
import com.example.foodkeeper.Fridge.models.User;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class ChangePasswordActivity extends AppCompatActivity {

    private TextInputEditText etCurrentPassword, etNewPassword, etConfirmPassword;
    private TextInputLayout tilCurrentPassword, tilNewPassword, tilConfirmPassword;
    private Button btnUpdate, btnCancel;
    private Database dbHelper;
    private String userEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_change_password);

        etCurrentPassword = findViewById(R.id.etCurrentPassword);
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);

        tilCurrentPassword = findViewById(R.id.tilCurrentPassword);
        tilNewPassword = findViewById(R.id.tilNewPassword);
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword);

        btnUpdate = findViewById(R.id.btnUpdate);
        btnCancel = findViewById(R.id.btnCancel);

        dbHelper = new Database(this);
        userEmail = getIntent().getStringExtra("USER_EMAIL");

        if (userEmail == null || userEmail.isEmpty()) {
            Toast.makeText(this, "Error: User not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        btnUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changePassword();
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void clearAllErrors() {
        tilCurrentPassword.setError(null);
        tilNewPassword.setError(null);
        tilConfirmPassword.setError(null);
    }

    private void changePassword() {
        String currentPassword = etCurrentPassword.getText().toString().trim();
        String newPassword = etNewPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        clearAllErrors();

        if (currentPassword.isEmpty()) {
            tilCurrentPassword.setError("Please enter current password");
            etCurrentPassword.requestFocus();
            return;
        }

        if (newPassword.isEmpty()) {
            tilNewPassword.setError("Please enter new password");
            etNewPassword.requestFocus();
            return;
        }

        if (confirmPassword.isEmpty()) {
            tilConfirmPassword.setError("Please confirm new password");
            etConfirmPassword.requestFocus();
            return;
        }

        User user = dbHelper.loadUserByEmail(userEmail);
        if (user == null || !user.getPassword().equals(currentPassword)) {
            tilCurrentPassword.setError("Current password is incorrect");
            etCurrentPassword.requestFocus();
            return;
        }

        if (!isValidPassword(newPassword)) {
            tilNewPassword.setError("Password must meet all requirements");
            etNewPassword.requestFocus();
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            tilConfirmPassword.setError("Passwords do not match");
            etConfirmPassword.requestFocus();
            return;
        }

        if (currentPassword.equals(newPassword)) {
            tilNewPassword.setError("New password must be different");
            etNewPassword.requestFocus();
            return;
        }

        int isUpdated = dbHelper.updatePassword(userEmail, newPassword);

        if (isUpdated != -1) {
            Toast.makeText(this, "Password changed successfully", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "Failed to change password", Toast.LENGTH_SHORT).show();
        }
    }

    public static boolean isValidPassword(String password) {
        int f1 = 0, f2 = 0, f3 = 0;
        if(password.length() < 8){
            return false;
        } else {
            for (int p = 0; p < password.length(); p++){
                if(Character.isLetter(password.charAt(p))){
                    f1 = 1;
                }
            }
            for (int r = 0; r < password.length(); r++){
                if(Character.isDigit(password.charAt(r))){
                    f2 = 1;
                }
            }
            for (int s = 0; s < password.length(); s++){
                char c = password.charAt(s);
                if(c >= 33 && c <= 46 || c == 64){
                    f3 = 1;
                }
            }
            if (f1 == 1 && f2 == 1 && f3 == 1) {
                return true;
            }
            return false;
        }
    }
}