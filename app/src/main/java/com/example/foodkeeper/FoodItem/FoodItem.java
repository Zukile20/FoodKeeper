package com.example.foodkeeper.FoodItem;

import androidx.annotation.NonNull;

import java.io.Serializable;

public class FoodItem implements Serializable {
    private int id;
    private String name;
    private String category;
    private String categoryName;
    private String expiryDate;
    private int quantity;
    private byte[] image;
    private int isInShopList;
    private boolean checked;

    public FoodItem(String name, String category, String expiryDate, int quantity, byte[] image) {
        this.name = name;
        this.category = category;
        this.expiryDate = expiryDate;
        this.quantity = quantity;
        this.image = image;
        isInShopList =0;
    }

    public FoodItem(int id, String name, String category, String expiryDate, int quantity, byte[] image,int shoppingList) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.expiryDate = expiryDate;
        this.quantity = quantity;
        this.image = image;
        this.isInShopList = shoppingList;
    }

    public FoodItem() {

    }
    public int getId() {
        return id;
    }
    public String getName(){
        return name;
    }
    public String getExpiryDate() {
        return expiryDate;
    }
    public String getCategory() {
        return category;
    }
    public int getQuantity() {
        return quantity;
    }
    public byte[] getImage() {
        return image;
    }
    public void setId(int id) {
        this.id = id;
    }
    public void setName(String name) {
        this.name = name;
    }
    public void setCategory(String category) {
        this.category = category;
    }
    public void setExpiryDate(String date) {
        this.expiryDate = date;
    }
    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
    public void setImage(byte[] image) {
        this.image = image;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public int getIsInShopList() {
        return isInShopList;
    }

    public void setIsInShopList(int isInShopList) {
        this.isInShopList = isInShopList;
    }
    public boolean getCheckState() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }

    @NonNull
    @Override
    public String toString() {
        return "● "+ name;
    }
}
