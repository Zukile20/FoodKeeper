package com.example.foodkeeper.Recipe;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.foodkeeper.FoodkeeperUtils.Database;
import com.example.foodkeeper.R;
import com.example.foodkeeper.Recipe.Adapters.InstructionsAdapter;
import com.example.foodkeeper.Recipe.Adapters.SimilarRecipeAdapter;
import com.example.foodkeeper.Recipe.Adapters.ingredientsAdapter;
import com.example.foodkeeper.Recipe.Listeners.InstructionsListener;
import com.example.foodkeeper.Recipe.Listeners.RecipeClickListerner;
import com.example.foodkeeper.Recipe.Listeners.RecipeDetailsListener;
import com.example.foodkeeper.Recipe.Listeners.SimilarRecipesListerner;
import com.example.foodkeeper.Recipe.Models.ExtendedIngredient;
import com.example.foodkeeper.Recipe.Models.InstructionsResponse;
import com.example.foodkeeper.Recipe.Models.Recipe;
import com.example.foodkeeper.Recipe.Models.RecipeDetailsResponse;
import com.example.foodkeeper.Recipe.Models.SimilarRecipeResponse;
import com.example.foodkeeper.Recipe.Models.Step;
import com.example.foodkeeper.Recipe.Models.TTSHelper;
import com.example.foodkeeper.Register.SessionManager;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class RecipeDetailsActivity extends AppCompatActivity implements TTSHelper.TTSListener {
    int id;
    ImageView mealImage, favButton;
    TextView textView_meal_name;
    RecyclerView recycler_meal_ingredients, recycler_meal_instructions, recycler_meal_similar;
    RequestManager manager;
    ProgressDialog dialog;
    ingredientsAdapter adapter;
    InstructionsAdapter instructionsAdapter;
    SimilarRecipeAdapter similarRecipeAdapter;
    private Database dbHelper;
    private boolean isFavorite = false;
    private TTSHelper ttsHelper;
    private ImageView btnPlayPause;

    private volatile boolean isPlaying = false;
    private volatile boolean isPaused = false;
    private volatile boolean isTTSReady = false;

    private RecipeDetailsResponse currentRecipe;
    private List<InstructionsResponse> currentInstructions;

    private List<String> textSegments;
    private int currentSegmentIndex = 0;
    private boolean isReadingComplete = false;

    private SessionManager sessionManager;
    private String userEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe_details);

        sessionManager = new SessionManager(this);
        userEmail = sessionManager.getUserEmail();

        if (userEmail == null || userEmail.isEmpty()) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        findViews();
        initializeTTS();
        setupTTSButtons();

        dbHelper = new Database(this);

        String idString = getIntent().getStringExtra("id");
        if (idString == null || idString.isEmpty()) {
            Toast.makeText(this, "Invalid recipe ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        try {
            id = Integer.parseInt(idString);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid recipe ID format", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize progress dialog but don't show it yet
        dialog = new ProgressDialog(this);
        dialog.setTitle("Loading Details...");
        dialog.setCancelable(false);

        manager = new RequestManager(this);
        manager.getRecipeDetails(recipeDetailsListener, id);
        manager.getInstructions(instructionsListener, id);
        manager.getSimilarRecipes(similarRecipesListerner, id);
    }

    private void findViews() {
        textView_meal_name = findViewById(R.id.textView_meal_name);
        mealImage = findViewById(R.id.mealImage);
        recycler_meal_ingredients = findViewById(R.id.recycler_meal_ingredients);
        recycler_meal_similar = findViewById(R.id.recycler_meal_similar);
        recycler_meal_instructions = findViewById(R.id.recycler_meal_instructions);
        favButton = findViewById(R.id.favButton);
        findViewById(R.id.backBtn).setOnClickListener(event -> finish());

        btnPlayPause = findViewById(R.id.btn_play);
        btnPlayPause.setEnabled(false);
        btnPlayPause.setAlpha(0.5f);
    }

    private void initializeTTS() {
        if (TTSHelper.isTTSAvailable(this)) {
            ttsHelper = new TTSHelper(this, this);
        } else {
            Toast.makeText(this, "Text-to-Speech not available on this device", Toast.LENGTH_LONG).show();
            btnPlayPause.setVisibility(View.GONE);
        }
    }

    private void setupTTSButtons() {
        btnPlayPause.setOnClickListener(v -> {
            if (isPlaying) {
                pauseRecipeReading();
            } else {
                if (isPaused) {
                    resumeRecipeReading();
                } else {
                    startRecipeReading();
                }
            }
        });
    }

    private void startRecipeReading() {
        if (ttsHelper == null || !isTTSReady) {
            Toast.makeText(this, "Text-to-Speech not ready", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentRecipe == null || currentInstructions == null) {
            Toast.makeText(this, "Recipe data not loaded yet", Toast.LENGTH_SHORT).show();
            return;
        }

        buildTextSegments();

        if (textSegments != null && !textSegments.isEmpty()) {
            currentSegmentIndex = 0;
            isReadingComplete = false;
            isPlaying = true;
            isPaused = false;
            updatePlayButtonToPause();
            speakCurrentSegment();
            Toast.makeText(this, "Reading complete recipe...", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "No recipe content to read", Toast.LENGTH_SHORT).show();
        }
    }

    private void pauseRecipeReading() {
        if (ttsHelper != null && isPlaying) {
            ttsHelper.stop();
            isPlaying = false;
            isPaused = true;
            updatePlayButtonToPlay();
            Toast.makeText(this, "Recipe reading paused", Toast.LENGTH_SHORT).show();
        }
    }

    private void resumeRecipeReading() {
        if (ttsHelper != null && isPaused && !isReadingComplete) {
            isPlaying = true;
            isPaused = false;
            updatePlayButtonToPause();
            speakCurrentSegment();
            Toast.makeText(this, "Resuming recipe reading...", Toast.LENGTH_SHORT).show();
        }
    }

    private void buildTextSegments() {
        textSegments = new ArrayList<>();

        if (currentRecipe != null && currentRecipe.title != null) {
            textSegments.add("Recipe: " + currentRecipe.title + ". Let's start cooking!");
        }

        if (currentRecipe != null && currentRecipe.extendedIngredients != null &&
                !currentRecipe.extendedIngredients.isEmpty()) {
            textSegments.add("First, let's gather our ingredients.");

            for (ExtendedIngredient ingredient : currentRecipe.extendedIngredients) {
                if (ingredient != null && ingredient.name != null && !ingredient.name.isEmpty()) {
                    textSegments.add(ingredient.name);
                }
            }
            textSegments.add("Now, let's start with the cooking instructions.");
        }

        if (currentInstructions != null && !currentInstructions.isEmpty()) {
            int stepNumber = 1;
            for (InstructionsResponse instruction : currentInstructions) {
                if (instruction.steps != null && !instruction.steps.isEmpty()) {
                    for (Step step : instruction.steps) {
                        if (step.step != null && !step.step.isEmpty()) {
                            String cleanStep = step.step.replaceAll("<[^>]*>", "").trim();
                            textSegments.add("Step " + stepNumber + ": " + cleanStep);
                            stepNumber++;
                        }
                    }
                }
            }
        }

        textSegments.add("Your delicious meal is ready! Enjoy your cooking!");
    }

    private void speakCurrentSegment() {
        if (textSegments != null && currentSegmentIndex < textSegments.size() && ttsHelper != null) {
            String textToSpeak = textSegments.get(currentSegmentIndex);
            ttsHelper.speak(textToSpeak);
        }
    }

    private void moveToNextSegment() {
        currentSegmentIndex++;
        if (currentSegmentIndex < textSegments.size()) {
            speakCurrentSegment();
        } else {
            onAllSegmentsComplete();
        }
    }

    private void onAllSegmentsComplete() {
        isPlaying = false;
        isPaused = false;
        isReadingComplete = true;
        currentSegmentIndex = 0;
        updatePlayButtonToPlay();
        Toast.makeText(this, "Recipe reading completed!", Toast.LENGTH_SHORT).show();
    }

    private void updatePlayButtonToPlay() {
        runOnUiThread(() -> btnPlayPause.setImageResource(R.drawable.play));
    }

    private void updatePlayButtonToPause() {
        runOnUiThread(() -> btnPlayPause.setImageResource(R.drawable.pause));
    }

    private void toggleFavorite() {
        if (currentRecipe == null) {
            Toast.makeText(this, "Recipe not loaded", Toast.LENGTH_SHORT).show();
            return;
        }

        isFavorite = !isFavorite;
        updateFavoriteButton();

        boolean success = dbHelper.toggleFavorite(currentRecipe.id, isFavorite, userEmail);

        if (success) {
            showFavoriteToast();
        } else {
            // Revert on failure
            isFavorite = !isFavorite;
            updateFavoriteButton();
            Toast.makeText(this, "Failed to update favorite status", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateFavoriteButton() {
        if (isFavorite) {
            favButton.setImageResource(R.drawable.heart_selected);
        } else {
            favButton.setImageResource(R.drawable.heart_unselected);
        }
    }

    private boolean loadFavoriteState(int recipeId) {
        try {
            List<Recipe> favoriteRecipes = dbHelper.getFavoriteRecipes(userEmail);
            for (Recipe recipe : favoriteRecipes) {
                if (recipe.id == recipeId) {
                    return true;
                }
            }
        } catch (Exception e) {
            // Handle error silently
        }
        return false;
    }

    private void showFavoriteToast() {
        String message = isFavorite ? "Added to favorites!" : "Removed from favorites!";
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void saveRecipeToDatabase(RecipeDetailsResponse response) {
        if (response == null) return;

        try {
            dbHelper.insertRecipe(
                    response.id,
                    response.title,
                    response.image,
                    response.aggregateLikes,
                    response.readyInMinutes,
                    response.servings,
                    response.fav,
                    userEmail
            );

            if (!dbHelper.hasIngredientsForRecipe(response.id) &&
                    response.extendedIngredients != null) {
                for (ExtendedIngredient ingredient : response.extendedIngredients) {
                    if (ingredient != null && ingredient.name != null && !ingredient.name.isEmpty()) {
                        long ingredientId = dbHelper.insertIngredient(ingredient.name);
                        if (ingredientId > 0) {
                            dbHelper.insertRecipeIngredient(response.id, ingredientId);
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Handle error silently or log it
        }
    }

    private void saveInstructionsToDatabase(int recipeId, List<InstructionsResponse> instructions) {
        if (instructions == null) return;

        try {
            for (InstructionsResponse instructionBlock : instructions) {
                if (instructionBlock.steps != null) {
                    for (Step step : instructionBlock.steps) {
                        if (step.step != null && !step.step.isEmpty()) {
                            dbHelper.insertInstruction(recipeId, step.number, step.step);
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Handle error silently or log it
        }
    }

    @Override
    public void onTTSReady() {
        runOnUiThread(() -> {
            isTTSReady = true;
            btnPlayPause.setEnabled(true);
            btnPlayPause.setAlpha(1.0f);
        });
    }

    @Override
    public void onTTSError(String error) {
        runOnUiThread(() -> {
            Toast.makeText(this, "TTS Error: " + error, Toast.LENGTH_SHORT).show();
            isPlaying = false;
            updatePlayButtonToPlay();
        });
    }

    @Override
    public void onSpeechStart() {
        runOnUiThread(() -> {
            isPlaying = true;
            updatePlayButtonToPause();
        });
    }

    @Override
    public void onSpeechComplete() {
        runOnUiThread(() -> {
            if (!isPaused) {
                moveToNextSegment();
            }
        });
    }

    private final RecipeDetailsListener recipeDetailsListener = new RecipeDetailsListener() {
        @Override
        public void didFetch(RecipeDetailsResponse response, String message) {
            // Only show dialog if loading from API
            if (message != null && message.contains("API")) {
                if (dialog != null && !dialog.isShowing()) {
                    dialog.show();
                }
            }

            // Dismiss dialog when done
            if (dialog != null && dialog.isShowing()) {
                dialog.dismiss();
            }

            currentRecipe = response;
            saveRecipeToDatabase(response);

            isFavorite = loadFavoriteState(response.id);
            updateFavoriteButton();

            textView_meal_name.setText(response.title);
            Picasso.get().load(response.image).into(mealImage);

            recycler_meal_ingredients.setHasFixedSize(true);
            recycler_meal_ingredients.setLayoutManager(
                    new LinearLayoutManager(RecipeDetailsActivity.this, LinearLayoutManager.VERTICAL, false)
            );
            adapter = new ingredientsAdapter(RecipeDetailsActivity.this, response.extendedIngredients);
            recycler_meal_ingredients.setAdapter(adapter);

            favButton.setOnClickListener(v -> {
                if (currentRecipe != null) {
                    toggleFavorite();
                }
            });
        }

        @Override
        public void didError(String message) {
            if (dialog != null && dialog.isShowing()) {
                dialog.dismiss();
            }
            Toast.makeText(RecipeDetailsActivity.this, message, Toast.LENGTH_SHORT).show();
        }
    };

    private final SimilarRecipesListerner similarRecipesListerner = new SimilarRecipesListerner() {
        @Override
        public void didFetch(List<SimilarRecipeResponse> response, String message) {
            recycler_meal_similar.setHasFixedSize(true);
            recycler_meal_similar.setLayoutManager(
                    new LinearLayoutManager(RecipeDetailsActivity.this, LinearLayoutManager.HORIZONTAL, false)
            );
            similarRecipeAdapter = new SimilarRecipeAdapter(
                    RecipeDetailsActivity.this, response, recipeClickListerner
            );
            recycler_meal_similar.setAdapter(similarRecipeAdapter);
        }

        @Override
        public void didError(String message) {
            Toast.makeText(RecipeDetailsActivity.this, message, Toast.LENGTH_SHORT).show();
        }
    };

    private final RecipeClickListerner recipeClickListerner = new RecipeClickListerner() {
        @Override
        public void onRecipeClicked(String id) {
            startActivity(new Intent(RecipeDetailsActivity.this, RecipeDetailsActivity.class)
                    .putExtra("id", id));
        }
    };

    private final InstructionsListener instructionsListener = new InstructionsListener() {
        @Override
        public void didFetch(List<InstructionsResponse> response, String message) {
            currentInstructions = response;

            if (!dbHelper.hasInstructionsForRecipe(id) && response != null) {
                saveInstructionsToDatabase(id, response);
            }

            recycler_meal_instructions.setHasFixedSize(true);
            recycler_meal_instructions.setLayoutManager(
                    new LinearLayoutManager(RecipeDetailsActivity.this, LinearLayoutManager.VERTICAL, false)
            );
            instructionsAdapter = new InstructionsAdapter(RecipeDetailsActivity.this, response);
            recycler_meal_instructions.setAdapter(instructionsAdapter);
        }

        @Override
        public void didError(String message) {
            Toast.makeText(RecipeDetailsActivity.this, message, Toast.LENGTH_SHORT).show();
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (ttsHelper != null) {
            ttsHelper.shutdown();
        }
        if (dbHelper != null) {
            dbHelper.close();
        }
        if (dialog != null) {
            dialog.dismiss();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isPlaying) {
            pauseRecipeReading();
        }
    }
}