package com.example.foodkeeper.Recipe.Models;

import java.util.ArrayList;

public class RecipeDetailsResponse {
    public int id;
    public String title;
    public String image;
    public ArrayList<ExtendedIngredient> extendedIngredients;

    public int aggregateLikes;
    public int readyInMinutes;
    public int servings;
    public int fav;

}
