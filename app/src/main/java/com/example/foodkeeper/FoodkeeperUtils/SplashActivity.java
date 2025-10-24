package com.example.foodkeeper.FoodkeeperUtils;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;

import com.example.foodkeeper.Register.LoginActivity;
import com.example.foodkeeper.R;

public class SplashActivity extends Activity {

    private TextView appNameText;
    private String appName = "FoodKeeper";
    private int index = 0;
    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        appNameText = findViewById(R.id.appNameText);
        animateText();
    }

    private void animateText() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (index < appName.length()) {
                    appNameText.setText(appNameText.getText().toString() + appName.charAt(index));
                    index++;
                    handler.postDelayed(this, 120); // delay between letters (ms)
                } else {
                    handler.postDelayed(() -> {
                        startActivity(new Intent(SplashActivity.this, LoginActivity.class));
                        finish();
                    }, 700);
                }
            }
        }, 120);
    }
}
