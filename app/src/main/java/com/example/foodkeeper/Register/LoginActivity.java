package com.example.foodkeeper.Register;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.foodkeeper.FoodItem.view_items.ItemsViewActivity;
import com.example.foodkeeper.FoodkeeperUtils.Database;
import com.example.foodkeeper.LandingPage.LandingPageActivity;
import com.example.foodkeeper.R;
import com.example.foodkeeper.Recipe.Listeners.RandomRecipeResponseListener;
import com.example.foodkeeper.Recipe.Models.RandomRecipeApiResponse;
import com.example.foodkeeper.Recipe.RequestManager;
import com.example.foodkeeper.User;

public class LoginActivity extends AppCompatActivity {
    EditText edEmail, edPassword;
    Button btn;
    TextView tv;
    Database db;
    User user;
    String userEmail;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        SQLiteDatabase database = openOrCreateDatabase("FoodKeeper.db", MODE_PRIVATE, null);
        database.execSQL("CREATE TABLE IF NOT EXISTS sample (id INTEGER PRIMARY KEY, name TEXT)");
        database.close();

        edEmail = findViewById(R.id.email);
        edPassword = findViewById(R.id.passwordField);
        btn = findViewById(R.id.loginBtn);
        tv = findViewById(R.id.txtSignUp);

        db = new Database(this);
        SessionManager sess = new SessionManager(this);
        if (sess.isLoggedIn()) {


            if (isEmailExists(sess.getUserEmail())) {
                startActivity(new Intent(this, LandingPageActivity.class));
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
                        user = db.loadUserByEmail(email);
                         sess.createLoginSession(email,user.getName());
                         userEmail=email;
                         recipe();
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
    private  void  recipe() {

        RequestManager requestManager = new RequestManager(this);
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Setting up your recipes...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        requestManager.getRandomRecipesForUser(new RandomRecipeResponseListener() {
            @Override
            public void didFetch(RandomRecipeApiResponse response, String message) {
                progressDialog.dismiss();
                Toast.makeText(LoginActivity.this, "Recipes loaded!", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(LoginActivity.this, LandingPageActivity.class);
                startActivity(intent);
                finish();
            }
            @Override
            public void didError(String message) {
                progressDialog.dismiss();
                Toast.makeText(LoginActivity.this, "Error loading recipes: " + message, Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(LoginActivity.this, LandingPageActivity.class);
                startActivity(intent);
                finish();
            }
        }, null, userEmail);
    }
}
