package com.example.foodkeeper.Meal;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Meal implements Parcelable {
    private long mealID;
    private String mealName;
    private String url;
    private long fridgeID;
    private String mealType;

    private LocalDate lastUsed;

    private ArrayList<String> foodItemIDs = new ArrayList<>();

    public void setFoodItemIDs(List<String> foodItemsIDs) {
        this.foodItemIDs.clear(); // Clear existing items first
        if (foodItemsIDs != null) {
            this.foodItemIDs.addAll(foodItemsIDs);
        }
    }

    public ArrayList<String> getFoodItemIDs() {
        return foodItemIDs;
    }

    public Meal(long MealID, String name, String imageUrl,long fridgeID) {
        this.mealID = MealID;
        this.mealName = name;
        this.url =imageUrl;
        this.lastUsed = null;
        this.fridgeID= fridgeID;
        this.foodItemIDs = new ArrayList<>(); // Initialize here too
    }
    public Meal( String name, String url,long fridgeID) {
        this.mealName = name;
        this.url =url;
        this.lastUsed = null;
        this.fridgeID= fridgeID;
        this.foodItemIDs = new ArrayList<>(); // Initialize here too
    }

    protected Meal(Parcel in) {
        mealID = in.readLong();
        mealName = in.readString();
        url = in.readString();
        lastUsed = LocalDate.parse(in.readString());
        foodItemIDs = new ArrayList<>();
        in.readStringList(foodItemIDs);

    }
    public static final Creator<Meal> CREATOR = new Creator<Meal>() {
        @Override
        public Meal createFromParcel(Parcel in) {
            return new Meal(in);
        }

        @Override
        public Meal[] newArray(int size) {
            return new Meal[size];
        }
    };

    public LocalDate getLastUsed() {
        return lastUsed;
    }


    public String getUri() {
        return url;
    }

    public String getMealName() {
        return mealName;
    }

    public long getMealID() {
        return mealID;
    }

    public void setMealID(long mealID) {
        this.mealID = mealID;
    }

    public void setMealName(String mealName) {
        this.mealName = mealName;
    }

    public void setUrl(String url) {
        this.url = url;
    }


    public String getMealType() {
        return mealType;
    }

    public void setMealType(String mealType) {
        this.mealType = mealType;
    }

    public void setLastUsed(LocalDate lastUsed) {
        this.lastUsed = lastUsed;
    }

    @Override
    public int describeContents() {
        return 0;
    }


    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeLong(mealID);
        dest.writeString(mealName);
        dest.writeString(url);
        dest.writeStringList(foodItemIDs);
    }



    @Override
    public String toString() {
        return "Meal{" +
                "mealID=" + mealID +
                ", mealName='" + mealName + '\'' +
                ", foodItemIDs=" + foodItemIDs +
                '}';
    }
}
