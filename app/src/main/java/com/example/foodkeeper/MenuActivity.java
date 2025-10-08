package com.example.foodkeeper;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.foodkeeper.FoodItem.ItemsViewActivity;
import com.example.foodkeeper.Fridge.ManageFridgeActivity;
import com.example.foodkeeper.MealPlan.WeeklyViewActivity;
import com.example.foodkeeper.Recipe.RecipeActivity;
import com.example.foodkeeper.ShoppingList.MainShoppingListActivity;
import com.example.foodkeeper.ViewMeals.mealsViewActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MenuActivity extends AppCompatActivity {
    private ImageView profileArrow, fridgeArrow, mealsArrow, mealPlanArrow, recipesArrow, shoppingArrow, logoutArrow;
    private BottomNavigationView bottomNav;
    @SuppressLint("WrongViewCast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_menu);

        bottomNav = findViewById(R.id.bottomNav);
        profileArrow = findViewById(R.id.myProfileOptions).findViewById(R.id.profileArrow);
        fridgeArrow = findViewById(R.id.manageRefrigeratorsOption).findViewById(R.id.fridgeArrow);
        mealsArrow = findViewById(R.id.viewMealsOption).findViewById(R.id.mealsArrow);
        mealPlanArrow = findViewById(R.id.mealPlanOption).findViewById(R.id.mealPlansArrow);
        recipesArrow = findViewById(R.id.recipesOption).findViewById(R.id.recipesArrow);
        shoppingArrow = findViewById(R.id.shoppingListOption).findViewById(R.id.shoppingArrow);
        logoutArrow = findViewById(R.id.logOutOption).findViewById(R.id.logoutArrow);

        bottomNav.setOnNavigationItemSelectedListener(item ->{
            int id = item.getItemId();

            if(id == R.id.nav_home){
                startActivity(new Intent(MenuActivity.this, LandingPageActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if(id == R.id.nav_view){
                startActivity(new Intent(MenuActivity.this, ItemsViewActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if(id == R.id.nav_search){
                startActivity(new Intent(MenuActivity.this, SearchActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            else if(id == R.id.nav_expiring) {
                startActivity(new Intent(MenuActivity.this, ExpiringActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_profileMenu) {

                return true;
            } else {
                return false;
            }

        });
        bottomNav.setSelectedItemId(R.id.nav_profileMenu);

        profileArrow.setOnClickListener(v -> {
            Intent intent = new Intent(MenuActivity.this, ProfileActivity.class);
            startActivity(intent);
        });
        fridgeArrow.setOnClickListener(v -> {
            Intent intent = new Intent(MenuActivity.this, ManageFridgeActivity.class);
            startActivity(intent);
        });
        mealsArrow.setOnClickListener(v -> {
           Intent intent = new Intent(MenuActivity.this, mealsViewActivity.class);
         startActivity(intent);
        });
        mealPlanArrow.setOnClickListener(v -> {
        Intent intent = new Intent(MenuActivity.this, WeeklyViewActivity.class);
         startActivity(intent);
        });
        recipesArrow.setOnClickListener(v -> {
           Intent intent = new Intent(MenuActivity.this, RecipeActivity.class);
           startActivity(intent);
            //finish();
        });
        shoppingArrow.setOnClickListener(v -> {
            Intent intent = new Intent(MenuActivity.this, MainShoppingListActivity.class);
            startActivity(intent);
           // finish();
        });
        logoutArrow.setOnClickListener(v -> {
            showLogoutDialog();
        });
    }

    private void showLogoutDialog() {
        LogoutDialog logoutDialog = new LogoutDialog(this, new LogoutDialog.OnLogoutListener() {
            @Override
            public void onLogoutConfirmed() {
                SessionManager userSession = new SessionManager(MenuActivity.this);
                Intent intent = new Intent(MenuActivity.this, LoginActivity.class);
                userSession.logoutUser();
                startActivity(intent);
                finish();
            }

            @Override
            public void onCancel() {
                Toast.makeText(MenuActivity.this, "Logout cancelled", Toast.LENGTH_SHORT).show();
            }
        });
        logoutDialog.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_profileMenu);
        }
    }
}