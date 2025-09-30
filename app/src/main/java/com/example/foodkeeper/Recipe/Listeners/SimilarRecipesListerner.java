package com.example.foodkeeper.Recipe.Listeners;


import com.example.foodkeeper.Recipe.Models.SimilarRecipeResponse;

import java.util.List;

public interface SimilarRecipesListerner {
    void didFetch(List<SimilarRecipeResponse> response, String message);
    void didError(String massage);

}
