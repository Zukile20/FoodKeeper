package com.example.foodkeeper.Recipe.Listeners;

import com.example.foodkeeper.Recipe.Models.IngredientsResponse;

import java.util.List;

public interface IngredientsListener {
    void didFetch(List<IngredientsResponse> response, String message);
    void didError(String message);
}
