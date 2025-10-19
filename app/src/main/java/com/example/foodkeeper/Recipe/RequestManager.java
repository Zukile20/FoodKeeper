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
import com.example.foodkeeper.SessionManager;

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
    FetchStateManager fetchStateManager;
    Retrofit retrofit;
    private static final String TAG = "RequestManager";
    private SessionManager sessionManager;

    public RequestManager(Context context) {
        this.context = context;
        this.dbHelper = new Database(context);
        this.fetchStateManager = new FetchStateManager(context);
        this.sessionManager = new SessionManager(context);

        retrofit = new Retrofit.Builder()
                .baseUrl("https://api.spoonacular.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    // Helper method to get user email
    private String getUserEmail() {
        String email = sessionManager.getUserEmail();
        if (email == null || email.isEmpty()) {
            Log.e(TAG, "User email is null or empty!");
        }
        return email;
    }

    // NEW METHOD: Check if user needs to fetch recipes on first login
    public void getRandomRecipesForUser(RandomRecipeResponseListener listener, List<String> tags, String userEmail) {
        try {
            // Check if this user has already fetched recipes
            if (fetchStateManager.hasFetchedRecipes(userEmail)) {
                Log.d(TAG, "User " + userEmail + " has already fetched recipes. Loading from database.");

                // Load from database
                List<Recipe> cachedRecipes = dbHelper.getAllRecipes(userEmail);
                if (!cachedRecipes.isEmpty()) {
                    RandomRecipeApiResponse cachedResponse = new RandomRecipeApiResponse();
                    cachedResponse.recipes = (java.util.ArrayList<Recipe>) cachedRecipes;
                    listener.didFetch(cachedResponse, "Loaded from database (user already fetched)");
                    return;
                }
            }

            Log.d(TAG, "First time login for user " + userEmail + ". Fetching recipes from API.");

            // First time for this user - fetch from API
            fetchRandomRecipesFromAPIForUser(listener, tags, userEmail);

        } catch (Exception e) {
            Log.e(TAG, "Error in getRandomRecipesForUser: " + e.getMessage());
            listener.didError("Error: " + e.getMessage());
        }
    }

    private void fetchRandomRecipesFromAPIForUser(RandomRecipeResponseListener listener, List<String> tags, String userEmail) {
        CallRandomRecipes callRandomRecipes = retrofit.create(CallRandomRecipes.class);
        Call<RandomRecipeApiResponse> call = callRandomRecipes.callRandomRecipe(
                context.getString(R.string.api_key), "100", tags
        );
        call.enqueue(new Callback<RandomRecipeApiResponse>() {
            @Override
            public void onResponse(Call<RandomRecipeApiResponse> call, retrofit2.Response<RandomRecipeApiResponse> response) {
                if (!response.isSuccessful()) {
                    listener.didError("API Error: " + response.message());
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
                                    recipe.fav,
                                    userEmail
                            );
                        } catch (Exception e) {
                            Log.e(TAG, "Error caching recipe: " + e.getMessage());
                        }
                    }

                    // Mark that this user has fetched recipes
                    fetchStateManager.setRecipesFetched(userEmail, true);
                    Log.d(TAG, "Successfully fetched and cached recipes for user " + userEmail);

                    listener.didFetch(data, "Fetched from API (first login)");
                } else {
                    listener.didError("No recipes received from API");
                }
            }

            @Override
            public void onFailure(Call<RandomRecipeApiResponse> call, Throwable t) {
                Log.e(TAG, "Network error: " + t.getMessage());
                listener.didError("Network Error: " + t.getMessage());
            }
        });
    }

    // ORIGINAL METHOD: DATABASE-FIRST for general use (with user email)
    public void getRandomRecipes(RandomRecipeResponseListener listener, List<String> tags) {
        String userEmail = getUserEmail();
        if (userEmail == null || userEmail.isEmpty()) {
            listener.didError("User not logged in");
            return;
        }

        try {
            // First check if we have recipes in database for this user
            List<Recipe> cachedRecipes = dbHelper.getAllRecipes(userEmail);

            if (!cachedRecipes.isEmpty()) {
                // We have recipes, return from database
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
        String userEmail = getUserEmail();
        if (userEmail == null || userEmail.isEmpty()) {
            listener.didError("User not logged in");
            return;
        }

        CallRandomRecipes callRandomRecipes = retrofit.create(CallRandomRecipes.class);
        Call<RandomRecipeApiResponse> call = callRandomRecipes.callRandomRecipe(
                context.getString(R.string.api_key), "100", tags
        );
        call.enqueue(new Callback<RandomRecipeApiResponse>() {
            @Override
            public void onResponse(Call<RandomRecipeApiResponse> call, retrofit2.Response<RandomRecipeApiResponse> response) {
                if (!response.isSuccessful()) {
                    loadFromDatabaseFallback(listener);
                    return;
                }

                RandomRecipeApiResponse data = response.body();
                if (data != null && data.recipes != null) {
                    // Store all recipes in database with userEmail
                    for (Recipe recipe : data.recipes) {
                        try {
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
        String userEmail = getUserEmail();
        if (userEmail == null || userEmail.isEmpty()) {
            listener.didError("User not logged in");
            return;
        }

        try {
            List<Recipe> cachedRecipes = dbHelper.getAllRecipes(userEmail);
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
        String userEmail = getUserEmail();
        if (userEmail == null || userEmail.isEmpty()) {
            listener.didError("User not logged in");
            return;
        }

        try {
            Log.d(TAG, "Getting recipe details for ID: " + id);

            RecipeDetailsResponse cachedDetails = dbHelper.getCachedRecipeDetails(id, userEmail);

            if (cachedDetails != null) {
                Log.d(TAG, "Found cached recipe: " + cachedDetails.title);

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

        fetchRecipeDetailsFromAPI(listener, id);
    }

    private void fetchRecipeDetailsFromAPI(RecipeDetailsListener listener, int id) {
        String userEmail = getUserEmail();
        if (userEmail == null || userEmail.isEmpty()) {
            listener.didError("User not logged in");
            return;
        }

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
                    try {
                        RecipeDetailsResponse cachedDetails = dbHelper.getCachedRecipeDetails(id, userEmail);
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

                    try {
                        cacheCompleteRecipeDetails(details, userEmail);
                        Log.d(TAG, "Successfully cached recipe details");
                        fetchAndCacheInstructions(id);
                    } catch (Exception e) {
                        Log.e(TAG, "Error caching recipe details: " + e.getMessage());
                    }

                    listener.didFetch(details, "Fetched from API and cached");
                } else {
                    listener.didError("No data received from API");
                }
            }

            @Override
            public void onFailure(Call<RecipeDetailsResponse> call, Throwable t) {
                Log.e(TAG, "Network error: " + t.getMessage());
                try {
                    RecipeDetailsResponse cachedDetails = dbHelper.getCachedRecipeDetails(id, userEmail);
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
                    }
                }
            }

            @Override
            public void onFailure(Call<List<InstructionsResponse>> call, Throwable t) {
                Log.w(TAG, "Network error while auto-fetching instructions: " + t.getMessage());
            }
        });
    }

    public void getInstructions(InstructionsListener listener, int recipeId) {
        try {
            Log.d(TAG, "Getting instructions for recipe ID: " + recipeId);

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

                    try {
                        cacheInstructions(recipeId, instructions);
                        Log.d(TAG, "Successfully cached instructions");
                    } catch (Exception e) {
                        Log.e(TAG, "Error caching instructions: " + e.getMessage());
                    }

                    listener.didFetch(instructions, "Fetched from API and cached");
                } else {
                    listener.didError("No instructions available from API");
                }
            }

            @Override
            public void onFailure(Call<List<InstructionsResponse>> call, Throwable t) {
                Log.e(TAG, "Instructions network error: " + t.getMessage());

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
    }private void cacheCompleteRecipeDetails(RecipeDetailsResponse details, String userEmail) {
        try {
            if (details == null || details.title == null) {
                Log.w(TAG, "Cannot cache null or incomplete recipe details");
                return;
            }

            Log.d(TAG, "Caching recipe details for: " + details.title);

            dbHelper.insertRecipe(
                    details.id,
                    details.title,
                    details.image,
                    details.aggregateLikes,
                    details.readyInMinutes,
                    details.servings,
                    details.fav,
                    userEmail
            );

            if (details.extendedIngredients != null && !details.extendedIngredients.isEmpty()) {
                Log.d(TAG, "Caching " + details.extendedIngredients.size() + " ingredients");
                cacheIngredientsForRecipe(details.id, details.extendedIngredients);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in cacheCompleteRecipeDetails: " + e.getMessage());
            throw e;
        }
    }

    private void cacheIngredientsForRecipe(int recipeId, List<ExtendedIngredient> ingredients) {
        try {
            dbHelper.deleteIngredientsForRecipe(recipeId);
            Log.d(TAG, "Caching " + ingredients.size() + " ingredients for recipe " + recipeId);

            for (int i = 0; i < ingredients.size(); i++) {
                ExtendedIngredient ingredient = ingredients.get(i);
                if (ingredient != null && ingredient.name != null && !ingredient.name.trim().isEmpty()) {
                    String ingredientName = ingredient.name.trim();

                    long ingredientId = dbHelper.insertIngredient(ingredientName);

                    if (ingredientId == -1) {
                        ingredientId = dbHelper.getIngredientIdByName(ingredientName);
                    }

                    if (ingredientId > 0) {
                        dbHelper.insertRecipeIngredient(recipeId, ingredientId);
                    }
                }
            }

            int cachedCount = dbHelper.getIngredientCountForRecipe(recipeId);
            Log.d(TAG, "Verification: " + cachedCount + " ingredients cached for recipe " + recipeId);

        } catch (Exception e) {
            Log.e(TAG, "Error caching ingredients: " + e.getMessage());
            throw e;
        }
    }

    private void cacheInstructions(int recipeId, List<InstructionsResponse> instructionsResponses) {
        try {
            if (instructionsResponses == null || instructionsResponses.isEmpty()) {
                Log.w(TAG, "Cannot cache null or empty instructions");
                return;
            }

            Log.d(TAG, "Caching instructions for recipe " + recipeId);

            int deletedCount = dbHelper.deleteInstructionsForRecipe(recipeId);
            Log.d(TAG, "Deleted " + deletedCount + " existing instructions");

            int totalSteps = 0;
            for (InstructionsResponse instructionsBlock : instructionsResponses) {
                if (instructionsBlock != null && instructionsBlock.steps != null) {
                    for (Step step : instructionsBlock.steps) {
                        if (step != null && step.step != null && !step.step.trim().isEmpty()) {
                            long result = dbHelper.insertInstruction(recipeId, step.number, step.step.trim());
                            if (result != -1) {
                                totalSteps++;
                            }
                        }
                    }
                }
            }

            Log.d(TAG, "Successfully cached " + totalSteps + " instruction steps for recipe " + recipeId);

        } catch (Exception e) {
            Log.e(TAG, "Error caching instructions: " + e.getMessage());
            throw e;
        }
    }

    public void searchRecipesFromAPI(RandomRecipeResponseListener listener, List<String> tags) {
        fetchRandomRecipesFromAPI(listener, tags);
    }

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