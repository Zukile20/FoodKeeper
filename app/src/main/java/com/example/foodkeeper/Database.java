package com.example.foodkeeper;

import static androidx.room.RoomMasterTable.TABLE_NAME;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.widget.Toast;

import com.example.foodkeeper.FoodItem.FoodItem;
import com.example.foodkeeper.Fridge.Fridge;
import com.example.foodkeeper.Meal.Meal;
import com.example.foodkeeper.MealPlan.CalendarUtils;
import com.example.foodkeeper.MealPlan.MealPlan;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Database extends SQLiteOpenHelper {
    private Context context;
    private static final String DATABASE_NAME = "FoodKeeper.db";
    private static final int DATABASE_VERSION = 16; // Increment version for schema changes
    private static Database instance;

    private static final String TABLE_USERS = "users";
    private static final String TABLE_FOOD_ITEMS = "food_items";
    private static final String TABLE_FRIDGES = "fridges";

    private static final String KEY_ID = "id";
    private static final String KEY_NAME = "name";
    private static final String KEY_SURNAME = "surname";
    public static final String KEY_EMAIL = "email";
    private static final String KEY_PHONE = "phone";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_PROFILE = "profile_image";

    // Food items - now belong to fridges only
    public static final String KEY_ITEM_NAME = "item_name";
    public static final String KEY_CATEGORY = "category";
    public static final String KEY_EXPIRY_DATE = "expiry_date";
    public static final String KEY_QUANTITY = "quantity";
    public static final String KEY_FOOD_IMAGE = "food_image";
    public static final String KEY_IS_IN_SHOPPING_LIST = "isInShoppingList";
    public static final String KEY_FRIDGE_ID = "fridge_id"; // Foreign key to fridges

    // Fridges - now belong to users
    private static final String KEY_BRAND = "brand";
    private static final String KEY_MODEL = "model";
    private static final String KEY_DESCRIPTION = "description";
    private static final String KEY_SIZE = "size";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_FRIDGE_IMAGE = "fridge_image";
    public static final String KEY_USER_EMAIL = "user_email"; // Foreign key to users

    SessionManager userSession;

    public Database(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createTables(db);
        userSession = new SessionManager(context);
    }

    private void fridgeUserItemTables(SQLiteDatabase db) {
        // Users table (unchanged)
        String CREATE_USERS_TABLE = "CREATE TABLE " + TABLE_USERS + "("
                + KEY_NAME + " TEXT,"
                + KEY_SURNAME + " TEXT,"
                + KEY_EMAIL + " TEXT PRIMARY KEY,"
                + KEY_PHONE + " INTEGER,"
                + KEY_PASSWORD + " TEXT,"
                + KEY_PROFILE + " BLOB)";
        db.execSQL(CREATE_USERS_TABLE);

        // Fridges table - now with user ownership
        String CREATE_FRIDGES_TABLE = "CREATE TABLE " + TABLE_FRIDGES + "("
                + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_BRAND + " TEXT,"
                + KEY_MODEL + " TEXT,"
                + KEY_DESCRIPTION + " TEXT,"
                + KEY_SIZE + " INTEGER,"
                + KEY_IS_LOGGED_IN + " INTEGER DEFAULT 0,"
                + KEY_FRIDGE_IMAGE + " BLOB,"
                + KEY_USER_EMAIL + " TEXT NOT NULL,"
                + "FOREIGN KEY(" + KEY_USER_EMAIL + ") REFERENCES " + TABLE_USERS + "(" + KEY_EMAIL + ") ON DELETE CASCADE)";
        db.execSQL(CREATE_FRIDGES_TABLE);

        // Food items table - now belongs to fridges only
        String CREATE_FOOD_ITEMS_TABLE = "CREATE TABLE " + TABLE_FOOD_ITEMS + "("
                + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_ITEM_NAME + " TEXT,"
                + KEY_CATEGORY + " TEXT,"
                + KEY_EXPIRY_DATE + " TEXT,"
                + KEY_QUANTITY + " INTEGER,"
                + KEY_FOOD_IMAGE + " BLOB,"
                + KEY_IS_IN_SHOPPING_LIST + " INTEGER,"
                + KEY_FRIDGE_ID + " INTEGER NOT NULL,"
                + "FOREIGN KEY(" + KEY_FRIDGE_ID + ") REFERENCES " + TABLE_FRIDGES + "(" + KEY_ID + ") ON DELETE CASCADE)";
        db.execSQL(CREATE_FOOD_ITEMS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop all tables in reverse dependency order
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_FOOD_ITEMS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_FRIDGES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        db.execSQL("DROP TABLE IF EXISTS MealPlan");
        db.execSQL("DROP TABLE IF EXISTS MealFoodItem");
        db.execSQL("DROP TABLE IF EXISTS Meal");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_INSTRUCTIONS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_RECIPES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_INGREDIENTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_RECIPE_INGREDIENTS);
        db.execSQL("DROP TABLE IF EXISTS " + SHOPPING_TABLE_NAME);

        onCreate(db);
    }

    // USER METHODS (unchanged)
    public void register(String name, String surname, String email, Integer phone, String password, byte[] profileImage) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(KEY_NAME, name);
        cv.put(KEY_SURNAME, surname);
        cv.put(KEY_EMAIL, email);
        cv.put(KEY_PHONE, phone);
        cv.put(KEY_PASSWORD, password);
        cv.put(KEY_PROFILE, profileImage);
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

    // FRIDGE METHODS - Updated for user ownership
    public long addFridge(Fridge fridge, String userEmail) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_BRAND, fridge.getBrand());
        values.put(KEY_MODEL, fridge.getModel());
        values.put(KEY_DESCRIPTION, fridge.getDescription());
        values.put(KEY_SIZE, fridge.getSize());
        values.put(KEY_IS_LOGGED_IN, fridge.isLoggedIn() ? 1 : 0);
        values.put(KEY_USER_EMAIL, userEmail);

        if (fridge.getImage() != null) {
            values.put(KEY_FRIDGE_IMAGE, fridge.getImage());
        }

        long result = db.insert(TABLE_FRIDGES, null, values);
        db.close();

        if (result == -1) {
            Toast.makeText(context, "Failed to add fridge", Toast.LENGTH_SHORT).show();
        } else {
            fridge.setId((int) result);
            Toast.makeText(context, "Fridge added successfully", Toast.LENGTH_SHORT).show();
        }
        return result;
    }

    public boolean updateFridge(Fridge fridge, String userEmail) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_BRAND, fridge.getBrand());
        values.put(KEY_MODEL, fridge.getModel());
        values.put(KEY_DESCRIPTION, fridge.getDescription());
        values.put(KEY_SIZE, fridge.getSize());
        values.put(KEY_IS_LOGGED_IN, fridge.isLoggedIn() ? 1 : 0);

        if (fridge.getImage() != null) {
            values.put(KEY_FRIDGE_IMAGE, fridge.getImage());
        }

        // Only update fridges belonging to this user
        int result = db.update(TABLE_FRIDGES, values,
                KEY_ID + "=? AND " + KEY_USER_EMAIL + "=?",
                new String[]{String.valueOf(fridge.getId()), userEmail});
        db.close();
        return result > 0;
    }

    @SuppressLint("Range")
    public List<Fridge> getUserFridges(String userEmail) {
        List<Fridge> fridgeList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_FRIDGES,
                null,
                KEY_USER_EMAIL + "=?", new String[]{userEmail},
                null, null, KEY_ID + " ASC");

        if (cursor.moveToFirst()) {
            do {
                Fridge fridge = new Fridge();
                fridge.setId(cursor.getInt(cursor.getColumnIndex(KEY_ID)));
                fridge.setBrand(cursor.getString(cursor.getColumnIndex(KEY_BRAND)));
                fridge.setModel(cursor.getString(cursor.getColumnIndex(KEY_MODEL)));
                fridge.setDescription(cursor.getString(cursor.getColumnIndex(KEY_DESCRIPTION)));
                fridge.setSize(cursor.getInt(cursor.getColumnIndex(KEY_SIZE)));
                fridge.setLoggedIn(cursor.getInt(cursor.getColumnIndex(KEY_IS_LOGGED_IN)) == 1);
                fridge.setImage(cursor.getBlob(cursor.getColumnIndex(KEY_FRIDGE_IMAGE)));

                fridgeList.add(fridge);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return fridgeList;
    }

    public boolean logIntoFridge(int fridgeId, String userEmail) {
        SQLiteDatabase db = this.getWritableDatabase();

        try {
            db.beginTransaction();

            // First, log out from all fridges for this user
            ContentValues loggedOffValues = new ContentValues();
            loggedOffValues.put(KEY_IS_LOGGED_IN, 0);
            db.update(TABLE_FRIDGES, loggedOffValues, KEY_USER_EMAIL + "=?", new String[]{userEmail});

            // Log into the specified fridge (only if it belongs to the user)
            ContentValues loggedInValues = new ContentValues();
            loggedInValues.put(KEY_IS_LOGGED_IN, 1);
            int rowsAffected = db.update(TABLE_FRIDGES, loggedInValues,
                    KEY_ID + "=? AND " + KEY_USER_EMAIL + "=?",
                    new String[]{String.valueOf(fridgeId), userEmail});

            db.setTransactionSuccessful();
            return rowsAffected > 0;
        } finally {
            db.endTransaction();
            db.close();
        }
    }

    @SuppressLint("Range")
    public Fridge getConnectedFridgeForUser(String userEmail) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_FRIDGES, null,
                KEY_USER_EMAIL + "=? AND " + KEY_IS_LOGGED_IN + " = 1",
                new String[]{userEmail}, null, null, null);

        Fridge fridge = null;
        if (cursor.moveToFirst()) {
            fridge = new Fridge();
            fridge.setId(cursor.getInt(cursor.getColumnIndex(KEY_ID)));
            fridge.setBrand(cursor.getString(cursor.getColumnIndex(KEY_BRAND)));
            fridge.setModel(cursor.getString(cursor.getColumnIndex(KEY_MODEL)));
            fridge.setDescription(cursor.getString(cursor.getColumnIndex(KEY_DESCRIPTION)));
            fridge.setSize(cursor.getInt(cursor.getColumnIndex(KEY_SIZE)));
            fridge.setLoggedIn(true);
            fridge.setImage(cursor.getBlob(cursor.getColumnIndex(KEY_FRIDGE_IMAGE)));
        }
        cursor.close();
        return fridge;
    }


    @SuppressLint("Range")
    public Fridge getFridgeById(int fridgeId, String userEmail) {
        SQLiteDatabase db = this.getReadableDatabase();
        Fridge fridge = null;

        Cursor cursor = db.query(TABLE_FRIDGES,
                null,
                KEY_ID + "=? AND " + KEY_USER_EMAIL + "=?",
                new String[]{String.valueOf(fridgeId), userEmail},
                null, null, null);

        if (cursor.moveToFirst()) {
            fridge = new Fridge();
            fridge.setId(cursor.getInt(cursor.getColumnIndex(KEY_ID)));
            fridge.setBrand(cursor.getString(cursor.getColumnIndex(KEY_BRAND)));
            fridge.setModel(cursor.getString(cursor.getColumnIndex(KEY_MODEL)));
            fridge.setDescription(cursor.getString(cursor.getColumnIndex(KEY_DESCRIPTION)));
            fridge.setSize(cursor.getInt(cursor.getColumnIndex(KEY_SIZE)));
            fridge.setLoggedIn(cursor.getInt(cursor.getColumnIndex(KEY_IS_LOGGED_IN)) == 1);
            fridge.setImage(cursor.getBlob(cursor.getColumnIndex(KEY_FRIDGE_IMAGE)));
        }
        cursor.close();
        return fridge;
    }

    public boolean deleteFridge(int fridgeId, String userEmail) {
        SQLiteDatabase db = this.getWritableDatabase();
        // Only delete fridges belonging to this user (cascade will delete food items)
        int result = db.delete(TABLE_FRIDGES,
                KEY_ID + "=? AND " + KEY_USER_EMAIL + "=?",
                new String[]{String.valueOf(fridgeId), userEmail});
        db.close();
        return result > 0;
    }

    public int getFridgeCountForUser(String userEmail) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_FRIDGES +
                " WHERE " + KEY_USER_EMAIL + "=?", new String[]{userEmail});
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        return count;
    }

    // FOOD ITEM METHODS - Updated to work with fridge ownership
    public long addFoodItem(FoodItem item, String userEmail) {
        // Get the current connected fridge for the user
        Fridge connectedFridge = getConnectedFridgeForUser(userEmail);
        if (connectedFridge == null) {
            Toast.makeText(context, "Please select a fridge first", Toast.LENGTH_SHORT).show();
            return -1;
        }

        return addFoodItemToFridge(item, connectedFridge.getId());
    }

    public long addFoodItemToFridge(FoodItem item, int fridgeId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_ITEM_NAME, item.getName());
        values.put(KEY_CATEGORY, item.getCategory());
        values.put(KEY_EXPIRY_DATE, item.getExpiryDate());
        values.put(KEY_QUANTITY, item.getQuantity());
        values.put(KEY_FRIDGE_ID, fridgeId);

        if (item.getImage() != null) {
            values.put(KEY_FOOD_IMAGE, item.getImage());
        }

        long result = db.insert(TABLE_FOOD_ITEMS, null, values);
        db.close();

        if (result == -1) {
            Toast.makeText(context, "Failed to add " + item.getName(), Toast.LENGTH_SHORT).show();
        } else {
            item.setId((int) result);
            Toast.makeText(context, item.getName() + " added successfully", Toast.LENGTH_SHORT).show();
        }
        return result;
    }

    public boolean updateFoodItem(FoodItem item, String userEmail) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_ITEM_NAME, item.getName());
        values.put(KEY_CATEGORY, item.getCategory());
        values.put(KEY_EXPIRY_DATE, item.getExpiryDate());
        values.put(KEY_QUANTITY, item.getQuantity());

        if (item.getImage() != null) {
            values.put(KEY_FOOD_IMAGE, item.getImage());
        }

        // Only update items in fridges belonging to this user
        String whereClause = KEY_ID + "=? AND " + KEY_FRIDGE_ID + " IN " +
                "(SELECT " + KEY_ID + " FROM " + TABLE_FRIDGES + " WHERE " + KEY_USER_EMAIL + "=?)";

        int result = db.update(TABLE_FOOD_ITEMS, values, whereClause,
                new String[]{String.valueOf(item.getId()), userEmail});
        db.close();
        return result > 0;
    }

    public List<FoodItem> getUserFoodItems(String userEmail) {
        List<FoodItem> foodItems = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        // Get all food items from all fridges belonging to the user
        String query = "SELECT fi.* FROM " + TABLE_FOOD_ITEMS + " fi " +
                "INNER JOIN " + TABLE_FRIDGES + " f ON fi." + KEY_FRIDGE_ID + " = f." + KEY_ID + " " +
                "WHERE f." + KEY_USER_EMAIL + " = ? " +
                "ORDER BY fi." + KEY_EXPIRY_DATE + " ASC";

        Cursor cursor = db.rawQuery(query, new String[]{userEmail});

        if (cursor.moveToFirst()) {
            do {
                FoodItem item = new FoodItem();
                item.setId(cursor.getInt(0));
                item.setName(cursor.getString(1));
                item.setCategory(cursor.getString(2));
                item.setExpiryDate(cursor.getString(3));
                item.setQuantity(cursor.getInt(4));
                item.setImage(cursor.getBlob(5));

                foodItems.add(item);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return foodItems;
    }

    public List<FoodItem> getFoodItemsInConnectedFridge(String userEmail) {
        List<FoodItem> foodItems = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        // Get food items from the currently connected fridge for the user
        String query = "SELECT fi.* FROM " + TABLE_FOOD_ITEMS + " fi " +
                "INNER JOIN " + TABLE_FRIDGES + " f ON fi." + KEY_FRIDGE_ID + " = f." + KEY_ID + " " +
                "WHERE f." + KEY_USER_EMAIL + " = ? AND f." + KEY_IS_LOGGED_IN + " = 1 " +
                "ORDER BY fi." + KEY_EXPIRY_DATE + " ASC";

        Cursor cursor = db.rawQuery(query, new String[]{userEmail});

        if (cursor.moveToFirst()) {
            do {
                FoodItem item = new FoodItem();
                item.setId(cursor.getInt(0));
                item.setName(cursor.getString(1));
                item.setCategory(cursor.getString(2));
                item.setExpiryDate(cursor.getString(3));
                item.setQuantity(cursor.getInt(4));
                item.setImage(cursor.getBlob(5));

                foodItems.add(item);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return foodItems;
    }

    public List<FoodItem> getFoodItemsByFridge(int fridgeId, String userEmail) {
        List<FoodItem> foodItems = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        // Get food items from specific fridge (only if user owns the fridge)
        String query = "SELECT fi.* FROM " + TABLE_FOOD_ITEMS + " fi " +
                "INNER JOIN " + TABLE_FRIDGES + " f ON fi." + KEY_FRIDGE_ID + " = f." + KEY_ID + " " +
                "WHERE f." + KEY_ID + " = ? AND f." + KEY_USER_EMAIL + " = ? " +
                "ORDER BY fi." + KEY_EXPIRY_DATE + " ASC";

        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(fridgeId), userEmail});

        if (cursor.moveToFirst()) {
            do {
                FoodItem item = new FoodItem();
                item.setId(cursor.getInt(0));
                item.setName(cursor.getString(1));
                item.setCategory(cursor.getString(2));
                item.setExpiryDate(cursor.getString(3));
                item.setQuantity(cursor.getInt(4));
                item.setImage(cursor.getBlob(5));

                foodItems.add(item);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return foodItems;
    }

    public boolean deleteFoodItem(int id, String userEmail) {
        SQLiteDatabase db = this.getWritableDatabase();

        // Only delete items from fridges belonging to this user
        String whereClause = KEY_ID + "=? AND " + KEY_FRIDGE_ID + " IN " +
                "(SELECT " + KEY_ID + " FROM " + TABLE_FRIDGES + " WHERE " + KEY_USER_EMAIL + "=?)";

        int result = db.delete(TABLE_FOOD_ITEMS, whereClause,
                new String[]{String.valueOf(id), userEmail});
        db.close();
        return result > 0;
    }

    // BACKWARD COMPATIBILITY METHODS - Deprecated but functional
//    @Deprecated
//    public long addFoodItem(FoodItem item) {
//        if (userSession != null && userSession.isLoggedIn()) {
//            return addFoodItem(item, userSession.getUserEmail());
//        }
//        return -1;
//    }
//
//    @Deprecated
//    public List<FoodItem> getUserFoodItems() {
//        if (userSession != null && userSession.isLoggedIn()) {
//            return getFoodItemsInConnectedFridge(userSession.getUserEmail());
//        }
//        return new ArrayList<>();
//    }
//
//    @Deprecated
//    public boolean updateFoodItem(FoodItem item) {
//        if (userSession != null && userSession.isLoggedIn()) {
//            return updateFoodItem(item, userSession.getUserEmail());
//        }
//        return false;
//    }
//
//    @Deprecated
//    public boolean deleteFoodItem(int id) {
//        if (userSession != null && userSession.isLoggedIn()) {
//            return deleteFoodItem(id, userSession.getUserEmail());
//        }
//        return false;
//    }
//
//    @Deprecated
//    public long addFridge(Fridge fridge) {
//        if (userSession != null && userSession.isLoggedIn()) {
//            return addFridge(fridge, userSession.getUserEmail());
//        }
//        return -1;
//    }
//
//    @Deprecated
//    public List<Fridge> getAllFridges() {
//        if (userSession != null && userSession.isLoggedIn()) {
//            return getUserFridges(userSession.getUserEmail());
//        }
//        return new ArrayList<>();
//    }
//
//    @Deprecated
//    public boolean logIntoFridge(int fridgeId) {
//        if (userSession != null && userSession.isLoggedIn()) {
//            return logIntoFridge(fridgeId, userSession.getUserEmail());
//        }
//        return false;
//    }
//
//    @Deprecated
//    public Fridge getConnectedFridge() {
//        if (userSession != null && userSession.isLoggedIn()) {
//            return getConnectedFridgeForUser(userSession.getUserEmail());
//        }
//        return null;
//    }
//
//    @Deprecated
//    public boolean deleteFridge(int fridgeId) {
//        if (userSession != null && userSession.isLoggedIn()) {
//            return deleteFridge(fridgeId, userSession.getUserEmail());
//        }
//        return false;
//    }
//
//    @Deprecated
//    public int getFridgeCount() {
//        if (userSession != null && userSession.isLoggedIn()) {
//            return getFridgeCountForUser(userSession.getUserEmail());
//        }
//        return 0;
//    }
    private void createTables(SQLiteDatabase db) {
        fridgeUserItemTables(db);
        createMealPlanMealTables(db);
        createRecipeTables(db);
        createShoppingListTable(db);
    }

    public static synchronized Database getInstance(Context context) {
        if (instance == null) {
            instance = new Database(context.getApplicationContext());
        }
        return instance;
    }

    // ... (Include all your other existing methods: meal methods, recipe methods, etc.)
    // I'll add the key ones here for completeness:

    public long createMeal(String mealName, String uri) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            ContentValues values = new ContentValues();
            values.put("mealName", mealName);
            if(uri!=null) {
                values.put("mealImage", uri);
            }
            return db.insert("Meal", null, values);
        } finally {
            db.close();
        }
    }

    public List<Meal> getAllMeals() {
        List<Meal> meals = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT * FROM Meal", null);

        if (cursor.moveToFirst()) {
            do {
                long mealID = cursor.getLong(cursor.getColumnIndexOrThrow("mealID"));
                Meal meal = getMealWithFoodItems(mealID);
                meals.add(meal);
            } while (cursor.moveToNext());
        }

        cursor.close();
        return meals;
    }

    public MealPlan getMealPlanForDay(LocalDate planDay) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM MealPlan WHERE planDay = ?";

        try (Cursor cursor = db.rawQuery(query, new String[]{planDay.toString()})) {
            if (cursor.moveToFirst()) {
                Long breakfastID = getNullableLong(cursor, "breakfastMealID");
                Long lunchID = getNullableLong(cursor, "lunchMealID");
                Long dinnerID = getNullableLong(cursor, "dinnerMealID");
                Long snackID = getNullableLong(cursor, "snackMealID");

                MealPlan plan = new MealPlan(planDay);
                plan.setBreakFast(breakfastID);
                plan.setLunch(lunchID);
                plan.setDinner(dinnerID);
                plan.setSnack(snackID);

                return plan;
            }
        } catch (SQLException e) {
            Log.e("MealPlanDB", "Error fetching meal plan for date: " + planDay, e);
            throw new RuntimeException("Failed to fetch meal plan", e);
        }

        return null;
    }

    private Long getNullableLong(Cursor cursor, String columnName) {
        int columnIndex = cursor.getColumnIndex(columnName);
        if (columnIndex == -1) {
            throw new IllegalArgumentException("Column not found: " + columnName);
        }
        return cursor.isNull(columnIndex) ? null : cursor.getLong(columnIndex);
    }

    public void addMealToPlan(long mealID, LocalDate planDay, String type) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        String planDayStr = planDay.toString();

        switch (Objects.requireNonNull(type)) {
            case "Breakfast":
                values.put("breakfastMealID", mealID);
                break;
            case "Lunch":
                values.put("lunchMealID", mealID);
                break;
            case "Dinner":
                values.put("dinnerMealID", mealID);
                break;
            case "Snack":
                values.put("snackMealID", mealID);
                break;
            default:
                throw new IllegalArgumentException("Invalid meal type: " + type);
        }

        int rowsAffected = db.update("MealPlan", values, "planDay = ?", new String[]{planDayStr});

        if (rowsAffected == 0) {
            values.put("planDay", planDayStr);
            db.insert("MealPlan", null, values);
        }
    }

    public List<FoodItem> getFoodItemsForMeal(long mealId) {
        List<FoodItem> foodItems = new ArrayList<>();

        SQLiteDatabase db = this.getReadableDatabase();

        String query = "SELECT f.* FROM " + TABLE_FOOD_ITEMS + " f " +
                "INNER JOIN MealFoodItem mfi ON f." + KEY_ID + " = mfi.foodItemID " +
                "WHERE mfi.mealID = ?";

        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(mealId)});

        if (cursor.moveToFirst()) {
            do {
                int foodItemID = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ID));
                String name = cursor.getString(cursor.getColumnIndexOrThrow(KEY_ITEM_NAME));
                byte[] imageUri = cursor.getBlob(cursor.getColumnIndexOrThrow(KEY_FOOD_IMAGE));
                int qty = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_QUANTITY));
                String expiryDateStr = cursor.getString(cursor.getColumnIndexOrThrow(KEY_EXPIRY_DATE));
                String categoryID = cursor.getString(cursor.getColumnIndexOrThrow(KEY_CATEGORY));
                int isInShoppingList = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_IS_IN_SHOPPING_LIST));

                FoodItem foodItem = new FoodItem(foodItemID, name, categoryID, expiryDateStr, qty, imageUri, isInShoppingList);
                foodItems.add(foodItem);
            } while (cursor.moveToNext());
        }

        cursor.close();
        return foodItems;
    }

    // MEAL PLAN METHODS
    public void deleteMealplan(LocalDate date) {
        SQLiteDatabase db = this.getWritableDatabase();
        String sql = "DELETE FROM mealPlan WHERE planDay = ?";
        db.execSQL(sql, new Object[]{date.toString()});
    }

    public void deleteExpiredMealPlans() {
        SQLiteDatabase db = this.getWritableDatabase();
        String sql = "DELETE FROM mealPlan WHERE planDay < ?";
        LocalDate curDate = CalendarUtils.selectedDate;
        db.execSQL(sql, new Object[]{curDate.toString()});
    }

    public void deleteMealPlan(LocalDate planDay) {
        SQLiteDatabase db = this.getWritableDatabase();
        String sql = "DELETE FROM mealPlan WHERE planDay = ?";
        db.execSQL(sql, new Object[]{planDay.toString()});
    }

    public boolean updateMeal(Meal meal) {
        if (meal == null || meal.getMealID() <= 0) {
            return false;
        }

        SQLiteDatabase db = this.getWritableDatabase();

        try {
            db.beginTransaction();

            ContentValues mealValues = new ContentValues();

            if (meal.getMealName() != null && !meal.getMealName().trim().isEmpty()) {
                mealValues.put("mealName", meal.getMealName());
            }
            if (meal.getUri() != null) {
                mealValues.put("mealImage", meal.getUri());
            }
            if (meal.getLastUsed() != null) {
                mealValues.put("lastUsed", meal.getLastUsed().toString());
            }

            if (mealValues.size() > 0) {
                int mealRowsAffected = db.update("Meal", mealValues, "mealID = ?",
                        new String[]{String.valueOf(meal.getMealID())});

                if (mealRowsAffected == 0) {
                    return false;
                }
            }

            db.delete("MealFoodItem", "mealID = ?",
                    new String[]{String.valueOf(meal.getMealID())});

            if (meal.getFoodItemIDs() != null && !meal.getFoodItemIDs().isEmpty()) {
                for (String foodItemID : meal.getFoodItemIDs()) {
                    if (foodItemID != null && Integer.parseInt(foodItemID) > 0) {
                        ContentValues mealFoodValues = new ContentValues();
                        mealFoodValues.put("mealID", meal.getMealID());
                        mealFoodValues.put("foodItemID", foodItemID);

                        long result = db.insert("MealFoodItem", null, mealFoodValues);
                        if (result == -1) {
                            db.endTransaction();
                            return false;
                        }
                    }
                }
            }

            db.setTransactionSuccessful();
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            db.endTransaction();
            db.close();
        }
    }

    public Meal getMealWithFoodItems(long mealID) {
        SQLiteDatabase db = this.getReadableDatabase();
        Meal meal = null;

        Cursor mealCursor = db.rawQuery("SELECT * FROM Meal WHERE mealID = ?",
                new String[]{String.valueOf(mealID)});

        if (mealCursor.moveToFirst()) {
            String name = mealCursor.getString(mealCursor.getColumnIndexOrThrow("mealName"));
            String image = mealCursor.getString(mealCursor.getColumnIndexOrThrow("mealImage"));
            String lastUsed = mealCursor.getString(mealCursor.getColumnIndexOrThrow("lastUsed"));

            meal = new Meal(mealID, name, R.drawable.image_placeholder);
            meal.setUrl(image);
            if (lastUsed != null) {
                meal.setLastUsed(LocalDate.parse(lastUsed));
            }

            Cursor foodItemsCursor = db.rawQuery(
                    "SELECT foodItemID FROM MealFoodItem WHERE mealID = ?",
                    new String[]{String.valueOf(mealID)}
            );

            ArrayList<String> foodItemIDs = new ArrayList<>();
            while (foodItemsCursor.moveToNext()) {
                String foodItemID = String.valueOf(foodItemsCursor.getLong(foodItemsCursor.getColumnIndexOrThrow("foodItemID")));
                foodItemIDs.add(foodItemID);
            }

            meal.setFoodItemIDs(foodItemIDs);
            foodItemsCursor.close();
        }

        mealCursor.close();
        return meal;
    }

    public boolean updateMealPlan(MealPlan mealPlan) {
        if (mealPlan == null) {
            Log.e("Database", "MealPlan cannot be null");
            return false;
        }
        SQLiteDatabase db = this.getWritableDatabase();

        try {
            Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM MealPlan WHERE planDay = ?",
                    new String[]{mealPlan.getPlanDay().toString()});

            boolean exists = false;
            if (cursor.moveToFirst()) {
                exists = cursor.getInt(0) > 0;
            }
            cursor.close();

            ContentValues values = new ContentValues();
            values.put("planDay", mealPlan.getPlanDay().toString());
            values.put("breakfastMealID", mealPlan.getBreakFast());
            values.put("lunchMealID", mealPlan.getLunch());
            values.put("dinnerMealID", mealPlan.getDinner());
            values.put("snackMealID", mealPlan.getSnack());

            long result;
            if (exists) {
                result = db.update("MealPlan", values, "planDay = ?", new String[]{mealPlan.getPlanDay().toString()});
            } else {
                result = db.insert("MealPlan", null, values);
            }

            return result > 0;

        } catch (Exception e) {
            Log.e("Database", "Error updating meal plan: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            db.close();
        }
    }

    public void deleteMeal(Meal meal) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete("Meal", "mealID =?", new String[]{String.valueOf(meal.getMealID())});
    }


    private void createMealPlanMealTables(SQLiteDatabase db) {
        String createMealTable = "CREATE TABLE Meal (" +
                "mealID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "mealName TEXT NOT NULL," +
                "mealImage TEXT ," +
                "lastUsed TEXT" +
                ")";
        db.execSQL(createMealTable);

        String createMealFoodItemTable = "CREATE TABLE MealFoodItem (" +
                "mealID INTEGER NOT NULL, " +
                "foodItemID INTEGER NOT NULL, " +
                "PRIMARY KEY (mealID, foodItemID), " +
                "FOREIGN KEY (mealID) REFERENCES Meal(mealID) ON DELETE CASCADE, " +
                "FOREIGN KEY (foodItemID) REFERENCES " + TABLE_FOOD_ITEMS + "(" + KEY_ID + ") ON DELETE CASCADE" +
                ")";
        db.execSQL(createMealFoodItemTable);

        String createMealPlanTable = "CREATE TABLE MealPlan (" +
                "planDay TEXT NOT NULL," +
                "breakfastMealID INTEGER," +
                "lunchMealID INTEGER," +
                "dinnerMealID INTEGER," +
                "snackMealID INTEGER," +
                "FOREIGN KEY (breakfastMealID) REFERENCES Meal(mealID)," +
                "FOREIGN KEY (lunchMealID) REFERENCES Meal(mealID)," +
                "FOREIGN KEY (dinnerMealID) REFERENCES Meal(mealID)," +
                "FOREIGN KEY (snackMealID) REFERENCES Meal(mealID)" +
                ")";
        db.execSQL(createMealPlanTable);
    }

    // RECIPE METHODS
    public static final String TABLE_RECIPES = "recipes";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_TITLE = "Recipe_Name";
    public static final String COLUMN_IMAGE = "Recipe_image";
    public static final String COLUMN_LIKES = "Recipe_likes";
    public static final String COLUMN_TIME = "Cooking_time";
    public static final String COLUMN_SERVINGS = "Recipe_servings";
    public static final String COLUMN_FAVORITE = "Favorite";

    public static final String TABLE_INGREDIENTS = "Ingredients";
    public static final String COLUMN_INGREDIENT_ID = "Ingredient_id";
    public static final String COLUMN_INGREDIENT_NAME = "Ingredient_Name";

    public static final String TABLE_RECIPE_INGREDIENTS = "Recipe_Ingredients";

    public static final String TABLE_INSTRUCTIONS = "Instructions";
    public static final String COLUMN_INSTRUCTION_ID = "Instruction_id";
    public static final String COLUMN_STEP_NUMBER = "Step_number";
    public static final String COLUMN_STEPS = "Step";
    public static final String COLUMN_RECIPE_ID = "Recipe_id";

    private void createRecipeTables(SQLiteDatabase db) {
        db.execSQL("PRAGMA foreign_keys=ON");

        db.execSQL("CREATE TABLE " + TABLE_RECIPES + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY, " +
                COLUMN_TITLE + " TEXT, " +
                COLUMN_IMAGE + " TEXT, " +
                COLUMN_LIKES + " INTEGER, " +
                COLUMN_TIME + " INTEGER, " +
                COLUMN_SERVINGS + " INTEGER, " +
                COLUMN_FAVORITE + " INTEGER DEFAULT 0)");

        db.execSQL("CREATE TABLE " + TABLE_INGREDIENTS + " (" +
                COLUMN_INGREDIENT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_INGREDIENT_NAME + " TEXT NOT NULL UNIQUE)");

        db.execSQL("CREATE TABLE " + TABLE_RECIPE_INGREDIENTS + " (" +
                COLUMN_RECIPE_ID + " INTEGER, " +
                COLUMN_INGREDIENT_ID + " INTEGER, " +
                "PRIMARY KEY (" + COLUMN_RECIPE_ID + ", " + COLUMN_INGREDIENT_ID + "), " +
                "FOREIGN KEY (" + COLUMN_RECIPE_ID + ") REFERENCES " + TABLE_RECIPES + "(" + COLUMN_ID + ") ON DELETE CASCADE, " +
                "FOREIGN KEY (" + COLUMN_INGREDIENT_ID + ") REFERENCES " + TABLE_INGREDIENTS + "(" + COLUMN_INGREDIENT_ID + ") ON DELETE CASCADE)");

        db.execSQL("CREATE TABLE " + TABLE_INSTRUCTIONS + " (" +
                COLUMN_INSTRUCTION_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_RECIPE_ID + " INTEGER, " +
                COLUMN_STEP_NUMBER + " INTEGER, " +
                COLUMN_STEPS + " TEXT, " +
                "FOREIGN KEY (" + COLUMN_RECIPE_ID + ") REFERENCES " + TABLE_RECIPES + "(" + COLUMN_ID + ") ON DELETE CASCADE)");
    }






    // SHOPPING LIST METHODS
    private static final String SHOPPING_TABLE_NAME = "my_shoppingList";
    private static final String SHOPPING_COLUMN_ID = "ShoppingList_ItemID";
    private static final String SHOPPING_COLUMN_NAME = "ShoppingList_ItemName";
    private static final String SHOPPING_COLUMN_QTY = "ShoppingList_ItemQTY";
    private static final String SHOPPING_COLUMN_BOUGHT = "ShoppingList_ItemBought";
    private void createShoppingListTable(SQLiteDatabase db) {
        String query = "CREATE TABLE " + SHOPPING_TABLE_NAME +
                " (" + SHOPPING_COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                SHOPPING_COLUMN_NAME + " TEXT, " +
                SHOPPING_COLUMN_QTY + " INTEGER ," +
                SHOPPING_COLUMN_BOUGHT + " INTEGER);";
        db.execSQL(query);
    }
    public long AddItem(String ItemName, int ItemQty){

        SQLiteDatabase db= this.getWritableDatabase();
        ContentValues cv= new ContentValues();

        cv.put(SHOPPING_COLUMN_NAME,ItemName);
        cv.put(SHOPPING_COLUMN_QTY,ItemQty);
        cv.put(SHOPPING_COLUMN_BOUGHT,0);

        long results= db.insert(SHOPPING_TABLE_NAME,null,cv);

        if(results==-1)
        {
            Toast.makeText(context, "Failed", Toast.LENGTH_SHORT).show();
        }
        else {
            Toast.makeText(context, "Added Successfully", Toast.LENGTH_SHORT).show();
        }
        return results;
    }
    public Cursor readAllData(){

        String query = "SELECT * FROM " + SHOPPING_TABLE_NAME;
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = null;
        if (db != null) {
            cursor = db.rawQuery(query, null);
        }
        return cursor;
    }
    public void deleteItemById(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        int result = db.delete(SHOPPING_TABLE_NAME, SHOPPING_COLUMN_ID + "=?", new String[]{String.valueOf(id)});

        if (result == -1) {
            Toast.makeText(context, "Failed to delete item", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, "Item deleted", Toast.LENGTH_SHORT).show();
        }
    }
    public void UpdateItemShoppingList(int id){

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(SHOPPING_COLUMN_BOUGHT,1);
        db.update(SHOPPING_TABLE_NAME, cv  ,SHOPPING_COLUMN_ID+"=?" ,new String[]{String.valueOf(id)});
    }
    public void UpdateItemBackShoppingList(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(SHOPPING_COLUMN_BOUGHT, 0); // Unmark as bought
        db.update(SHOPPING_COLUMN_BOUGHT, cv, SHOPPING_COLUMN_ID + "=?", new String[]{String.valueOf(id)});
    }



}