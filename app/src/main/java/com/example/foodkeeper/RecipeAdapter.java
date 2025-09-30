package com.example.foodkeeper;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.foodkeeper.Recipe.Listeners.RecipeClickListerner;
import com.example.foodkeeper.Recipe.Models.Recipe;
import com.squareup.picasso.Picasso;

import java.util.List;

public class RecipeAdapter extends RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder> {
    private List<Recipe> recipes;
    private RecipeClickListerner listener;
    private Context context;

    public RecipeAdapter(Context context, List<Recipe> recipes, RecipeClickListerner listener) {
        this.context = context;
        this.recipes = recipes;
        this.listener = listener;
    }

    @NonNull
    @Override
    public RecipeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recipe, parent, false);
        return new RecipeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecipeViewHolder holder, int position) {
        Recipe recipe = recipes.get(position);

        // Set recipe data
        holder.recipeName.setText(recipe.title);
        holder.recipeName.setSelected(true);

        // Load image with Picasso
        Picasso.get()
                .load(recipe.image)
                .into(holder.recipeImage);

        // Set click listener
        holder.random_list_container.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRecipeClicked(String.valueOf(recipe.id));
            }
        });
    }

    @Override
    public int getItemCount() {
        return recipes != null ? recipes.size() : 0;
    }

    // Method to update recipes list
    public void updateRecipes(List<Recipe> newRecipes) {
        this.recipes = newRecipes;
        notifyDataSetChanged();
    }

    class RecipeViewHolder extends RecyclerView.ViewHolder {
        CardView random_list_container;
        ImageView recipeImage;
        TextView recipeName;

        public RecipeViewHolder(@NonNull View itemView) {
            super(itemView);
            random_list_container = itemView.findViewById(R.id.random_container);
            recipeImage = itemView.findViewById(R.id.recipe_image);
            recipeName = itemView.findViewById(R.id.recipe_name);
        }
    }
}