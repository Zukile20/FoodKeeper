package com.example.foodkeeper.Recipe.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;


import com.example.foodkeeper.Database;
import com.example.foodkeeper.R;
import com.example.foodkeeper.Recipe.Listeners.RecipeClickListerner;
import com.example.foodkeeper.Recipe.Models.Recipe;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class RandomRecipeAdapter extends RecyclerView.Adapter<RandomRecipeViewHolder> {
    Context contex;
    List<Recipe> list;
    RecipeClickListerner listerner;
    Database db; // Move database helper to class level
    Boolean infavourite=true;
    public RandomRecipeAdapter(Context contex, List<Recipe> list, RecipeClickListerner listerner) {
        this.contex = contex;
        this.list = list;
        this.listerner = listerner;
        this.db = new Database(contex);
        checkState();
        // Initialize once
    }

    @NonNull
    @Override
    public RandomRecipeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new RandomRecipeViewHolder(LayoutInflater.from(contex).inflate(R.layout.recipe_list_view, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RandomRecipeViewHolder holder, int position) {
        Recipe recipe = list.get(position);

        // Set recipe data
        holder.RecipeName.setText(recipe.title);
        holder.RecipeName.setSelected(true);
        holder.textView_likes.setText(recipe.aggregateLikes + " Likes");
        holder.textView_servings.setText(recipe.servings + " Servings");
        holder.textView_time.setText(recipe.readyInMinutes + " Minutes");
        Picasso.get().load(recipe.image).into(holder.imageView_food);

        // Set up click listener for recipe
        holder.random_list_container.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listerner.onRecipeClicked(String.valueOf(recipe.id));
            }
        });

        // Set up favorite button
        updateFavoriteButton(holder, recipe);

        holder.favButton.setOnClickListener(v -> {
            // Toggle favorite status
            boolean newFavoriteStatus = recipe.fav != 1;

            // Update database
            boolean success = db.toggleFavorite(recipe.id, newFavoriteStatus);

            if (success) {
                // Update the recipe object
                recipe.fav = newFavoriteStatus ? 1 : 0;

                // Update the UI
                updateFavoriteButton(holder, recipe);
                String message =  newFavoriteStatus? "Added to favorites!" : "Removed from favorites!";
                Toast.makeText(contex, message, Toast.LENGTH_SHORT).show();
                if(infavourite)
                {
                    updateRecipes();
                }
            }
        });
    }
    public void checkState() {
        for(Recipe recipe : list) {
            if (recipe.fav != 1) {
                infavourite = false;
                break;
            }
        }
    }
    public void updateRecipes() {
        List<Recipe> newList = new ArrayList<>();
        for(Recipe recipe : list) {
            if (recipe.fav == 1) {
               newList.add(recipe);
            }
        }
        list= newList;
        notifyDataSetChanged();
    }

    private void updateFavoriteButton(RandomRecipeViewHolder holder, Recipe recipe) {
        holder.favButton.setImageResource(recipe.fav == 1 ?
                R.drawable.heart_selected : R.drawable.heart_unselected);
    }

    @Override
    public int getItemCount() {
        return list.size();
    }
}

class RandomRecipeViewHolder extends RecyclerView.ViewHolder {
    CardView random_list_container;
    TextView RecipeName, textView_servings, textView_time, textView_likes;
    ImageView imageView_food, favButton;

    public RandomRecipeViewHolder(@NonNull View itemView) {
        super(itemView);
        random_list_container = itemView.findViewById(R.id.random_list_container);
        RecipeName = itemView.findViewById(R.id.RecipeName);
        textView_servings = itemView.findViewById(R.id.textView_servings);
        textView_time = itemView.findViewById(R.id.textView_time);
        textView_likes = itemView.findViewById(R.id.textView_likes);
        imageView_food = itemView.findViewById(R.id.imageView_food);
        favButton = itemView.findViewById(R.id.favButton);
    }
}