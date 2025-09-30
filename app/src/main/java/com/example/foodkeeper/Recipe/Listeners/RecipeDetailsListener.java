package com.example.foodkeeper.Recipe.Listeners;


import com.example.foodkeeper.Recipe.Models.RecipeDetailsResponse;

public interface RecipeDetailsListener {

    void didFetch(RecipeDetailsResponse response, String massage );
    void didError(String massage);
}
