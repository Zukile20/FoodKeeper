package com.example.foodkeeper.Fridge.models;

public class Fridge {
    private String brand, model, description;
    private int size, id;
    private boolean isLoggedIn;
    private byte[] image;

    public Fridge(String brand, String model, String description, int size, boolean isLoggedIn, byte[] image) {
        this.brand = brand;
        this.model = model;
        this.description = description;
        this.size = size;
        this.isLoggedIn = isLoggedIn;
        this.image = image;
    }

    public Fridge() {

    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getBrand() {
        return brand;
    }

    public boolean isLoggedIn() {
        return isLoggedIn;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public void setLoggedIn(boolean loggedIn) {
        isLoggedIn = loggedIn;
    }

    public byte[] getImage() {
        return image;
    }

    public void setImage(byte[] image) {
        this.image = image;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}