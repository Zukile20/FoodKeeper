package com.example.foodkeeper;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.foodkeeper.FoodItem.ItemsViewActivity;

public class LoginActivity extends AppCompatActivity {
    EditText edEmail, edPassword;
    Button btn;
    TextView tv;
    Database db;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        edEmail = findViewById(R.id.email);
        edPassword = findViewById(R.id.passwordField);
        btn = findViewById(R.id.loginBtn);
        tv = findViewById(R.id.txtSignUp);

        db = new Database(this);
        SessionManager sess = new SessionManager(this);
        if (sess.isLoggedIn()) {


            if (isEmailExists(sess.getUserEmail())) {
                startActivity(new Intent(this, ItemsViewActivity.class));
                finish();
                return;
            } else {
                sess.logoutUser();
            }
        }
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = edEmail.getText().toString();
                String password = edPassword.getText().toString();


                if(email.length() == 0 || password.length() == 0){
                    Toast.makeText(getApplicationContext(), "Please fill in all the details", Toast.LENGTH_SHORT).show();
                } else {
                    if (db.login(email, password) == 1) {
                        Toast.makeText(getApplicationContext(), "Login Successfully", Toast.LENGTH_SHORT).show();
                         sess.createLoginSession(email, db.KEY_EMAIL);
                        startActivity(new Intent(LoginActivity.this, ItemsViewActivity.class));
                        finish();
                    } else {
                        Toast.makeText(getApplicationContext(), "Invalid Email Address and Password", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
        tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            }
        });
    }
    private void attemptLogin() {
        String email = edEmail.getText().toString().trim();
        String password = edPassword.getText().toString().trim();

        if (db.login(email, password) == 1) {
            SharedPreferences.Editor editor = getSharedPreferences("user_prefs", MODE_PRIVATE).edit();
            editor.putString("user_email", email);
            editor.apply();

            startActivity(new Intent(this, ItemsViewActivity.class));
            finish();
        } else {
            // Show error
        }
    }
    @Override
    protected void onDestroy() {
        db.close();
        super.onDestroy();
    }
    private boolean isEmailExists(String email) {
        SQLiteDatabase db = this.db.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM users WHERE email = ?", new String[]{email});
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }
}
