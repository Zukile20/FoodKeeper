package com.example.foodkeeper.Recipe;


import android.content.Context;

import android.util.Log;



import com.example.foodkeeper.Database;
import com.example.foodkeeper.R;
import com.example.foodkeeper.Recipe.Listeners.InstructionsListener;
import com.example.foodkeeper.Recipe.Listeners.RandomRecipeResponseListener;
import com.example.foodkeeper.Recipe.Listeners.RecipeDetailsListener;
import com.example.foodkeeper.Recipe.Listeners.SimilarRecipesListerner;
import com.example.foodkeeper.Recipe.Models.ExtendedIngredient;
import com.example.foodkeeper.Recipe.Models.InstructionsResponse;
import com.example.foodkeeper.Recipe.Models.RandomRecipeApiResponse;
import com.example.foodkeeper.Recipe.Models.Recipe;
import com.example.foodkeeper.Recipe.Models.RecipeDetailsResponse;
import com.example.foodkeeper.Recipe.Models.SimilarRecipeResponse;
import com.example.foodkeeper.Recipe.Models.Step;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public class RequestManager {

    Context context;
    Database dbHelper;
    Retrofit retrofit;
    private static final String TAG = "RequestManager";

    public RequestManager(Context context) {
        this.context = context;
        this.dbHelper = new Database(context);

        retrofit = new Retrofit.Builder()
                .baseUrl("https://api.spoonacular.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    // DATABASE-FIRST: Check database first, then API if needed
    public void getRandomRecipes(RandomRecipeResponseListener listener, List<String> tags) {
        try {
            // First check if we have recipes in database
            List<Recipe> cachedRecipes = dbHelper.getAllRecipes();

            if (!cachedRecipes.isEmpty()) {
                // We have enough recipes, return from database
                RandomRecipeApiResponse cachedResponse = new RandomRecipeApiResponse();
                cachedResponse.recipes = (java.util.ArrayList<Recipe>) cachedRecipes;
                listener.didFetch(cachedResponse, "Loaded from database");
                return;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading cached recipes: " + e.getMessage());
        }

        // Not enough recipes in database, fetch from API
        fetchRandomRecipesFromAPI(listener, tags);
    }

    private void fetchRandomRecipesFromAPI(RandomRecipeResponseListener listener, List<String> tags) {
        CallRandomRecipes callRandomRecipes = retrofit.create(CallRandomRecipes.class);
        Call<RandomRecipeApiResponse> call = callRandomRecipes.callRandomRecipe(
                context.getString(R.string.api_key), "100", tags
        );
        call.enqueue(new Callback<RandomRecipeApiResponse>() {
            @Override
            public void onResponse(Call<RandomRecipeApiResponse> call, retrofit2.Response<RandomRecipeApiResponse> response) {
                if (!response.isSuccessful()) {
                    // API failed, try to load from database
                    loadFromDatabaseFallback(listener);
                    return;
                }

                RandomRecipeApiResponse data = response.body();
                if (data != null && data.recipes != null) {
                    // Store all recipes in database
                    for (Recipe recipe : data.recipes) {
                        try {
                            dbHelper.insertRecipe(
                                    recipe.id,
                                    recipe.title,
                                    recipe.image,
                                    recipe.aggregateLikes,
                                    recipe.readyInMinutes,
                                    recipe.servings,
                                    recipe.fav
                            );
                        } catch (Exception e) {
                            Log.e(TAG, "Error caching recipe: " + e.getMessage());
                        }
                    }
                    listener.didFetch(data, "Fetched from API and cached");
                } else {
                    loadFromDatabaseFallback(listener);
                }
            }

            @Override
            public void onFailure(Call<RandomRecipeApiResponse> call, Throwable t) {
                loadFromDatabaseFallback(listener);
            }
        });

    }

    private void loadFromDatabaseFallback(RandomRecipeResponseListener listener) {
        try {
            List<Recipe> cachedRecipes = dbHelper.getAllRecipes();
            if (!cachedRecipes.isEmpty()) {
                RandomRecipeApiResponse cachedResponse = new RandomRecipeApiResponse();
                cachedResponse.recipes = (java.util.ArrayList<Recipe>) cachedRecipes;
                listener.didFetch(cachedResponse, "Loaded from database (API failed)");
            } else {
                listener.didError("No data available");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading fallback data: " + e.getMessage());
            listener.didError("Database error: " + e.getMessage());
        }
    }

    // DATABASE-FIRST: Recipe details with complete caching (including instructions)
    public void getRecipeDetails(RecipeDetailsListener listener, int id) {
        try {
            Log.d(TAG, "Getting recipe details for ID: " + id);

            // First check database for complete recipe details
            RecipeDetailsResponse cachedDetails = dbHelper.getCachedRecipeDetails(id);

            if (cachedDetails != null) {
                Log.d(TAG, "Found cached recipe: " + cachedDetails.title);

                // Validate that we have complete data (including checking for instructions)
                boolean hasIngredients = cachedDetails.extendedIngredients != null &&
                        !cachedDetails.extendedIngredients.isEmpty();
                boolean hasInstructions = dbHelper.hasInstructionsForRecipe(id);

                if (cachedDetails.title != null && hasIngredients && hasInstructions) {
                    Log.d(TAG, "Using cached recipe with " + cachedDetails.extendedIngredients.size() +
                            " ingredients and instructions available");
                    listener.didFetch(cachedDetails, "Loaded from database");
                    return;
                }
            }

            Log.d(TAG, "No complete cached data found, fetching from API");
        } catch (Exception e) {
            Log.e(TAG, "Error checking cached recipe details: " + e.getMessage());
        }

        // Not in database or incomplete, fetch from API
        fetchRecipeDetailsFromAPI(listener, id);
    }

    private void fetchRecipeDetailsFromAPI(RecipeDetailsListener listener, int id) {
        Log.d(TAG, "Fetching recipe details from API for ID: " + id);

        CallRecipeDetails callRecipeDetails = retrofit.create(CallRecipeDetails.class);
        Call<RecipeDetailsResponse> call = callRecipeDetails.callRecipeDetails(
                id, context.getString(R.string.api_key)
        );

        call.enqueue(new Callback<RecipeDetailsResponse>() {
            @Override
            public void onResponse(Call<RecipeDetailsResponse> call, retrofit2.Response<RecipeDetailsResponse> response) {
                if (!response.isSuccessful()) {
                    Log.e(TAG, "API error: " + response.code() + " " + response.message());
                    // Try loading from database as fallback
                    try {
                        RecipeDetailsResponse cachedDetails = dbHelper.getCachedRecipeDetails(id);
                        if (cachedDetails != null && cachedDetails.title != null) {
                            listener.didFetch(cachedDetails, "Loaded from database (API failed)");
                        } else {
                            listener.didError("API Error: " + response.message());
                        }
                    } catch (Exception e) {
                        listener.didError("API Error: " + response.message());
                    }
                    return;
                }

                RecipeDetailsResponse details = response.body();
                if (details != null) {
                    Log.d(TAG, "Received recipe from API: " + details.title);

                    // Cache the complete recipe details
                    try {
                        cacheCompleteRecipeDetails(details);
                        Log.d(TAG, "Successfully cached recipe details");

                        // Automatically fetch and cache instructions after recipe details
                        fetchAndCacheInstructions(id);

                    } catch (Exception e) {
                        Log.e(TAG, "Error caching recipe details: " + e.getMessage());
                        // Continue anyway - don't let caching errors affect the UI
                    }

                    listener.didFetch(details, "Fetched from API and cached");
                } else {
                    listener.didError("No data received from API");
                }
            }

            @Override
            public void onFailure(Call<RecipeDetailsResponse> call, Throwable t) {
                Log.e(TAG, "Network error: " + t.getMessage());
                // Try loading from database as fallback
                try {
                    RecipeDetailsResponse cachedDetails = dbHelper.getCachedRecipeDetails(id);
                    if (cachedDetails != null && cachedDetails.title != null) {
                        listener.didFetch(cachedDetails, "Loaded from database (Network failed)");
                    } else {
                        listener.didError("Network Error: " + t.getMessage());
                    }
                } catch (Exception e) {
                    listener.didError("Network Error: " + t.getMessage());
                }
            }
        });

    }

    // Helper method to automatically fetch and cache instructions
    private void fetchAndCacheInstructions(int recipeId) {
        Log.d(TAG, "Auto-fetching instructions for recipe " + recipeId);

        CallInstructions callInstructions = retrofit.create(CallInstructions.class);
        Call<List<InstructionsResponse>> call = callInstructions.callInstructions(
                recipeId, context.getString(R.string.api_key)
        );

        call.enqueue(new Callback<List<InstructionsResponse>>() {
            @Override
            public void onResponse(Call<List<InstructionsResponse>> call, retrofit2.Response<List<InstructionsResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<InstructionsResponse> instructions = response.body();
                    if (!instructions.isEmpty()) {
                        try {
                            cacheInstructions(recipeId, instructions);
                            Log.d(TAG, "Instructions auto-cached successfully for recipe " + recipeId);
                        } catch (Exception e) {
                            Log.e(TAG, "Error auto-caching instructions: " + e.getMessage());
                        }
                    } else {
                        Log.d(TAG, "No instructions available for recipe " + recipeId);
                    }
                } else {
                    Log.w(TAG, "Failed to fetch instructions for auto-caching: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<List<InstructionsResponse>> call, Throwable t) {
                Log.w(TAG, "Network error while auto-fetching instructions: " + t.getMessage());
            }
        });

    }

    // DATABASE-FIRST: Instructions with caching
    public void getInstructions(InstructionsListener listener, int recipeId) {
        try {
            Log.d(TAG, "Getting instructions for recipe ID: " + recipeId);

            // Check database first
            List<InstructionsResponse> cachedInstructions = dbHelper.getCachedInstructions(recipeId);

            if (cachedInstructions != null && !cachedInstructions.isEmpty()) {
                Log.d(TAG, "Found cached instructions");
                listener.didFetch(cachedInstructions, "Loaded from database");
                return;
            }

            Log.d(TAG, "No cached instructions found, fetching from API");
        } catch (Exception e) {
            Log.e(TAG, "Error checking cached instructions: " + e.getMessage());
        }

        // Not in database, fetch from API
        fetchInstructionsFromAPI(listener, recipeId);
    }

    private void fetchInstructionsFromAPI(InstructionsListener listener, int recipeId) {
        Log.d(TAG, "Fetching instructions from API for recipe ID: " + recipeId);

        CallInstructions callInstructions = retrofit.create(CallInstructions.class);
        Call<List<InstructionsResponse>> call = callInstructions.callInstructions(
                recipeId, context.getString(R.string.api_key)
        );

        call.enqueue(new Callback<List<InstructionsResponse>>() {
            @Override
            public void onResponse(Call<List<InstructionsResponse>> call, retrofit2.Response<List<InstructionsResponse>> response) {
                if (!response.isSuccessful()) {
                    Log.e(TAG, "Instructions API error: " + response.code() + " " + response.message());

                    // Try loading from database as fallback
                    try {
                        List<InstructionsResponse> cachedInstructions = dbHelper.getCachedInstructions(recipeId);
                        if (cachedInstructions != null && !cachedInstructions.isEmpty()) {
                            listener.didFetch(cachedInstructions, "Loaded from database (API failed)");
                        } else {
                            listener.didError("API Error: " + response.message());
                        }
                    } catch (Exception e) {
                        listener.didError("API Error: " + response.message());
                    }
                    return;
                }

                List<InstructionsResponse> instructions = response.body();
                if (instructions != null && !instructions.isEmpty()) {
                    Log.d(TAG, "Received instructions from API");

                    // Cache instructions in database
                    try {
                        cacheInstructions(recipeId, instructions);
                        Log.d(TAG, "Successfully cached instructions");
                    } catch (Exception e) {
                        Log.e(TAG, "Error caching instructions: " + e.getMessage());
                        // Continue anyway - don't let caching errors affect the UI
                    }

                    listener.didFetch(instructions, "Fetched from API and cached");
                } else {
                    listener.didError("No instructions available from API");
                }
            }

            @Override
            public void onFailure(Call<List<InstructionsResponse>> call, Throwable t) {
                Log.e(TAG, "Instructions network error: " + t.getMessage());

                // Try loading from database as fallback
                try {
                    List<InstructionsResponse> cachedInstructions = dbHelper.getCachedInstructions(recipeId);
                    if (cachedInstructions != null && !cachedInstructions.isEmpty()) {
                        listener.didFetch(cachedInstructions, "Loaded from database (Network failed)");
                    } else {
                        listener.didError("Network Error: " + t.getMessage());
                    }
                } catch (Exception e) {
                    listener.didError("Network Error: " + t.getMessage());
                }
            }
        });
    }

    // NEW: Get complete recipe data including instructions in one call
    public void getCompleteRecipeData(RecipeDetailsListener detailsListener,
                                      InstructionsListener instructionsListener,
                                      int recipeId) {
        Log.d(TAG, "Getting complete recipe data for ID: " + recipeId);

        // Get recipe details (which will also auto-fetch instructions)
        getRecipeDetails(detailsListener, recipeId);

        // Get instructions (will use cached if available)
        getInstructions(instructionsListener, recipeId);
    }

    // Similar recipes - keep as is since they're not commonly cached
    public void getSimilarRecipes(SimilarRecipesListerner listener, int id) {
        CallSimilarRecipes callSimilarRecipes = retrofit.create(CallSimilarRecipes.class);
        Call<List<SimilarRecipeResponse>> call = callSimilarRecipes.callSimilarRecipe(
                id, "10", context.getString(R.string.api_key)
        );

        call.enqueue(new Callback<List<SimilarRecipeResponse>>() {
            @Override
            public void onResponse(Call<List<SimilarRecipeResponse>> call, Response<List<SimilarRecipeResponse>> response) {
                if (!response.isSuccessful()) {
                    listener.didError(response.message());
                    return;
                }
                listener.didFetch(response.body(), response.message());
            }

            @Override
            public void onFailure(Call<List<SimilarRecipeResponse>> call, Throwable t) {
                listener.didError(t.getMessage());
            }
        });
    }

    // ========================================
    // HELPER METHODS FOR CACHING
    // ========================================

    // Helper method to cache complete recipe details
    private void cacheCompleteRecipeDetails(RecipeDetailsResponse details) {
        try {
            if (details == null || details.title == null) {
                Log.w(TAG, "Cannot cache null or incomplete recipe details");
                return;
            }

            Log.d(TAG, "Caching recipe details for: " + details.title);

            // First ensure the basic recipe info is stored
            dbHelper.insertRecipe(
                    details.id,
                    details.title,
                    details.image,
                    details.aggregateLikes,
                    details.readyInMinutes,
                    details.servings,
                    details.fav
            );

            // Cache ingredients
            if (details.extendedIngredients != null && !details.extendedIngredients.isEmpty()) {
                Log.d(TAG, "Caching " + details.extendedIngredients.size() + " ingredients");
                cacheIngredientsForRecipe(details.id, details.extendedIngredients);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in cacheCompleteRecipeDetails: " + e.getMessage());
            throw e; // Re-throw so calling method can handle
        }
    }

    // Helper method to cache ingredients for a recipe
    private void cacheIngredientsForRecipe(int recipeId, List<ExtendedIngredient> ingredients) {
        try {
            // Clear existing ingredients for this recipe first to avoid duplicates
            dbHelper.deleteIngredientsForRecipe(recipeId);

            Log.d(TAG, "Caching " + ingredients.size() + " ingredients for recipe " + recipeId);

            for (int i = 0; i < ingredients.size(); i++) {
                ExtendedIngredient ingredient = ingredients.get(i);
                if (ingredient != null && ingredient.name != null && !ingredient.name.trim().isEmpty()) {
                    String ingredientName = ingredient.name.trim();
                    Log.d(TAG, "Processing ingredient " + (i+1) + ": " + ingredientName);

                    // Insert ingredient (will ignore if already exists)
                    long ingredientId = dbHelper.insertIngredient(ingredientName);

                    // If ingredient was ignored (already exists), get its ID
                    if (ingredientId == -1) {
                        ingredientId = dbHelper.getIngredientIdByName(ingredientName);
                    }

                    // Link recipe to ingredient
                    if (ingredientId > 0) {
                        dbHelper.insertRecipeIngredient(recipeId, ingredientId);
                        Log.d(TAG, "Successfully linked ingredient: " + ingredientName + " (ID: " + ingredientId + ")");
                    } else {
                        Log.e(TAG, "Failed to get ingredient ID for: " + ingredientName);
                    }
                } else {
                    Log.w(TAG, "Skipping invalid ingredient at index " + i);
                }
            }

            // Verify what we cached
            int cachedCount = dbHelper.getIngredientCountForRecipe(recipeId);
            Log.d(TAG, "Verification: " + cachedCount + " ingredients cached for recipe " + recipeId);

        } catch (Exception e) {
            Log.e(TAG, "Error caching ingredients: " + e.getMessage());
            e.printStackTrace();
            throw e; // Re-throw so calling method can handle
        }
    }

    // Helper method to cache instructions
    private void cacheInstructions(int recipeId, List<InstructionsResponse> instructionsResponses) {
        try {
            if (instructionsResponses == null || instructionsResponses.isEmpty()) {
                Log.w(TAG, "Cannot cache null or empty instructions");
                return;
            }

            Log.d(TAG, "Caching instructions for recipe " + recipeId);

            // Delete any existing instructions for this recipe
            int deletedCount = dbHelper.deleteInstructionsForRecipe(recipeId);
            Log.d(TAG, "Deleted " + deletedCount + " existing instructions");

            // Cache new instructions
            int totalSteps = 0;
            for (InstructionsResponse instructionsBlock : instructionsResponses) {
                if (instructionsBlock != null && instructionsBlock.steps != null) {
                    for (Step step : instructionsBlock.steps) {
                        if (step != null && step.step != null && !step.step.trim().isEmpty()) {
                            long result = dbHelper.insertInstruction(recipeId, step.number, step.step.trim());
                            if (result != -1) {
                                totalSteps++;
                                Log.d(TAG, "Cached instruction step " + step.number + ": " +
                                        step.step.substring(0, Math.min(50, step.step.length())) + "...");
                            }
                        }
                    }
                }
            }

            Log.d(TAG, "Successfully cached " + totalSteps + " instruction steps for recipe " + recipeId);

        } catch (Exception e) {
            Log.e(TAG, "Error caching instructions: " + e.getMessage());
            e.printStackTrace();
            throw e; // Re-throw so calling method can handle
        }
    }

    public void searchRecipesFromAPI(RandomRecipeResponseListener listener, List<String> tags) {
        // Always fetch from API for search queries, bypass database check
        fetchRandomRecipesFromAPI(listener, tags);
    }

    // ========================================
    // RETROFIT INTERFACES
    // ========================================

    private interface CallRandomRecipes {
        @GET("recipes/random")
        Call<RandomRecipeApiResponse> callRandomRecipe(
                @Query("apiKey") String apiKey,
                @Query("number") String number,
                @Query("tags") List<String> tags
        );
    }

    private interface CallRecipeDetails {
        @GET("recipes/{id}/information")
        Call<RecipeDetailsResponse> callRecipeDetails(
                @Path("id") int id,
                @Query("apiKey") String apiKey
        );
    }

    private interface CallSimilarRecipes {
        @GET("recipes/{id}/similar")
        Call<List<SimilarRecipeResponse>> callSimilarRecipe(
                @Path("id") int id,
                @Query("number") String number,
                @Query("apiKey") String apiKey
        );
    }

    private interface CallInstructions {
        @GET("recipes/{id}/analyzedInstructions")
        Call<List<InstructionsResponse>> callInstructions(
                @Path("id") int id,
                @Query("apiKey") String apiKey
        );
    }
}