package com.example.foodkeeper.Meal;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.example.foodkeeper.FoodkeeperUtils.Database;
import com.example.foodkeeper.FoodItem.models.FoodItem;
import com.example.foodkeeper.R;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;

public class ViewMealActivity extends AppCompatActivity {

    private Database db;
    private ArrayList<FoodItem> foodItems;
    private Meal meal;
    private ArrayAdapter<FoodItem> adapter;
    private long mealID;

    private ImageView mealImage;
    private TextView mealName, lastUsed;
    private ListView listView;
    private Button backBtn, editBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_meal);

        db = Database.getInstance(this);

        if (!getIntentData()) {
            Toast.makeText(this, "Error loading meal", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        setupListeners();
        setupAdapter();
        bindData();
    }

    private boolean getIntentData() {
        Intent intent = getIntent();
        if (intent.hasExtra("viewMeal")) {
            mealID = intent.getLongExtra("viewMeal", 0);
            if (mealID > 0) {
                meal = db.getMealWithFoodItems(mealID);
                if (meal != null) {
                    loadItems(meal);
                    return true;
                }
            }
        }
        return false;
    }

    private void loadItems(Meal meal) {
        foodItems = (ArrayList<FoodItem>) db.getFoodItemsForMeal(meal.getMealID());
        if (foodItems == null) {
            foodItems = new ArrayList<>();
        }
    }

    private void initializeViews() {
        mealImage = findViewById(R.id.mealImage);
        mealName = findViewById(R.id.mealNameText);
        listView = findViewById(R.id.customListView);
        lastUsed = findViewById(R.id.lastUsedView);
        backBtn = findViewById(R.id.backBtn);
        editBtn = findViewById(R.id.editButton);
    }

    private void setupListeners() {
        backBtn.setOnClickListener(v -> finish());

        editBtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, UpdateMealActivity.class);
            intent.putExtra("EditMeal", meal.getMealID());
            startActivity(intent);
        });
    }

    private void setupAdapter() {
        adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                foodItems
        );
        listView.setAdapter(adapter);
    }

    @SuppressLint("SetTextI18n")
    private void bindData() {
        if (meal == null) {
            return;
        }

        mealName.setText(meal.getMealName());

        loadMealImage(meal.getUri(), mealImage);

        formatLastUsedDate();
    }

    private void formatLastUsedDate() {
        if (meal.getLastUsed() == null) {
            lastUsed.setText("Last used: Not yet used");
            return;
        }

        LocalDate lastUsedDate = meal.getLastUsed();
        LocalDate today = LocalDate.now();
        long daysBetween = ChronoUnit.DAYS.between(lastUsedDate, today);

        if (daysBetween == 0) {
            lastUsed.setText("Last used: Today");
        } else if (daysBetween == 1) {
            lastUsed.setText("Last used: Yesterday");
        } else if (daysBetween < 7) {
            lastUsed.setText("Last used: " + daysBetween + " days ago");
        } else if (daysBetween < 30) {
            long weeks = daysBetween / 7;
            lastUsed.setText("Last used: " + weeks + (weeks == 1 ? " week ago" : " weeks ago"));
        } else {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");
            lastUsed.setText("Last used: " + lastUsedDate.format(formatter));
        }
    }

    private void loadMealImage(String imagePath, ImageView imageView) {
        if (imagePath == null || imagePath.isEmpty()) {
            imageView.setImageResource(R.drawable.image_placeholder);
            return;
        }
        File imageFile = new File(imagePath);
        if (imageFile.exists()) {
            Glide.with(this)
                    .load(imageFile)
                    .placeholder(R.drawable.image_placeholder)
                    .error(R.drawable.image_placeholder)
                    .fallback(R.drawable.image_placeholder)
                    .centerCrop()
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model,
                                                    Target<Drawable> target, boolean isFirstResource) {
                            Log.e("ViewMeal", "Failed to load image: " + imagePath);
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model,
                                                       Target<Drawable> target, DataSource dataSource,
                                                       boolean isFirstResource) {
                            return false;
                        }
                    })
                    .into(imageView);
        } else {
            try {
                byte[] decodedBytes = android.util.Base64.decode(imagePath, android.util.Base64.DEFAULT);
                Glide.with(this)
                        .load(decodedBytes)
                        .placeholder(R.drawable.image_placeholder)
                        .error(R.drawable.image_placeholder)
                        .centerCrop()
                        .into(imageView);
            } catch (IllegalArgumentException e) {
                imageView.setImageResource(R.drawable.image_placeholder);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mealID > 0) {
            Meal updatedMeal = db.getMealWithFoodItems(mealID);

            if (updatedMeal == null) {
                Toast.makeText(this, "Meal has been deleted", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            meal = updatedMeal;

            ArrayList<FoodItem> newItems = (ArrayList<FoodItem>) db.getFoodItemsForMeal(meal.getMealID());
            if (newItems != null) {
                foodItems.clear();
                foodItems.addAll(newItems);
            }

            bindData();

            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
        }
    }
}