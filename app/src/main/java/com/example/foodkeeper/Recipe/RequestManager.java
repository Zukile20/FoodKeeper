package com.example.foodkeeper.Recipe;

import android.content.Context;
import android.util.Log;

import com.example.foodkeeper.FoodkeeperUtils.Database;
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
import com.example.foodkeeper.Register.SessionManager;

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

    private String getUserEmail() {
        String email = sessionManager.getUserEmail();
        return email;
    }

    public void getRandomRecipesForUser(RandomRecipeResponseListener listener, List<String> tags, String userEmail) {
        try {
            if (fetchStateManager.hasFetchedRecipes(userEmail)) {
                List<Recipe> cachedRecipes = dbHelper.getAllRecipes(userEmail);
                if (!cachedRecipes.isEmpty()) {
                    RandomRecipeApiResponse cachedResponse = new RandomRecipeApiResponse();
                    cachedResponse.recipes = (java.util.ArrayList<Recipe>) cachedRecipes;
                    listener.didFetch(cachedResponse, "Loaded from database (user already fetched)");
                    return;
                }
            }
            fetchRandomRecipesFromAPIForUser(listener, tags, userEmail);

        } catch (Exception e) {
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
                             e.getMessage();
                        }
                    }

                    fetchStateManager.setRecipesFetched(userEmail, true);
                    listener.didFetch(data, "Fetched from API (first login)");
                } else {
                    listener.didError("No recipes received from API");
                }
            }

            @Override
            public void onFailure(Call<RandomRecipeApiResponse> call, Throwable t) {
                listener.didError("Network Error: " + t.getMessage());
            }
        });
    }

    public void getRandomRecipes(RandomRecipeResponseListener listener, List<String> tags) {
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
                listener.didFetch(cachedResponse, "Loaded from database");
                return;
            }
        } catch (Exception e) {
           e.getMessage();
        }

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
            listener.didError("Database error: " + e.getMessage());
        }
    }

    public void getRecipeDetails(RecipeDetailsListener listener, int id) {
        String userEmail = getUserEmail();
        if (userEmail == null || userEmail.isEmpty()) {
            listener.didError("User not logged in");
            return;
        }

        try {
            RecipeDetailsResponse cachedDetails = dbHelper.getCachedRecipeDetails(id, userEmail);

            if (cachedDetails != null) {
                boolean hasIngredients = cachedDetails.extendedIngredients != null &&
                        !cachedDetails.extendedIngredients.isEmpty();
                boolean hasInstructions = dbHelper.hasInstructionsForRecipe(id);

                if (cachedDetails.title != null && hasIngredients && hasInstructions) {
                    listener.didFetch(cachedDetails, "Loaded from database");
                    return;
                }
            }

        } catch (Exception e) {
             e.getMessage();
        }

        fetchRecipeDetailsFromAPI(listener, id);
    }

    private void fetchRecipeDetailsFromAPI(RecipeDetailsListener listener, int id) {
        String userEmail = getUserEmail();
        if (userEmail == null || userEmail.isEmpty()) {
            listener.didError("User not logged in");
            return;
        }


        CallRecipeDetails callRecipeDetails = retrofit.create(CallRecipeDetails.class);
        Call<RecipeDetailsResponse> call = callRecipeDetails.callRecipeDetails(
                id, context.getString(R.string.api_key)
        );

        call.enqueue(new Callback<RecipeDetailsResponse>() {
            @Override
            public void onResponse(Call<RecipeDetailsResponse> call, retrofit2.Response<RecipeDetailsResponse> response) {
                if (!response.isSuccessful()) {
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
                    try {
                        cacheCompleteRecipeDetails(details, userEmail);
                        fetchAndCacheInstructions(id);
                    } catch (Exception e) {
                        e.getMessage();
                    }

                    listener.didFetch(details, "Fetched from API and cached");
                } else {
                    listener.didError("No data received from API");
                }
            }

            @Override
            public void onFailure(Call<RecipeDetailsResponse> call, Throwable t) {
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
                        } catch (Exception e) {
                            e.getMessage();
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<List<InstructionsResponse>> call, Throwable t) {
               t.getMessage();
            }
        });
    }

    public void getInstructions(InstructionsListener listener, int recipeId) {
        try {
            List<InstructionsResponse> cachedInstructions = dbHelper.getCachedInstructions(recipeId);

            if (cachedInstructions != null && !cachedInstructions.isEmpty()) {
                listener.didFetch(cachedInstructions, "Loaded from database");
                return;
            }

        } catch (Exception e) {
           e.getMessage();
        }

        fetchInstructionsFromAPI(listener, recipeId);
    }

    private void fetchInstructionsFromAPI(InstructionsListener listener, int recipeId) {

        CallInstructions callInstructions = retrofit.create(CallInstructions.class);
        Call<List<InstructionsResponse>> call = callInstructions.callInstructions(
                recipeId, context.getString(R.string.api_key)
        );

        call.enqueue(new Callback<List<InstructionsResponse>>() {
            @Override
            public void onResponse(Call<List<InstructionsResponse>> call, retrofit2.Response<List<InstructionsResponse>> response) {
                if (!response.isSuccessful()) {
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

                    try {
                        cacheInstructions(recipeId, instructions);
                    } catch (Exception e) {
                        e.getMessage();
                    }

                    listener.didFetch(instructions, "Fetched from API and cached");
                } else {
                    listener.didError("No instructions available from API");
                }
            }

            @Override
            public void onFailure(Call<List<InstructionsResponse>> call, Throwable t) {

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
                id, "5", context.getString(R.string.api_key)
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
    private void cacheCompleteRecipeDetails(RecipeDetailsResponse details, String userEmail) {
        try {
            if (details == null || details.title == null) {
                return;
            }


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
                cacheIngredientsForRecipe(details.id, details.extendedIngredients);
            }
        } catch (Exception e) {
            throw e;
        }
    }

    private void cacheIngredientsForRecipe(int recipeId, List<ExtendedIngredient> ingredients) {
        try {
            dbHelper.deleteIngredientsForRecipe(recipeId);
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
        } catch (Exception e) {
            throw e;
        }
    }

    private void cacheInstructions(int recipeId, List<InstructionsResponse> instructionsResponses) {
        try {
            if (instructionsResponses == null || instructionsResponses.isEmpty()) {
                return;
            }
            int deletedCount = dbHelper.deleteInstructionsForRecipe(recipeId);

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


        } catch (Exception e) {
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