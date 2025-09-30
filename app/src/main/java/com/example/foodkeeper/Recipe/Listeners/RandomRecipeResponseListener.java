package com.example.foodkeeper.Recipe.Listeners;


import com.example.foodkeeper.Recipe.Models.RandomRecipeApiResponse;

public interface RandomRecipeResponseListener {

    void didFetch(RandomRecipeApiResponse response, String message);
    void didError(String massage);
}
