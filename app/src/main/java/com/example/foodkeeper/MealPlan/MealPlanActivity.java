package com.example.foodkeeper.MealPlan;

import static android.view.View.GONE;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static com.example.foodkeeper.MealPlan.CalendarUtils.selectedDate;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.foodkeeper.Database;
import com.example.foodkeeper.Meal.Meal;
import com.example.foodkeeper.R;
import com.example.foodkeeper.SessionManager;
import com.example.foodkeeper.ViewMeals.mealsViewActivity;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

public class MealPlanActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_meal_plan);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();//hide the app title
        }
        initializeLayouts();
        initializeViews();
        initializeLayouts();
        setUpListeners();

    }
    Database db ;
    SessionManager sess;
    Button cancelBtn,saveBtn,breakfastBtn,lunchBtn,dinnerBtn,snackBtn;
    TextView dayText;
    ActivityResultLauncher<Intent> launcher ;
    private void initializeViews() {
        db = Database.getInstance(this);
        sess = new SessionManager(this);
        dayText = findViewById(R.id.selected_day_text);
        cancelBtn = findViewById(R.id.cancelBtn);
        saveBtn = findViewById(R.id.saveBtn);
        breakfastBtn = findViewById(R.id.breakFastBtn);
        lunchBtn = findViewById(R.id.lunchBtn);
        dinnerBtn = findViewById(R.id.DinnerBtn);
        snackBtn = findViewById(R.id.snackBtn);

    }

    private void setUpListeners() {
        setupresultslauncher();
        cancelBtn.setOnClickListener(v -> {
            db.deleteMealplan(selectedDate);

            setResult(RESULT_CANCELED);
            finish();})
        ;
        saveBtn.setOnClickListener(V -> {
            setResult(RESULT_OK);
            finish();
        });


        breakfastBtn.setOnClickListener(V ->
        {
          Intent requestIntent = new Intent(this, mealsViewActivity.class);
          requestIntent.putExtra("mealType","Breakfast");
          requestIntent.putExtra("ACTIVITY_MODE",1);
          launcher.launch(requestIntent);
            finish();
        });

        lunchBtn.setOnClickListener(V ->
        {
          Intent requestIntent = new Intent(this, mealsViewActivity.class);
          requestIntent.putExtra("mealType","Lunch");
            requestIntent.putExtra("ACTIVITY_MODE",1);
            launcher.launch(requestIntent);
            finish();
        });

        dinnerBtn.setOnClickListener(V ->
        {
          Intent requestIntent = new Intent(this, mealsViewActivity.class);
          requestIntent.putExtra("mealType","Dinner");
            requestIntent.putExtra("ACTIVITY_MODE",1);
            launcher.launch(requestIntent);
            finish();
        });

        snackBtn.setOnClickListener(V ->
        {
          Intent requestIntent = new Intent(this, mealsViewActivity.class);
          requestIntent.putExtra("mealType","Snack");
            requestIntent.putExtra("ACTIVITY_MODE",1);
            launcher.launch(requestIntent);
            finish();
        });

        updateSelectedDayText();
    }
    private void setupresultslauncher()
    {
        launcher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),result->
                {
                    if(result.getResultCode()==RESULT_OK)
                    {

                        if(result.getData()!=null)
                        {

                            Intent data = result.getData();
                            String mealType = data.getStringExtra("mealType");
                            long mealID = data.getLongExtra("selectedMeal", 0);
                            long fridgeId = db.getConnectedFridgeForUser(sess.getUserEmail()).getId();

                            Meal meal = db.getMealWithFoodItems(mealID);
                            meal.setLastUsed(selectedDate);
                            db.updateMeal(meal);//updates the last used date for the meal
                            switch (Objects.requireNonNull(mealType))
                            {
                                case "Breakfast" :
                                {
                                    try {
                                        db.addMealToPlan(meal.getMealID(),CalendarUtils.selectedDate,mealType,fridgeId);
                                        showMealLayout(breakfastContainer,meal,breakFastLayout);
                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    }
                                    break;
                                }
                                case "Lunch" :
                                {
                                    db.addMealToPlan(meal.getMealID(), CalendarUtils.selectedDate, mealType,fridgeId);
                                    try {
                                        showMealLayout(lunchContainer, meal,lunchLayout);

                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    }
                                    break;
                                }
                                case "Dinner" : {
                                    db.addMealToPlan(meal.getMealID(), CalendarUtils.selectedDate, mealType,fridgeId);
                                    try {
                                        showMealLayout(dinnerContainer, meal,dinnerLayout);

                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    }
                                    break;
                                }
                                case "Snack" : {
                                    db.addMealToPlan(meal.getMealID(), CalendarUtils.selectedDate, mealType,fridgeId);
                                    try {
                                        showMealLayout(snackContainer, meal,snackLayout);
                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    }
                                    break;
                                }
                            }
                        }
                    }
                }
        );
    }
    private LinearLayout breakfastContainer;
    private LinearLayout lunchContainer;
    private LinearLayout dinnerContainer;
    private LinearLayout snackContainer;

    private void initializeLayouts()
    {
        breakfastContainer = findViewById(R.id.breakFastContainer);
        lunchContainer =findViewById(R.id.lunchContainer);
        dinnerContainer =findViewById(R.id.dinnerContainer);
        snackContainer =findViewById(R.id.snackContainer);

        breakFastLayout =findViewById(R.id.breakFastLayout);
        lunchLayout = findViewById(R.id.lunchLayout);
        dinnerLayout = findViewById(R.id.dinnerLayout);
        snackLayout =findViewById(R.id.snackLayout);
    }
    private void updateSelectedDayText()
    {
        String formatted = CalendarUtils.formattedDayText(selectedDate);
        dayText.setText(formatted);
    }
    private void showMealLayout(ViewGroup container, Meal meal,ViewGroup placeholder) throws IOException {
        if(meal!=null) {
            placeholder.setVisibility(GONE);
            container.setVisibility(VISIBLE);
            container.setMinimumHeight(50);
            View mealView = LayoutInflater.from(this).inflate(R.layout.meal_event_cell, container, false);

            TextView mealName = mealView.findViewById(R.id.eventCellTV);
            ImageView mealImage = mealView.findViewById(R.id.mealImageView);
            ImageView optButton = mealView.findViewById(R.id.editMealBtn);
            optButton.setVisibility(INVISIBLE);
            container.setClickable(false);

            mealName.setText(meal.getMealName());
            if (meal.getUri() != null) {//since a meal might not have an image
                File imageUri = new File(meal.getUri());
                Glide.with(this)
                        .load(imageUri)
                        .into(mealImage);
            } else {
                mealImage.setImageResource(R.drawable.place_holder);
            }
            mealView.setClickable(false);
            container.addView(mealView);
        }

    }
    private FrameLayout breakFastLayout,lunchLayout,dinnerLayout,snackLayout;


}