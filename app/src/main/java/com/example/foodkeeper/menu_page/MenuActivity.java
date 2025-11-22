package com.example.foodkeeper.menu_page;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.foodkeeper.FoodItem.SearchActivity;
import com.example.foodkeeper.FoodItem.expiring.ExpiringActivity;
import com.example.foodkeeper.FoodItem.view_items.ItemsViewActivity;
import com.example.foodkeeper.Fridge.ManageFridgeActivity;
import com.example.foodkeeper.LandingPage.LandingPageActivity;
import com.example.foodkeeper.MealPlan.WeeklyViewActivity;
import com.example.foodkeeper.profile_activity.ProfileActivity;
import com.example.foodkeeper.R;
import com.example.foodkeeper.Recipe.RecipeActivity;
import com.example.foodkeeper.Register.LoginActivity;
import com.example.foodkeeper.Register.SessionManager;
import com.example.foodkeeper.ShoppingList.MainShoppingListActivity;
import com.example.foodkeeper.ViewMeals.mealsViewActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MenuActivity extends AppCompatActivity {
    private LinearLayout myProfileOptions, manageRefrigeratorsOption, viewMealsOption, mealPlanOption, recipesOption, shoppingListOption, logOutOption;
    private BottomNavigationView bottomNav;
    @SuppressLint("WrongViewCast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_menu);

        bottomNav = findViewById(R.id.bottomNav);
        myProfileOptions = findViewById(R.id.myProfileOptions);
        manageRefrigeratorsOption = findViewById(R.id.manageRefrigeratorsOption);
        viewMealsOption = findViewById(R.id.viewMealsOption);
        mealPlanOption = findViewById(R.id.mealPlanOption);
        recipesOption = findViewById(R.id.recipesOption);
        shoppingListOption = findViewById(R.id.shoppingListOption);
        logOutOption = findViewById(R.id.logOutOption);

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

        myProfileOptions.setOnClickListener(v -> {
            Intent intent = new Intent(MenuActivity.this, ProfileActivity.class);
            startActivity(intent);
        });

        manageRefrigeratorsOption.setOnClickListener(v -> {
            Intent intent = new Intent(MenuActivity.this, ManageFridgeActivity.class);
            startActivity(intent);
        });

        viewMealsOption.setOnClickListener(v -> {
            Intent intent = new Intent(MenuActivity.this, mealsViewActivity.class);
            startActivity(intent);
        });

        mealPlanOption.setOnClickListener(v -> {
            Intent intent = new Intent(MenuActivity.this, WeeklyViewActivity.class);
            startActivity(intent);
        });

        recipesOption.setOnClickListener(v -> {
            Intent intent = new Intent(MenuActivity.this, RecipeActivity.class);
            startActivity(intent);
        });

        shoppingListOption.setOnClickListener(v -> {
            Intent intent = new Intent(MenuActivity.this, MainShoppingListActivity.class);
            startActivity(intent);
        });

        logOutOption.setOnClickListener(v -> {
            showLogoutDialog();
        });
    }

    private void showLogoutDialog() {
        LogoutDialog logoutDialog = LogoutDialog.newInstance();
        logoutDialog.setOnLogoutListener(new LogoutDialog.OnLogoutListener() {
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
        logoutDialog.show(getSupportFragmentManager(), "logout_dialog");
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_profileMenu);
        }
    }
}