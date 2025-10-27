package com.example.foodkeeper.LandingPage;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.foodkeeper.FoodkeeperUtils.Database;
import com.example.foodkeeper.FoodItem.expiring.ExpiringActivity;
import com.example.foodkeeper.FoodItem.view_items.ItemsViewActivity;
import com.example.foodkeeper.Meal.Meal;
import com.example.foodkeeper.MealPlan.MealPlan;
import com.example.foodkeeper.MealPlan.WeeklyViewActivity;
import com.example.foodkeeper.menu_page.MenuActivity;
import com.example.foodkeeper.profile_activity.ProfileActivity;
import com.example.foodkeeper.R;
import com.example.foodkeeper.Recipe.Listeners.RecipeClickListerner;
import com.example.foodkeeper.Recipe.Models.Recipe;
import com.example.foodkeeper.Recipe.RecipeActivity;
import com.example.foodkeeper.Recipe.RecipeDetailsActivity;
import com.example.foodkeeper.FoodItem.SearchActivity;
import com.example.foodkeeper.Register.SessionManager;
import com.example.foodkeeper.ShoppingList.MainShoppingListActivity;
import com.example.foodkeeper.Fridge.models.User;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;


public class LandingPageActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;
    private RecyclerView recipesView;
    TextView greetText;
    private RecyclerView mealsView;
    private ImageView profileImage;
    private RecipeAdapter recipeAdapter;
    private MealAdapterLanding adapter;
    private Database database;
    private SessionManager sess;
    User user;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);


        // Initialize database
        database = new Database(this);
        sess= new SessionManager(this);
        user= database.loadUserByEmail(sess.getUserEmail());

        handleNotificationIntent(getIntent());

        // Initialize RecyclerViews
        recipesView = findViewById(R.id.recipesView);
        mealsView = findViewById(R.id.mealsView);
        mealsView = findViewById(R.id.mealsView);
        greetText = findViewById(R.id.greetText);
        profileImage = findViewById(R.id.profileImageView);

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
        refreshProfileFoto();
    }

    private void refreshgreetTxt() {

        greetText.setText("Hi, "+ sess.getUserName());
    }
    private void setupRecipesRecyclerView() {
        List<Recipe> recipes = database.getRandomRecipes(5,sess.getUserEmail());

        RecipeClickListerner listener = new RecipeClickListerner() {
            @Override
            public void onRecipeClicked(String id) {
                Intent intent = new Intent(LandingPageActivity.this, RecipeDetailsActivity.class).putExtra("id", id);
                startActivity(intent);
            }
        };
        recipeAdapter = new RecipeAdapter(this, recipes, listener);

        recipesView.setLayoutManager(new LinearLayoutManager(
                this,
                LinearLayoutManager.HORIZONTAL,
                false
        ));
        recipesView.setAdapter(recipeAdapter);
    }
    public void showProfileActivity(View view)
    {
        Intent intent = new Intent(this, ProfileActivity.class);
        startActivity(intent);
    }

    private void setupMealAdapter()
    {
        MealPlan plan = database.getMealPlanForDay(LocalDate.now(),database.getConnectedFridgeForUser(sess.getUserEmail()).getId());
        List<Meal> meals = new ArrayList<>();
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
        refresh();
    }

    private void refresh() {
        user = database.loadUserByEmail(sess.getUserEmail());
        refreshRecipes();
        refreshMealPlan();
        refreshgreetTxt();
        refreshProfileFoto();
    }

    private void refreshProfileFoto()
    {
            Glide.with(this)
                    .load(user.getProfileImage())
                    .placeholder(R.drawable.ic_profile)
                    .error(R.drawable.ic_profile)
                    .into(profileImage);

    }

    private void refreshRecipes() {
        List<Recipe> newRecipes = database.getRandomRecipes(5,sess.getUserEmail());
        if (recipeAdapter != null) {
            recipeAdapter.updateRecipes(newRecipes);
        }
    }

    private void refreshMealPlan() {

        setupMealAdapter();
    }
    public void showAllRecipes(View v)
    {
        startActivity(new Intent(this,RecipeActivity.class));
    }
    public void WeeklyMealPlan(View v){
        startActivity(new Intent(this,WeeklyViewActivity.class));
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (database != null) {
            database.close();
        }
    }

    public void showAddToShoppingListDialog(String itemName) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.notification_shopping_list);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.setCancelable(true);

        Button btnCancel = dialog.findViewById(R.id.btn_cancel);
        Button btnAdd = dialog.findViewById(R.id.btn_add);
        TextView message = dialog.findViewById(R.id.dialog_message);
        String dialogPrompt = "Would you like to add '" + itemName + "' to your shopping list?";
        message.setText(dialogPrompt);

        btnCancel.setOnClickListener(v -> {
            dialog.dismiss();
        });

        btnAdd.setOnClickListener(v -> {
                Intent intent = new Intent(this, MainShoppingListActivity.class);
                intent.putExtra("notificationAdd",1);
            intent.putExtra("item_name", itemName);
            startActivity(intent);
            dialog.dismiss();
        });

        dialog.show();
    }
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleNotificationIntent(intent);
    }

    private void handleNotificationIntent(Intent intent) {
        if (intent != null && intent.hasExtra("show_shopping_dialog")) {
            boolean showDialog = intent.getBooleanExtra("show_shopping_dialog", false);
            if (showDialog) {
                String itemName = intent.getStringExtra("item_name");
                showAddToShoppingListDialog(itemName);
            }
        }
    }

}