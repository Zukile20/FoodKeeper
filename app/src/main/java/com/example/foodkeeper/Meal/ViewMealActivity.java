package com.example.foodkeeper.Meal;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Base64;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.foodkeeper.FoodkeeperUtils.Database;
import com.example.foodkeeper.FoodItem.models.FoodItem;
import com.example.foodkeeper.R;

import java.io.File;
import java.util.ArrayList;

public class ViewMealActivity extends AppCompatActivity {

    Database db;
    ArrayList<FoodItem> foodItems;
    Meal meal;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_view_meal);
        db = Database.getInstance(this);
        getIntentData();
        initializeViews();
        setupAdapter();
        bindData();
        setupListeners();
    }
    private void getIntentData() {
        Intent intent = getIntent();
        if (intent.hasExtra("viewMeal")) {
            long mealID = intent.getLongExtra("viewMeal",0);
            meal = db.getMealWithFoodItems(mealID);
            loadItems(meal);
        }
    }

    public void loadItems(Meal meal)
    {
        foodItems  = (ArrayList<FoodItem>)db.getFoodItemsForMeal(meal.getMealID());
    }
    private ImageView mealImage;
    private TextView mealName,      lastUsed;
    private ListView listView;
    private Button backBtn;
    private void initializeViews()
    {

        mealImage =findViewById(R.id.mealImage);
        mealName = findViewById(R.id.mealNameText);
        listView = findViewById(R.id.customListView);
        lastUsed = findViewById(R.id.lastUsedView);
        backBtn = findViewById(R.id.backBtn);
    }
    private void setupListeners()
    {
        backBtn.setOnClickListener(v->
        {
            finish();
        });
    }


    private void setupAdapter()
    {
        ArrayAdapter<FoodItem> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                foodItems
        );
        listView.setAdapter(adapter);
    }
    @SuppressLint("SetTextI18n")
    private void bindData()
    {
        if(meal!=null) {
            mealName.setText(meal.getMealName());
            loadBase64ImageWithGlide(this, meal.getUri(),mealImage);
            if(meal.getLastUsed()!=null)
            {
                lastUsed.setText(lastUsed.getText()+meal.getLastUsed().toString());
            }
            else {
                lastUsed.setText(lastUsed.getText()+"Not yet used");
            }
        }
    }
    public void loadBase64ImageWithGlide(Context context, String base64String, ImageView imageView) {
        if (base64String == null || base64String.isEmpty()) {
            imageView.setImageResource(R.drawable.image_placeholder);
            return;
        }

        try {
            byte[] decodedBytes = Base64.decode(base64String, Base64.DEFAULT);

            Glide.with(context)
                    .load(decodedBytes)
                    .placeholder(R.drawable.image_placeholder)
                    .error(R.drawable.image_placeholder)
                    .centerCrop()
                    .into(imageView);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            imageView.setImageResource(R.drawable.image_placeholder);
        }
    }

}