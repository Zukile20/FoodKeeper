package com.example.foodkeeper.Recipe.Models;

public class Recipe {
    public int id;
    public String title;
    public String image;
    public int readyInMinutes;
    public int servings;
    public int aggregateLikes;

    public int fav;

    public Recipe(int id, String title, String image, int aggregateLikes, int readyInMinutes, int servings,int fav) {
        this.id = id;
        this.title = title;
        this.image = image;
        this.readyInMinutes = readyInMinutes;
        this.servings = servings;
        this.aggregateLikes = aggregateLikes;
        this.fav=fav;
    }
}
