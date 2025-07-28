package com.example.foodkeeper;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

import java.util.ArrayList;

public class Database extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "FoodKeeper.db";
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_USERS = "users";
    private static final String TABLE_FOOD_ITEMS = "food_items";
    private static final String KEY_ID = "id";

    private static final String KEY_NAME = "name";
    private static final String KEY_SURNAME = "surname";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_PHONE = "phone";
    private static final String KEY_PASSWORD = "password";

    private static final String KEY_ITEM_NAME = "item_name";
    private static final String KEY_CATEGORY = "category";
    private static final String KEY_EXPIRY_DATE = "expiry_date";
    private static final String KEY_QUANTITY = "quantity";
    private static final String KEY_USER_EMAIL = "userEmail";

    public Database(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_USERS_TABLE = "CREATE TABLE " + TABLE_USERS + "("
                + KEY_NAME + " TEXT,"
                + KEY_SURNAME + " TEXT,"
                + KEY_EMAIL + " TEXT PRIMARY KEY,"
                + KEY_PHONE + " INTEGER,"
                + KEY_PASSWORD + " TEXT)";
        db.execSQL(CREATE_USERS_TABLE);

        String CREATE_FOOD_ITEMS_TABLE = "CREATE TABLE " + TABLE_FOOD_ITEMS + "("
                + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_ITEM_NAME + " TEXT,"
                + KEY_CATEGORY + " TEXT,"
                + KEY_EXPIRY_DATE + " TEXT,"
                + KEY_QUANTITY + "INTEGER,"
                + KEY_USER_EMAIL + "TEXT) ";
        db.execSQL(CREATE_FOOD_ITEMS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_FOOD_ITEMS);
        onCreate(db);
    }
    public void register(String name, String surname, String email, Integer phone, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(KEY_NAME, name);
        cv.put(KEY_SURNAME, surname);
        cv.put(KEY_EMAIL, email);
        cv.put(KEY_PHONE, phone);
        cv.put(KEY_PASSWORD, password);
        db.insert(TABLE_USERS, null, cv);
        db.close();
    }
    public int login(String email, String password) {
        int result = 0;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM " + TABLE_USERS + " WHERE "
                        + KEY_EMAIL + "=? AND " + KEY_PASSWORD + "=?",
                new String[]{email, password});
        if(c.moveToFirst()) {
            result = 1;
        }
        c.close();
        return result;
    }
    public long addFoodItem(String name, String category, String expiryDate, int quantity, String userEmail) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("category", category);
        values.put("expiry_date", expiryDate);
        values.put("quantity", quantity);
        values.put("user_email", userEmail);
        Long result = db.insert("food_items_table", null, values);
        db.close();

        return result;
    }

    public Cursor getAllFoodItems(String userEmail) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_FOOD_ITEMS + " WHERE "
                + KEY_USER_EMAIL + "=?", new String[]{userEmail});
    }

    public Cursor getItemsByCategory(String category, String userEmail) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_FOOD_ITEMS + " WHERE "
                        + KEY_CATEGORY + "=? AND " + KEY_USER_EMAIL + "=?",
                new String[]{category, userEmail});
    }

    public ArrayList<String> getAllCategories() {
        ArrayList<String> categories = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT DISTINCT category FROM food_items_table", null);

        if (cursor.moveToFirst()) {
            do {
                String category = cursor.getString(0);
                if (category != null && !category.isEmpty()) {
                    categories.add(category);
                }
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return categories;
    }
}