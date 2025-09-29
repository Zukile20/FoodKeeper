package com.example.foodkeeper.Meal;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.example.foodkeeper.Database;
import com.example.foodkeeper.FoodItem.FoodItem;
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
            loadMealImage(mealImage, meal);
            if(meal.getLastUsed()!=null)
            {
                lastUsed.setText(lastUsed.getText()+meal.getLastUsed().toString());
            }
            else {
                lastUsed.setText(lastUsed.getText()+"Not yet used");
            }
        }
    }
    private void loadMealImage(ImageView imageView, Meal meal) {
        String imagePath = meal.getUri();

        Log.d("MealImage", "Loading image from path: " + imagePath);

        if (imagePath != null && !imagePath.isEmpty()) {
            File imageFile = new File(imagePath);

            // Check if file exists
            if (!imageFile.exists()) {
                Log.e("MealImage", "File does not exist: " + imagePath);
                imageView.setImageResource(R.drawable.place_holder);
                return;
            }

            Log.d("MealImage", "File exists, size: " + imageFile.length() + " bytes");

            Glide.with(this)
                    .load(imageFile)
                    .placeholder(R.drawable.place_holder)
                    .error(R.drawable.place_holder)
                    .fallback(R.drawable.place_holder)
                    .override(64, 64)
                    .centerCrop()
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                            Log.e("ImageLoad", "Failed to load image from: " + imagePath);
                            if (e != null) {
                                Log.e("ImageLoad", "Glide error details: " + e.getMessage());
                                e.printStackTrace();
                            }
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                            Log.d("ImageLoad", "Image loaded successfully from: " + imagePath);
                            return false;
                        }
                    })
                    .into(imageView);
        } else {
            Log.d("MealImage", "No image path provided, using placeholder");
            imageView.setImageResource(R.drawable.place_holder);
        }
    }

}