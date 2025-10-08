package com.example.foodkeeper;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.foodkeeper.FoodItem.ItemsViewActivity;
import com.example.foodkeeper.Meal.Meal;
import com.example.foodkeeper.MealPlan.MealPlan;
import com.example.foodkeeper.MealPlan.WeeklyViewActivity;
import com.example.foodkeeper.Recipe.Listeners.RecipeClickListerner;
import com.example.foodkeeper.Recipe.Models.Recipe;
import com.example.foodkeeper.Recipe.RecipeActivity;
import com.example.foodkeeper.Recipe.RecipeDetailsActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;


public class LandingPageActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;
    private RecyclerView recipesView;
    TextView greetText;
    private RecyclerView mealsView;
    private RecipeAdapter recipeAdapter;
    private MealAdapterLanding adapter;
    private Database database;
    private SessionManager sess;
    ActivityResultLauncher<Intent> launcher;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);


        // Initialize database
        database = new Database(this);
        sess= new SessionManager(this);

        // Initialize RecyclerViews
        recipesView = findViewById(R.id.recipesView);
        mealsView = findViewById(R.id.mealsView);
        mealsView = findViewById(R.id.mealsView);
        greetText = findViewById(R.id.greetText);

        // Setup Recipes RecyclerView
        setupRecipesRecyclerView();
        setupMealAdapter();


        bottomNav = findViewById(R.id.bottomNav);

        bottomNav.setOnNavigationItemSelectedListener(item ->{
            int id = item.getItemId();

            if(id == R.id.nav_home){

                return true;
            } else if(id == R.id.nav_search){
                startActivity(new Intent(LandingPageActivity.this, SearchActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            }else if(id == R.id.nav_view){
                startActivity(new Intent(LandingPageActivity.this, ItemsViewActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if(id == R.id.nav_expiring) {
                startActivity(new Intent(LandingPageActivity.this, ExpiringActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_profileMenu) {
                startActivity(new Intent(LandingPageActivity.this, MenuActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else {
                return false;
            }

        });
        bottomNav.setSelectedItemId(R.id.nav_home);
        refreshgreetTxt();
    }
    private void refreshgreetTxt() {

        greetText.setText("Hi, "+ sess.getUserName());
    }
    private void setupRecipesRecyclerView() {
        // Get random recipes from database (e.g., 5 random recipes)
        List<Recipe> recipes = database.getRandomRecipes(5);

        // Create click listener
        RecipeClickListerner listener = new RecipeClickListerner() {
            @Override
            public void onRecipeClicked(String id) {
                // Handle recipe click - navigate to recipe detail
                Intent intent = new Intent(LandingPageActivity.this, RecipeDetailsActivity.class).putExtra("id", id);
                startActivity(intent);
            }
        };

        // Create and set adapter
        recipeAdapter = new RecipeAdapter(this, recipes, listener);

        recipesView.setLayoutManager(new LinearLayoutManager(
                this,
                LinearLayoutManager.HORIZONTAL,
                false
        ));
        recipesView.setAdapter(recipeAdapter);
    }
    public void showProfileActivity(View view) {
        Intent intent = new Intent(this, ProfileActivity.class);
        startActivity(intent);
    }
    private void setupMealAdapter() {
        MealPlan plan = database.getMealPlanForDay(LocalDate.now());
        List<Meal> meals = new ArrayList<>();

        //load the meals in the meal plan
        if(plan!=null) {

            if (plan.getBreakFast() != null) {
                Meal meal =database.getMealWithFoodItems(plan.getBreakFast());
                meal.setMealType("Breakfast");
                meals.add(meal);
            }
            if (plan.getLunch() != null) {
                Meal meal =database.getMealWithFoodItems(plan.getLunch());
                meal.setMealType("Lunch");
                meals.add(meal);
            }
            if (plan.getDinner() != null) {
                Meal meal =database.getMealWithFoodItems(plan.getDinner());
                meal.setMealType("Dinner");
                meals.add(meal);
            }
            if (plan.getSnack() != null) {
                Meal meal =database.getMealWithFoodItems(plan.getSnack());
                meal.setMealType("Snack");
                meals.add(meal);
            }
        }
        LinearLayout mealsEmptyState = findViewById(R.id.mealsEmptyState);
        if (meals.isEmpty()) {
            mealsView.setVisibility(View.GONE);
            mealsEmptyState.setVisibility(View.VISIBLE);
        } else {
            mealsView.setVisibility(View.VISIBLE);
            mealsEmptyState.setVisibility(View.GONE);
        }
        Button addMealPlanButton = findViewById(R.id.addMealPlanButton);
        addMealPlanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LandingPageActivity.this, WeeklyViewActivity.class);
                startActivity(intent);
            }
        });
        adapter = new MealAdapterLanding(this, meals, new MealAdapterLanding.OnClickListener() {
            @Override
            public void onClick(Meal meal, int position) {
                Intent intent = new Intent(LandingPageActivity.this, WeeklyViewActivity.class);
                startActivity(intent);
            }
        });
        mealsView.setAdapter(adapter);
    }
    @Override
    protected void onResume() {
        super.onResume();
        // Refresh recipes when returning to this activity
        refreshRecipes();
        refreshMealPlan();
        refreshgreetTxt();

    }
    private void refreshRecipes() {
        // Get new random recipes
        List<Recipe> newRecipes = database.getRandomRecipes(5);
        if (recipeAdapter != null) {
            recipeAdapter.updateRecipes(newRecipes);
        }
    }
    private void refreshMealPlan() {
        // Get new random recipes
        setupMealAdapter();
    }
    public void showAllRecipes(View v)
    {
        startActivity(new Intent(this,RecipeActivity.class));
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Close database connection if needed
        if (database != null) {
            database.close();
        }
    }

}