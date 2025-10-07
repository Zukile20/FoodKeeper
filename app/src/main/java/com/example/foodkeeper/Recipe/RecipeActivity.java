package com.example.foodkeeper.Recipe;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import com.example.foodkeeper.Database;
import com.example.foodkeeper.R;
import com.example.foodkeeper.Recipe.Adapters.RandomRecipeAdapter;
import com.example.foodkeeper.Recipe.Listeners.RandomRecipeResponseListener;
import com.example.foodkeeper.Recipe.Listeners.RecipeClickListerner;
import com.example.foodkeeper.Recipe.Models.RandomRecipeApiResponse;
import com.example.foodkeeper.Recipe.Models.Recipe;

import java.util.ArrayList;
import java.util.List;

public class RecipeActivity extends AppCompatActivity {
    ProgressDialog dialog;
    RequestManager manager;
    RandomRecipeAdapter randomRecipeAdapter;
    RecyclerView recyclerView;
    List<String> tags = new ArrayList<>();
    SearchView searchView;

    // Filter buttons
    TextView btnAllRecipes, btnFavoritesOnly;
    LinearLayout layoutEmptyFavorites;

    // Filter state
    private boolean showingFavoritesOnly = false;

    Database dbHelper;
    ActivityResultLauncher<Intent> launcher;
    private Button back_btn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.recipe_activity);

        dialog = new ProgressDialog(this);
        dialog.setTitle("Loading...");

        dbHelper = new Database(this);
        manager = new RequestManager(this);

        // Initialize UI elements
        initializeViews();
        setupSearchView();
        setupFilterButtons();

        // Load recipes at startup
        loadRecipesFromDatabase();

        launcher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    // Refresh the current view based on filter state
                    if (showingFavoritesOnly) {
                        showFavoriteRecipes();
                    } else {
                        showAllRecipes();
                    }
                });
        back_btn.setOnClickListener(v -> {
            finish();
        });
    }

    private void initializeViews() {
        searchView = findViewById(R.id.search_view);
        btnAllRecipes = findViewById(R.id.btn_all_recipes);
        btnFavoritesOnly = findViewById(R.id.btn_favorites_only);
        layoutEmptyFavorites = findViewById(R.id.layout_empty_favorites);
        recyclerView = findViewById(R.id.recycler_random);
        back_btn=findViewById(R.id.backr_btn);
    }

    private void setupSearchView() {
        searchView.setIconified(false);
        searchView.setQueryHint("Search for a Recipe");

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchRecipes(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.isEmpty()) {
                    // Reset to current filter when search is cleared
                    if (showingFavoritesOnly) {
                        showFavoriteRecipes();
                    } else {
                        showAllRecipes();
                    }
                }
                return false;
            }
        });
    }

    private void setupFilterButtons() {
        // Set initial button states
        updateButtonStates();

        btnAllRecipes.setOnClickListener(v -> {
            showingFavoritesOnly = false;
            updateButtonStates();
            showAllRecipes();
            searchView.setQuery("", false); // Clear search
        });

        btnFavoritesOnly.setOnClickListener(v -> {
            showingFavoritesOnly = true;
            updateButtonStates();
            showFavoriteRecipes();
            searchView.setQuery("", false); // Clear search

        });
    }

    private void updateButtonStates() {
        if (showingFavoritesOnly) {
            // Favorites button is active
            btnFavoritesOnly.setBackgroundTintList(getResources().getColorStateList(R.color.blue));
            btnFavoritesOnly.setTextColor(getResources().getColor(android.R.color.darker_gray));

            // All recipes button is inactive
            btnAllRecipes.setBackgroundTintList(getResources().getColorStateList(R.color.light_pink));
            btnAllRecipes.setTextColor(getResources().getColor(R.color.black));
        } else {
            // All recipes button is active
            btnAllRecipes.setBackgroundTintList(getResources().getColorStateList(R.color.blue));
            btnAllRecipes.setTextColor(getResources().getColor(android.R.color.darker_gray));

            // Favorites button is inactive
            btnFavoritesOnly.setBackgroundTintList(getResources().getColorStateList(R.color.light_pink));
            btnFavoritesOnly.setTextColor(getResources().getColor(R.color.black));
        }
    }

    private void showAllRecipes() {
        List<Recipe> allRecipes = dbHelper.getAllRecipes();
        loadRecipesIntoRecyclerView(allRecipes);

        // Hide empty state
        layoutEmptyFavorites.setVisibility(android.view.View.GONE);
        recyclerView.setVisibility(android.view.View.VISIBLE);
    }

    private void showFavoriteRecipes() {
        List<Recipe> favoriteRecipes = dbHelper.getFavoriteRecipes();

        if (favoriteRecipes.isEmpty()) {
            // Show empty favorites state
            recyclerView.setVisibility(android.view.View.GONE);
            layoutEmptyFavorites.setVisibility(android.view.View.VISIBLE);
        } else {
            // Show favorite recipes
            layoutEmptyFavorites.setVisibility(android.view.View.GONE);
            recyclerView.setVisibility(android.view.View.VISIBLE);
            loadRecipesIntoRecyclerView(favoriteRecipes);
        }
    }

    private void loadRecipesFromDatabase() {
        List<Recipe> recipes = dbHelper.getAllRecipes();
        if (recipes.isEmpty()) {
            // If DB empty, fetch from API
            dialog.show();
            manager.getRandomRecipes(randomRecipeResponseListener, tags);
        } else {
            // Load recipes based on current filter
            if (showingFavoritesOnly) {
                showFavoriteRecipes();
            } else {
                showAllRecipes();
            }
        }
    }

    private void searchRecipes(String query) {
        dialog.show();

        // Search based on current filter
        List<Recipe> localResults;
        if (showingFavoritesOnly) {
            localResults = dbHelper.searchFavoriteRecipes(query);
        } else {
            localResults = dbHelper.searchRecipes(query);
        }

        if (!localResults.isEmpty()) {
            // Found locally → show instantly
            loadRecipesIntoRecyclerView(localResults);
            layoutEmptyFavorites.setVisibility(android.view.View.GONE);
            recyclerView.setVisibility(android.view.View.VISIBLE);
            dialog.dismiss();
        } else {
            // Not found locally → fetch from API (only if showing all recipes)
            if (!showingFavoritesOnly) {
                tags.clear();
                tags.add(query);

                // Use searchRecipesFromAPI instead of getRandomRecipes to force API call
                manager.searchRecipesFromAPI(new RandomRecipeResponseListener() {
                    @Override
                    public void didFetch(RandomRecipeApiResponse response, String message) {
                        dialog.dismiss();

                        // Save new recipes into DB
                        for (Recipe recipe : response.recipes) {
                            dbHelper.insertRecipe(
                                    recipe.id,
                                    recipe.title,
                                    recipe.image,
                                    recipe.aggregateLikes,
                                    recipe.readyInMinutes,
                                    recipe.servings,
                                    recipe.fav
                            );
                        }

                        // Reload results from DB for consistency
                        List<Recipe> updatedResults = dbHelper.searchRecipes(query);
                        if (updatedResults.isEmpty()) {
                            // Show empty state message for search
                            recyclerView.setVisibility(android.view.View.GONE);
                            layoutEmptyFavorites.setVisibility(android.view.View.GONE);
                            Toast.makeText(RecipeActivity.this, "No recipes found for: " + query, Toast.LENGTH_SHORT).show();
                        } else {
                            loadRecipesIntoRecyclerView(updatedResults);
                            recyclerView.setVisibility(android.view.View.VISIBLE);
                            layoutEmptyFavorites.setVisibility(android.view.View.GONE);
                        }
                    }

                    @Override
                    public void didError(String message) {
                        dialog.dismiss();
                        recyclerView.setVisibility(android.view.View.GONE);
                        layoutEmptyFavorites.setVisibility(android.view.View.GONE);
                        Toast.makeText(RecipeActivity.this, "Error searching recipes: " + message, Toast.LENGTH_SHORT).show();
                    }
                }, tags);
            } else {
                // No favorites found matching search
                dialog.dismiss();
                recyclerView.setVisibility(android.view.View.GONE);
                layoutEmptyFavorites.setVisibility(android.view.View.VISIBLE);
                Toast.makeText(this, "No favorite recipes found for: " + query, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void loadRecipesIntoRecyclerView(List<Recipe> recipes) {
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new GridLayoutManager(RecipeActivity.this, 1));
        randomRecipeAdapter = new RandomRecipeAdapter(RecipeActivity.this, recipes, recipeClickListerner);
        recyclerView.setAdapter(randomRecipeAdapter);
    }

    private final RandomRecipeResponseListener randomRecipeResponseListener = new RandomRecipeResponseListener() {
        @Override
        public void didFetch(RandomRecipeApiResponse response, String message) {
            dialog.dismiss();

            // Save results into DB
            for (Recipe recipe : response.recipes) {
                dbHelper.insertRecipe(
                        recipe.id,
                        recipe.title,
                        recipe.image,
                        recipe.aggregateLikes,
                        recipe.readyInMinutes,
                        recipe.servings,
                        recipe.fav
                );
            }

            // Reload from DB based on current filter
            loadRecipesFromDatabase();
        }

        @Override
        public void didError(String message) {
            dialog.dismiss();
            Toast.makeText(RecipeActivity.this, "Error: " + message, Toast.LENGTH_SHORT).show();
        }
    };

    private final RecipeClickListerner recipeClickListerner = new RecipeClickListerner() {
        @Override
        public void onRecipeClicked(String id) {
            launcher.launch(new Intent(RecipeActivity.this, RecipeDetailsActivity.class).putExtra("id", id));
        }
    };
}