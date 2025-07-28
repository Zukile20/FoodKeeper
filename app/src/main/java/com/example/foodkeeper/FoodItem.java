package com.example.foodkeeper;

public class FoodItem {
    private String name;
    private String category;
    private String expiryDate;
    private int quantity;

    public FoodItem(String name, String category, String expiryDate, int quantity){
        this.name = name;
        this.category = category;
        this.expiryDate = expiryDate;
        this.quantity = quantity;
    }

}
