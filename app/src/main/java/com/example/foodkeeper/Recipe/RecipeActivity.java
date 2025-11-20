package com.example.foodkeeper.Recipe;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
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

import com.example.foodkeeper.FoodkeeperUtils.Database;
import com.example.foodkeeper.R;
import com.example.foodkeeper.Recipe.Adapters.RandomRecipeAdapter;
import com.example.foodkeeper.Recipe.Listeners.RandomRecipeResponseListener;
import com.example.foodkeeper.Recipe.Listeners.RecipeClickListerner;
import com.example.foodkeeper.Recipe.Models.RandomRecipeApiResponse;
import com.example.foodkeeper.Recipe.Models.Recipe;
import com.example.foodkeeper.Register.SessionManager;

import java.util.ArrayList;
import java.util.List;

public class RecipeActivity extends AppCompatActivity {
    ProgressDialog dialog;
    RequestManager manager;
    RandomRecipeAdapter randomRecipeAdapter;
    RecyclerView recyclerView;
    List<String> tags = new ArrayList<>();
    SearchView searchView;

    TextView btnAllRecipes, btnFavoritesOnly;
    LinearLayout layoutEmptyFavorites;

    private boolean showingFavoritesOnly = false;

    Database dbHelper;
    ActivityResultLauncher<Intent> launcher;
    private Button back_btn;

    private SessionManager sessionManager;
    private String userEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.recipe_activity);

        dialog = new ProgressDialog(this);
        dialog.setTitle("Loading...");

        dbHelper = new Database(this);

        sessionManager = new SessionManager(this);
        userEmail = sessionManager.getUserEmail();

        if (userEmail == null || userEmail.isEmpty()) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        manager = new RequestManager(this);

        initializeViews();
        setupSearchView();
        setupFilterButtons();

        loadRecipesFromDatabase();

        launcher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
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
        back_btn = findViewById(R.id.backr_btn);
    }

    private void setupSearchView() {
        searchView.setIconified(true);
        searchView.setQueryHint("Search for a Recipe");

        searchView.setOnQueryTextFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    searchView.clearFocus();
                }
            }
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchRecipes(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.isEmpty()) {
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
        updateButtonStates();

        btnAllRecipes.setOnClickListener(v -> {
            showingFavoritesOnly = false;
            updateButtonStates();
            showAllRecipes();
            searchView.setQuery("", false);
        });

        btnFavoritesOnly.setOnClickListener(v -> {
            showingFavoritesOnly = true;
            updateButtonStates();
            showFavoriteRecipes();
            searchView.setQuery("", false);
        });
    }

    private void updateButtonStates() {
        if (showingFavoritesOnly) {
            btnFavoritesOnly.setBackgroundTintList(getResources().getColorStateList(R.color.blue));
            btnFavoritesOnly.setTextColor(getResources().getColor(android.R.color.darker_gray));

            btnAllRecipes.setBackgroundTintList(getResources().getColorStateList(R.color.light_pink));
            btnAllRecipes.setTextColor(getResources().getColor(R.color.black));
        } else {
            btnAllRecipes.setBackgroundTintList(getResources().getColorStateList(R.color.blue));
            btnAllRecipes.setTextColor(getResources().getColor(android.R.color.darker_gray));

            btnFavoritesOnly.setBackgroundTintList(getResources().getColorStateList(R.color.light_pink));
            btnFavoritesOnly.setTextColor(getResources().getColor(R.color.black));
        }
    }

    private void showAllRecipes() {
        List<Recipe> allRecipes = dbHelper.getAllRecipes(userEmail);
        loadRecipesIntoRecyclerView(allRecipes);

        layoutEmptyFavorites.setVisibility(android.view.View.GONE);
        recyclerView.setVisibility(android.view.View.VISIBLE);
    }

    private void showFavoriteRecipes() {
        List<Recipe> favoriteRecipes = dbHelper.getFavoriteRecipes(userEmail);

        if (favoriteRecipes.isEmpty()) {
            recyclerView.setVisibility(android.view.View.GONE);
            layoutEmptyFavorites.setVisibility(android.view.View.VISIBLE);
        } else {
            layoutEmptyFavorites.setVisibility(android.view.View.GONE);
            recyclerView.setVisibility(android.view.View.VISIBLE);
            loadRecipesIntoRecyclerView(favoriteRecipes);
        }
    }

    private void loadRecipesFromDatabase() {
        List<Recipe> recipes = dbHelper.getAllRecipes(userEmail);
        if (recipes.isEmpty()) {
            dialog.show();
            manager.getRandomRecipes(randomRecipeResponseListener, tags);
        } else {
            if (showingFavoritesOnly) {
                showFavoriteRecipes();
            } else {
                showAllRecipes();
            }
        }
    }

    private void searchRecipes(String query) {
        dialog.show();

        List<Recipe> localResults;
        if (showingFavoritesOnly) {
            localResults = dbHelper.searchFavoriteRecipes(query, userEmail);
        } else {
            localResults = dbHelper.searchRecipes(query, userEmail);
        }

        if (!localResults.isEmpty()) {
            loadRecipesIntoRecyclerView(localResults);
            layoutEmptyFavorites.setVisibility(android.view.View.GONE);
            recyclerView.setVisibility(android.view.View.VISIBLE);
            dialog.dismiss();
        } else {
            if (!showingFavoritesOnly) {
                tags.clear();
                tags.add(query);

                manager.searchRecipesFromAPI(new RandomRecipeResponseListener() {
                    @Override
                    public void didFetch(RandomRecipeApiResponse response, String message) {
                        dialog.dismiss();

                        for (Recipe recipe : response.recipes) {
                            dbHelper.insertRecipe(
                                    recipe.id,
                                    recipe.title,
                                    recipe.image,
                                    recipe.aggregateLikes,
                                    recipe.readyInMinutes,
                                    recipe.servings,
                                    recipe.fav,
                                    userEmail
                            );
                        }

                        List<Recipe> updatedResults = dbHelper.searchRecipes(query, userEmail);
                        if (updatedResults.isEmpty()) {
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

        // Set the empty listener
        randomRecipeAdapter.setOnFavoritesEmptyListener(new RandomRecipeAdapter.OnFavoritesEmptyListener() {
            @Override
            public void onFavoritesEmpty() {
                // Show empty state immediately when last favorite is removed
                recyclerView.setVisibility(android.view.View.GONE);
                layoutEmptyFavorites.setVisibility(android.view.View.VISIBLE);
            }
        });

        recyclerView.setAdapter(randomRecipeAdapter);
    }

    private final RandomRecipeResponseListener randomRecipeResponseListener = new RandomRecipeResponseListener() {
        @Override
        public void didFetch(RandomRecipeApiResponse response, String message) {
            dialog.dismiss();

            for (Recipe recipe : response.recipes) {
                dbHelper.insertRecipe(
                        recipe.id,
                        recipe.title,
                        recipe.image,
                        recipe.aggregateLikes,
                        recipe.readyInMinutes,
                        recipe.servings,
                        recipe.fav,
                        userEmail
                );
            }

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