package com.example.foodkeeper.FoodkeeperUtils;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.widget.Toast;

import com.example.foodkeeper.FoodItem.models.Category;
import com.example.foodkeeper.FoodItem.models.FoodItem;
import com.example.foodkeeper.Fridge.Fridge;
import com.example.foodkeeper.Meal.Meal;
import com.example.foodkeeper.MealPlan.CalendarUtils;
import com.example.foodkeeper.MealPlan.MealPlan;
import com.example.foodkeeper.Recipe.Models.ExtendedIngredient;
import com.example.foodkeeper.Recipe.Models.InstructionsResponse;
import com.example.foodkeeper.Recipe.Models.Recipe;
import com.example.foodkeeper.Recipe.Models.RecipeDetailsResponse;
import com.example.foodkeeper.Recipe.Models.Step;
import com.example.foodkeeper.Register.SessionManager;
import com.example.foodkeeper.User;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Database extends SQLiteOpenHelper {
    private Context context;
    private static final String DATABASE_NAME = "FoodKeeper.db";
    private static final int DATABASE_VERSION = 38;
    private static Database instance;
    private static final String TABLE_USERS = "users";
    private static final String TABLE_FOOD_ITEMS = "food_items";
    private static final String TABLE_FRIDGES = "fridges";
    private static final String TABLE_CATEGORIES = "categories";
    private static final String KEY_ID = "item_id";
    private static final String KEY_NAME = "name";
    private static final String KEY_SURNAME = "surname";
    public static final String KEY_EMAIL = "email";
    private static final String KEY_PHONE = "phone";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_PROFILE = "profile_image";

    public static final String KEY_ITEM_NAME = "item_name";
    public static final String KEY_CATEGORY = "category";
    public static final String KEY_EXPIRY_DATE = "expiry_date";
    public static final String KEY_QUANTITY = "quantity";
    public static final String KEY_FOOD_IMAGE = "food_image";
    public static final String KEY_IS_IN_SHOPPING_LIST = "isInShoppingList";
    public static final String KEY_FRIDGE_ID = "fridge_id";


    private static final String KEY_BRAND = "brand";
    private static final String KEY_MODEL = "model";
    private static final String KEY_DESCRIPTION = "description";
    private static final String KEY_SIZE = "size";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_FRIDGE_IMAGE = "fridge_image";
    public static final String KEY_USER_EMAIL = "user_email";

    private static final String KEY_CATEGORY_ID = "category_id";
    private static final String KEY_CATEGORY_NAME = "category_name";

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
        String CREATE_USERS_TABLE = "CREATE TABLE " + TABLE_USERS + "("
                + KEY_NAME + " TEXT,"
                + KEY_SURNAME + " TEXT,"
                + KEY_EMAIL + " TEXT PRIMARY KEY,"
                + KEY_PHONE + " TEXT,"
                + KEY_PASSWORD + " TEXT,"
                + KEY_PROFILE + " BLOB)";
        db.execSQL(CREATE_USERS_TABLE);

        String CREATE_FRIDGES_TABLE = "CREATE TABLE " + TABLE_FRIDGES + "("
                + KEY_FRIDGE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_BRAND + " TEXT,"
                + KEY_MODEL + " TEXT,"
                + KEY_DESCRIPTION + " TEXT,"
                + KEY_SIZE + " INTEGER,"
                + KEY_IS_LOGGED_IN + " INTEGER DEFAULT 0,"
                + KEY_FRIDGE_IMAGE + " BLOB,"
                + KEY_USER_EMAIL + " TEXT NOT NULL,"
                + "FOREIGN KEY(" + KEY_USER_EMAIL + ") REFERENCES " + TABLE_USERS + "(" + KEY_EMAIL + ") ON DELETE CASCADE)";
        db.execSQL(CREATE_FRIDGES_TABLE);

        String CREATE_FOOD_ITEMS_TABLE = "CREATE TABLE " + TABLE_FOOD_ITEMS + "("
                + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_ITEM_NAME + " TEXT,"
                + KEY_CATEGORY + " INTEGER,"
                + KEY_EXPIRY_DATE + " TEXT,"
                + KEY_QUANTITY + " INTEGER,"
                + KEY_FOOD_IMAGE + " BLOB,"
                + KEY_IS_IN_SHOPPING_LIST + " INTEGER,"
                + KEY_FRIDGE_ID + " INTEGER NOT NULL,"
                + "FOREIGN KEY(" + KEY_CATEGORY + ") REFERENCES " + TABLE_CATEGORIES + "(" + KEY_CATEGORY_ID + ") ON DELETE SET NULL,"
                + "FOREIGN KEY(" + KEY_FRIDGE_ID + ") REFERENCES " + TABLE_FRIDGES + "(" + KEY_FRIDGE_ID + ") ON DELETE CASCADE)";
        db.execSQL(CREATE_FOOD_ITEMS_TABLE);
    }

    private void createCategoryTable(SQLiteDatabase db) {
        String CREATE_CATEGORIES_TABLE = "CREATE TABLE " + TABLE_CATEGORIES + "("
                + KEY_CATEGORY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_CATEGORY_NAME + " TEXT NOT NULL UNIQUE"
                + ")";
        db.execSQL(CREATE_CATEGORIES_TABLE);

        populateCategories(db);
    }

    private void populateCategories(SQLiteDatabase db) {
        String[][] categories = {
                {"1", "Fruits"},
                {"2", "Vegetables"},
                {"3", "Dairy & Eggs"},
                {"4", "Meat & Poultry"},
                {"5", "Seafood"},
                {"6", "Grains & Bread"},
                {"7", "Beverages"},
                {"8", "Snacks"},
                {"9", "Frozen Foods"},
                {"10", "Canned Goods"},
                {"11", "Spices"},
                {"12", "Bakery"},
                {"13", "Deli"},
                {"14", "Leftovers"},
                {"15", "Other"}
        };

        for (String[] category : categories) {
            ContentValues values = new ContentValues();
            values.put(KEY_CATEGORY_ID, Integer.parseInt(category[0]));
            values.put(KEY_CATEGORY_NAME, category[1]);

            db.insertWithOnConflict(TABLE_CATEGORIES, null, values, SQLiteDatabase.CONFLICT_IGNORE);
        }
    }

    @SuppressLint("Range")
    public List<Category> getAllCategories() {
        List<Category> categories = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;

        try {
            cursor = db.query(TABLE_CATEGORIES,
                    new String[]{KEY_CATEGORY_ID, KEY_CATEGORY_NAME},
                    null, null, null, null, KEY_CATEGORY_NAME + " ASC");

            if (cursor.moveToFirst()) {
                do {
                    Category category = new Category();
                    category.setId(cursor.getInt(cursor.getColumnIndex(KEY_CATEGORY_ID)));
                    category.setName(cursor.getString(cursor.getColumnIndex(KEY_CATEGORY_NAME)));
                    categories.add(category);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }
        return categories;
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_FOOD_ITEMS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_FRIDGES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CATEGORIES);
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

    @SuppressLint("Range")
    public Category getCategoryById(int categoryId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Category category = null;
        Cursor cursor = null;

        try {
            cursor = db.query(TABLE_CATEGORIES,
                    new String[]{KEY_CATEGORY_ID, KEY_CATEGORY_NAME},
                    KEY_CATEGORY_ID + " = ?",
                    new String[]{String.valueOf(categoryId)},
                    null, null, null);

            if (cursor.moveToFirst()) {
                category = new Category();
                category.setId(cursor.getInt(cursor.getColumnIndex(KEY_CATEGORY_ID)));
                category.setName(cursor.getString(cursor.getColumnIndex(KEY_CATEGORY_NAME)));
            }
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }
        return category;
    }

    public String getCategoryNameById(int categoryId) {
        Category category = getCategoryById(categoryId);
        return category != null ? category.getName() : "Other";
    }

    public void register(String name, String surname, String email, String phone, String password, byte[] profileImage) {
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

        int result = db.update(TABLE_FRIDGES, values,
                KEY_FRIDGE_ID+ "=? AND " + KEY_USER_EMAIL + "=?",
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
                null, null, KEY_FRIDGE_ID + " ASC");

        if (cursor.moveToFirst()) {
            do {
                Fridge fridge = new Fridge();
                fridge.setId(cursor.getInt(cursor.getColumnIndex(KEY_FRIDGE_ID)));
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

            ContentValues loggedOffValues = new ContentValues();
            loggedOffValues.put(KEY_IS_LOGGED_IN, 0);
            db.update(TABLE_FRIDGES, loggedOffValues, KEY_USER_EMAIL + "=?", new String[]{userEmail});

            ContentValues loggedInValues = new ContentValues();
            loggedInValues.put(KEY_IS_LOGGED_IN, 1);
            int rowsAffected = db.update(TABLE_FRIDGES, loggedInValues,
                    KEY_FRIDGE_ID + "=? AND " + KEY_USER_EMAIL + "=?",
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
            fridge.setId(cursor.getInt(cursor.getColumnIndex(KEY_FRIDGE_ID)));
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
                KEY_FRIDGE_ID + "=? AND " + KEY_USER_EMAIL + "=?",
                new String[]{String.valueOf(fridgeId), userEmail},
                null, null, null);

        if (cursor.moveToFirst()) {
            fridge = new Fridge();
            fridge.setId(cursor.getInt(cursor.getColumnIndex(KEY_FRIDGE_ID)));
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
        int result = db.delete(TABLE_FRIDGES,
                KEY_FRIDGE_ID + "=? AND " + KEY_USER_EMAIL + "=?",
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

    public long addFoodItem(FoodItem item, String userEmail) {
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
        values.put(KEY_EXPIRY_DATE, item.getExpiryDate());
        values.put(KEY_QUANTITY, item.getQuantity());
        values.put(KEY_FRIDGE_ID, fridgeId);

        if (item.getCategory() != null) {
            try {
                int categoryId = Integer.parseInt(item.getCategory());
                values.put(KEY_CATEGORY, categoryId);
            } catch (NumberFormatException e) {
                values.put(KEY_CATEGORY, 15);
            }
        } else {
            values.put(KEY_CATEGORY, 15);
        }

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





    private void createTables(SQLiteDatabase db) {
        fridgeUserItemTables(db);
        createCategoryTable(db);
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
    public long createMeal(Meal meal, long fridgeID) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            ContentValues values = new ContentValues();
            values.put("mealName", meal.getMealName());
            values.put("fridgeID", fridgeID);  // Make sure this matches your schema
            if(meal.getUri() != null) {
                values.put("mealImage", meal.getUri());
            }
            return db.insert("Meal", null, values);
        } finally {
            db.close();
        }
    }
    private Long getNullableLong(Cursor cursor, String columnName) {
        int columnIndex = cursor.getColumnIndex(columnName);
        if (columnIndex == -1) {
            throw new IllegalArgumentException("Column not found: " + columnName);
        }
        return cursor.isNull(columnIndex) ? null : cursor.getLong(columnIndex);
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

        String whereClause = KEY_ID + "=? AND " + KEY_FRIDGE_ID + " IN " +
                "(SELECT " + KEY_FRIDGE_ID + " FROM " + TABLE_FRIDGES + " WHERE " + KEY_USER_EMAIL + "=?)";

        int result = db.update(TABLE_FOOD_ITEMS, values, whereClause,
                new String[]{String.valueOf(item.getId()), userEmail});
        db.close();
        return result > 0;
    }
    public List<FoodItem> getUserFoodItems(String userEmail) {
        List<FoodItem> foodItems = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String query = "SELECT fi.*, c." + KEY_CATEGORY_NAME + " FROM " + TABLE_FOOD_ITEMS + " fi " +
                "INNER JOIN " + TABLE_FRIDGES + " f ON fi." + KEY_FRIDGE_ID + " = f." + KEY_FRIDGE_ID + " " +
                "LEFT JOIN " + TABLE_CATEGORIES + " c ON fi." + KEY_CATEGORY + " = c." + KEY_CATEGORY_ID + " " +
                "WHERE f." + KEY_USER_EMAIL + " = ? " +
                "ORDER BY fi." + KEY_EXPIRY_DATE + " ASC";

        Cursor cursor = db.rawQuery(query, new String[]{userEmail});

        if (cursor.moveToFirst()) {
            do {
                FoodItem item = new FoodItem();
                item.setId(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ID)));
                item.setName(cursor.getString(cursor.getColumnIndexOrThrow(KEY_ITEM_NAME)));
                item.setCategory(cursor.getString(cursor.getColumnIndexOrThrow(KEY_CATEGORY)));

                String categoryName = cursor.getString(cursor.getColumnIndexOrThrow(KEY_CATEGORY_NAME));
                item.setCategoryName(categoryName != null ? categoryName : "Other");

                item.setExpiryDate(cursor.getString(cursor.getColumnIndexOrThrow(KEY_EXPIRY_DATE)));
                item.setQuantity(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_QUANTITY)));
                item.setImage(cursor.getBlob(cursor.getColumnIndexOrThrow(KEY_FOOD_IMAGE)));

                foodItems.add(item);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return foodItems;
    }
    public List<FoodItem> getFoodItemsInConnectedFridge(String userEmail) {
        List<FoodItem> foodItems = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String query = "SELECT fi.*, c." + KEY_CATEGORY_NAME + " FROM " + TABLE_FOOD_ITEMS + " fi " +
                "INNER JOIN " + TABLE_FRIDGES + " f ON fi." + KEY_FRIDGE_ID + " = f." + KEY_FRIDGE_ID + " " +
                "LEFT JOIN " + TABLE_CATEGORIES + " c ON fi." + KEY_CATEGORY + " = c." + KEY_CATEGORY_ID + " " +
                "WHERE f." + KEY_USER_EMAIL + " = ? AND f." + KEY_IS_LOGGED_IN + " = 1 " +
                "ORDER BY fi." + KEY_EXPIRY_DATE + " ASC";

        Cursor cursor = db.rawQuery(query, new String[]{userEmail});

        if (cursor.moveToFirst()) {
            do {
                FoodItem item = new FoodItem();
                item.setId(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ID)));
                item.setName(cursor.getString(cursor.getColumnIndexOrThrow(KEY_ITEM_NAME)));
                item.setCategory(cursor.getString(cursor.getColumnIndexOrThrow(KEY_CATEGORY)));
                item.setExpiryDate(cursor.getString(cursor.getColumnIndexOrThrow(KEY_EXPIRY_DATE)));
                item.setQuantity(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_QUANTITY)));
                item.setImage(cursor.getBlob(cursor.getColumnIndexOrThrow(KEY_FOOD_IMAGE)));

                foodItems.add(item);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return foodItems;
    }
    public List<FoodItem> getFoodItemsByFridge(int fridgeId, String userEmail) {
        List<FoodItem> foodItems = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String query = "SELECT fi.* FROM " + TABLE_FOOD_ITEMS + " fi " +
                "INNER JOIN " + TABLE_FRIDGES + " f ON fi." + KEY_FRIDGE_ID + " = f." + KEY_FRIDGE_ID + " " +
                "WHERE f." + KEY_FRIDGE_ID + " = ? AND f." + KEY_USER_EMAIL + " = ? " +
                "ORDER BY fi." + KEY_EXPIRY_DATE + " ASC";

        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(fridgeId), userEmail});

        if (cursor.moveToFirst()) {
            do {
                FoodItem item = new FoodItem();
                item.setId(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ID)));
                item.setName(cursor.getString(cursor.getColumnIndexOrThrow(KEY_ITEM_NAME)));
                item.setCategory(cursor.getString(cursor.getColumnIndexOrThrow(KEY_CATEGORY)));
                item.setExpiryDate(cursor.getString(cursor.getColumnIndexOrThrow(KEY_EXPIRY_DATE)));
                item.setQuantity(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_QUANTITY)));
                item.setImage(cursor.getBlob(cursor.getColumnIndexOrThrow(KEY_FOOD_IMAGE)));

                foodItems.add(item);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return foodItems;
    }
    public boolean deleteFoodItem(int id, String userEmail) {
        SQLiteDatabase db = this.getWritableDatabase();

        String whereClause = KEY_ID + "=? AND " + KEY_FRIDGE_ID + " IN " +
                "(SELECT " + KEY_FRIDGE_ID + " FROM " + TABLE_FRIDGES + " WHERE " + KEY_USER_EMAIL + "=?)";

        int result = db.delete(TABLE_FOOD_ITEMS, whereClause,
                new String[]{String.valueOf(id), userEmail});
        db.close();
        return result > 0;
    }

    public List<Meal> getMealsInConnectedFridge(String userEmail) {
        List<Meal> meals = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            String query = "SELECT m.* FROM Meal m " +
                    "INNER JOIN " + TABLE_FRIDGES + " f ON m.fridgeID = f." + KEY_FRIDGE_ID + " " +
                    "WHERE f." + KEY_IS_LOGGED_IN + " = 1 AND f." + KEY_USER_EMAIL + "=?";

            cursor = db.rawQuery(query, new String[]{userEmail});

            if (cursor.moveToFirst()) {
                do {
                    long id = cursor.getLong(cursor.getColumnIndexOrThrow("mealID"));
                    String name = cursor.getString(cursor.getColumnIndexOrThrow("mealName"));
                    String imageUri = cursor.getString(cursor.getColumnIndexOrThrow("mealImage"));
                    String lastUsed = cursor.getString(cursor.getColumnIndexOrThrow("lastUsed"));
                    long fridgeID = cursor.getLong(cursor.getColumnIndexOrThrow("fridgeID"));

                    Meal meal = new Meal(id, name, imageUri, fridgeID);
                    if (lastUsed != null) {
                        meal.setLastUsed(LocalDate.parse(lastUsed));
                    }
                    meals.add(meal);
                } while (cursor.moveToNext());
            }

            return meals;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
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
            long fridgeID = mealCursor.getLong(mealCursor.getColumnIndexOrThrow("fridgeID"));

            meal = new Meal(mealID, name,image, fridgeID);
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

    public void deleteMeal(Meal meal) throws Exception {
        SQLiteDatabase db = this.getReadableDatabase();
        long mealID = meal.getMealID();

        Cursor cursor = db.rawQuery(
                "SELECT COUNT(*) FROM MealPlan WHERE breakfastMealID = ? OR lunchMealID = ? OR dinnerMealID = ? OR snackMealID = ?",
                new String[]{String.valueOf(mealID), String.valueOf(mealID), String.valueOf(mealID), String.valueOf(mealID)}
        );
        cursor.moveToFirst();
        int count = cursor.getInt(0);
        cursor.close();

        if (count > 0) {
            throw new Exception("This meal is currently used in " + count + " meal plan(s)");
        }

        // If not in use, proceed with deletion
        db = this.getWritableDatabase();
        db.delete("Meal", "mealID = ?", new String[]{String.valueOf(mealID)});
        db.close();
    }
    public void deleteMealPlansForMeal(int mealID) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.putNull("breakfastMealID");
        db.update("MealPlan", values, "breakfastMealID = ?", new String[]{String.valueOf(mealID)});

        values.clear();
        values.putNull("lunchMealID");
        db.update("MealPlan", values, "lunchMealID = ?", new String[]{String.valueOf(mealID)});

        values.clear();
        values.putNull("dinnerMealID");
        db.update("MealPlan", values, "dinnerMealID = ?", new String[]{String.valueOf(mealID)});

        values.clear();
        values.putNull("snackMealID");
        db.update("MealPlan", values, "snackMealID = ?", new String[]{String.valueOf(mealID)});

        db.close();
    }
    public MealPlan getMealPlanForDay(LocalDate planDay, long fridgeID) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM MealPlan WHERE planDay = ? AND fridgeID = ?";

        try (Cursor cursor = db.rawQuery(query, new String[]{planDay.toString(), String.valueOf(fridgeID)})) {
            if (cursor.moveToFirst()) {
                Long breakfastID = getNullableLong(cursor, "breakfastMealID");
                Long lunchID = getNullableLong(cursor, "lunchMealID");
                Long dinnerID = getNullableLong(cursor, "dinnerMealID");
                Long snackID = getNullableLong(cursor, "snackMealID");

                MealPlan plan = new MealPlan(planDay, fridgeID);
                plan.setBreakFast(breakfastID);
                plan.setLunch(lunchID);
                plan.setDinner(dinnerID);
                plan.setSnack(snackID);

                return plan;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch meal plan", e);
        }

        return null;
    }
    public void addMealToPlan(long mealID, LocalDate planDay, String type, long fridgeID) {
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

        int rowsAffected = db.update("MealPlan", values,
                "planDay = ? AND fridgeID = ?",
                new String[]{planDayStr, String.valueOf(fridgeID)});

        if (rowsAffected == 0) {
            values.put("planDay", planDayStr);
            values.put("fridgeID", fridgeID);
            db.insert("MealPlan", null, values);
        }
    }

    public boolean updateMealPlan(MealPlan mealPlan) {
        if (mealPlan == null) {
            Log.e("Database", "MealPlan cannot be null");
            return false;
        }
        SQLiteDatabase db = this.getWritableDatabase();

        try {
            Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM MealPlan WHERE planDay = ? AND fridgeID = ?",
                    new String[]{mealPlan.getPlanDay().toString(), String.valueOf(mealPlan.getFridgeID())});

            boolean exists = false;
            if (cursor.moveToFirst()) {
                exists = cursor.getInt(0) > 0;
            }
            cursor.close();

            ContentValues values = new ContentValues();
            values.put("planDay", mealPlan.getPlanDay().toString());
            values.put("fridgeID", mealPlan.getFridgeID());
            values.put("breakfastMealID", mealPlan.getBreakFast());
            values.put("lunchMealID", mealPlan.getLunch());
            values.put("dinnerMealID", mealPlan.getDinner());
            values.put("snackMealID", mealPlan.getSnack());

            long result;
            if (exists) {
                result = db.update("MealPlan", values,
                        "planDay = ? AND fridgeID = ?",
                        new String[]{mealPlan.getPlanDay().toString(), String.valueOf(mealPlan.getFridgeID())});
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

    public void deleteMealplan(LocalDate date, long fridgeID) {
        SQLiteDatabase db = this.getWritableDatabase();
        String sql = "DELETE FROM MealPlan WHERE planDay = ? AND fridgeID = ?";
        db.execSQL(sql, new Object[]{date.toString(), fridgeID});
    }
    private void createMealPlanMealTables(SQLiteDatabase db) {
        String createMealTable = "CREATE TABLE Meal (" +
                "mealID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "mealName TEXT NOT NULL," +
                "mealImage TEXT," +
                "lastUsed TEXT," +
                "fridgeID INTEGER NOT NULL," +
                "FOREIGN KEY(fridgeID) REFERENCES " + TABLE_FRIDGES + "(" + KEY_FRIDGE_ID + ") ON DELETE CASCADE)";
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
                "fridgeID INTEGER NOT NULL," +
                "breakfastMealID INTEGER," +
                "lunchMealID INTEGER," +
                "dinnerMealID INTEGER," +
                "snackMealID INTEGER," +
                "PRIMARY KEY (planDay, fridgeID)," +
                "FOREIGN KEY (fridgeID) REFERENCES " + TABLE_FRIDGES + "(" + KEY_FRIDGE_ID + ") ON DELETE CASCADE," +
                "FOREIGN KEY (breakfastMealID) REFERENCES Meal(mealID)," +
                "FOREIGN KEY (lunchMealID) REFERENCES Meal(mealID)," +
                "FOREIGN KEY (dinnerMealID) REFERENCES Meal(mealID)," +
                "FOREIGN KEY (snackMealID) REFERENCES Meal(mealID)" +
                ")";
        db.execSQL(createMealPlanTable);
    }

    public static final String TABLE_RECIPES = "recipes";
    public static final String COLUMN_ID = "recipe_id";
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
                COLUMN_FAVORITE + " INTEGER DEFAULT 0, " +
                KEY_USER_EMAIL + " TEXT, " +
                "FOREIGN KEY(" + KEY_USER_EMAIL + ") REFERENCES " + TABLE_USERS + "(" + KEY_EMAIL + ") ON DELETE CASCADE);");

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



    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        if (!db.isReadOnly()) {
            db.execSQL("PRAGMA foreign_keys=ON");
        }
    }

    public long insertRecipe(int id, String Recipe_Name, String Recipe_image, int Recipe_likes, int Cooking_time, int Recipe_servings, int fav, String userEmail) {
        SQLiteDatabase db = this.getWritableDatabase();

        try {
            // Check if recipe already exists for this user
            Cursor cursor = db.rawQuery("SELECT " + COLUMN_FAVORITE + " FROM " + TABLE_RECIPES +
                            " WHERE " + COLUMN_ID + " = ? AND " + KEY_USER_EMAIL + " = ?",
                    new String[]{String.valueOf(id), userEmail});

            int existingFavoriteStatus = 0;
            if (cursor.moveToFirst()) {
                existingFavoriteStatus = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_FAVORITE));
            }
            cursor.close();

            ContentValues values = new ContentValues();
            values.put(COLUMN_ID, id);
            values.put(COLUMN_TITLE, Recipe_Name);
            values.put(COLUMN_IMAGE, Recipe_image);
            values.put(COLUMN_LIKES, Recipe_likes);
            values.put(COLUMN_TIME, Cooking_time);
            values.put(COLUMN_SERVINGS, Recipe_servings);
            values.put(COLUMN_FAVORITE, existingFavoriteStatus);
            values.put(KEY_USER_EMAIL, userEmail);

            long result = db.insertWithOnConflict(TABLE_RECIPES, null, values, SQLiteDatabase.CONFLICT_REPLACE);
            return result;

        } finally {
            db.close();
        }
    }
    public List<Recipe> getAllRecipes(String userEmail) {
        List<Recipe> recipes = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;

        try {
            cursor = db.rawQuery("SELECT * FROM " + TABLE_RECIPES + " WHERE " + KEY_USER_EMAIL + " = ?",
                    new String[]{userEmail});

            if (cursor.moveToFirst()) {
                do {
                    int id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID));
                    String title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE));
                    String image = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_IMAGE));
                    int likes = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_LIKES));
                    int time = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TIME));
                    int servings = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_SERVINGS));
                    int favorite = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_FAVORITE));

                    Recipe recipe = new Recipe(id, title, image, likes, time, servings, favorite);
                    recipes.add(recipe);
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.close();
        }
        return recipes;
    }
    public List<Recipe> searchRecipes(String query, String userEmail) {
        List<Recipe> recipes = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;

        try {
            cursor = db.rawQuery(
                    "SELECT * FROM " + TABLE_RECIPES + " WHERE " + COLUMN_TITLE + " LIKE ? AND " + KEY_USER_EMAIL + " = ?",
                    new String[]{"%" + query + "%", userEmail}
            );

            if (cursor.moveToFirst()) {
                do {
                    int id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID));
                    String title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE));
                    String image = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_IMAGE));
                    int likes = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_LIKES));
                    int time = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TIME));
                    int servings = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_SERVINGS));
                    int favorite = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_FAVORITE));

                    recipes.add(new Recipe(id, title, image, likes, time, servings, favorite));
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.close();
        }
        return recipes;
    }
    public List<Recipe> searchFavoriteRecipes(String searchQuery, String userEmail) {
        List<Recipe> favoriteRecipes = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;

        try {
            String query = "SELECT * FROM " + TABLE_RECIPES +
                    " WHERE " + COLUMN_FAVORITE + " = 1 AND " +
                    COLUMN_TITLE + " LIKE ? AND " + KEY_USER_EMAIL + " = ?";
            String[] selectionArgs = {"%" + searchQuery + "%", userEmail};

            cursor = db.rawQuery(query, selectionArgs);

            if (cursor.moveToFirst()) {
                do {
                    int id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID));
                    String title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE));
                    String image = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_IMAGE));
                    int aggregateLikes = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_LIKES));
                    int readyInMinutes = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TIME));
                    int servings = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_SERVINGS));
                    int fav = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_FAVORITE));

                    favoriteRecipes.add(new Recipe(id, title, image, aggregateLikes, readyInMinutes, servings, fav));
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.close();
        }

        return favoriteRecipes;
    }
    public boolean toggleFavorite(int recipeId, boolean isFavorite, String userEmail) {
        SQLiteDatabase db = null;
        try {
            db = this.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(COLUMN_FAVORITE, isFavorite ? 1 : 0);

            int rowsAffected = db.update(TABLE_RECIPES, values,
                    COLUMN_ID + " = ? AND " + KEY_USER_EMAIL + " = ?",
                    new String[]{String.valueOf(recipeId), userEmail});

            return rowsAffected > 0;
        } finally {
            if (db != null) {
                db.close();
            }
        }
    }
    public List<Recipe> getFavoriteRecipes(String userEmail) {
        List<Recipe> recipes = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;

        try {
            cursor = db.rawQuery(
                    "SELECT * FROM " + TABLE_RECIPES + " WHERE " + COLUMN_FAVORITE + " = 1 AND " + KEY_USER_EMAIL + " = ?",
                    new String[]{userEmail}
            );

            if (cursor.moveToFirst()) {
                do {
                    int id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID));
                    String title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE));
                    String image = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_IMAGE));
                    int likes = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_LIKES));
                    int time = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TIME));
                    int servings = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_SERVINGS));
                    int favorite = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_FAVORITE));

                    recipes.add(new Recipe(id, title, image, likes, time, servings, favorite));
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.close();
        }
        return recipes;
    }
    public long insertIngredient(String name) {
        SQLiteDatabase db = null;
        Cursor cursor = null;
        long result = -1;

        try {
            db = this.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(COLUMN_INGREDIENT_NAME, name);
            result = db.insertWithOnConflict(TABLE_INGREDIENTS, null, values, SQLiteDatabase.CONFLICT_IGNORE);

            // If the ingredient already exists, get its ID
            if (result == -1) {
                cursor = db.rawQuery(
                        "SELECT " + COLUMN_INGREDIENT_ID + " FROM " + TABLE_INGREDIENTS +
                                " WHERE " + COLUMN_INGREDIENT_NAME + " = ?",
                        new String[]{name}
                );

                if (cursor.moveToFirst()) {
                    result = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_INGREDIENT_ID));
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            if (db != null) {
                db.close();
            }
        }
        return result;
    }
    public long getIngredientIdByName(String name) {
        SQLiteDatabase db = this.getReadableDatabase();
        long id = -1;
        Cursor cursor = null;

        try {
            cursor = db.rawQuery(
                    "SELECT " + COLUMN_INGREDIENT_ID + " FROM " + TABLE_INGREDIENTS +
                            " WHERE " + COLUMN_INGREDIENT_NAME + " = ?",
                    new String[]{name}
            );

            if (cursor.moveToFirst()) {
                id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_INGREDIENT_ID));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.close();
        }
        return id;
    }
    public long insertRecipeIngredient(int recipeId, long ingredientId) {
        SQLiteDatabase db = null;
        long result = -1;

        try {
            db = this.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(COLUMN_RECIPE_ID, recipeId);
            values.put(COLUMN_INGREDIENT_ID, ingredientId);
            result = db.insertWithOnConflict(TABLE_RECIPE_INGREDIENTS, null, values, SQLiteDatabase.CONFLICT_IGNORE);
        } finally {
            if (db != null) {
                db.close();
            }
        }
        return result;
    }
    public long insertInstruction(int recipeId, int stepNumber, String step) {
        SQLiteDatabase db = null;
        long result = -1;

        try {
            db = this.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(COLUMN_RECIPE_ID, recipeId);
            values.put(COLUMN_STEP_NUMBER, stepNumber);
            values.put(COLUMN_STEPS, step);
            result = db.insertWithOnConflict(TABLE_INSTRUCTIONS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        } finally {
            if (db != null) {
                db.close();
            }
        }
        return result;
    }
    public List<InstructionsResponse> getCachedInstructions(int recipeId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        List<InstructionsResponse> result = new ArrayList<>();

        try {
            cursor = db.rawQuery(
                    "SELECT * FROM " + TABLE_INSTRUCTIONS + " WHERE " + COLUMN_RECIPE_ID + " = ? ORDER BY " + COLUMN_STEP_NUMBER,
                    new String[]{String.valueOf(recipeId)}
            );

            if (cursor.getCount() == 0) {
                return null;
            }

            InstructionsResponse instructionsBlock = new InstructionsResponse();
            instructionsBlock.name = "Cached Instructions";
            instructionsBlock.steps = new ArrayList<>();

            while (cursor.moveToNext()) {
                Step step = new Step();
                step.number = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_STEP_NUMBER));
                step.step = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_STEPS));
                instructionsBlock.steps.add(step);
            }

            result.add(instructionsBlock);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.close();
        }
        return result;
    }
    public int deleteInstructionsForRecipe(int recipeId) {
        SQLiteDatabase db = null;
        int rowsDeleted = 0;

        try {
            db = this.getWritableDatabase();
            rowsDeleted = db.delete(TABLE_INSTRUCTIONS, COLUMN_RECIPE_ID + " = ?", new String[]{String.valueOf(recipeId)});
        } finally {
            if (db != null) {
                db.close();
            }
        }
        return rowsDeleted;
    }
    public RecipeDetailsResponse getCachedRecipeDetails(int recipeId, String userEmail) {
        SQLiteDatabase db = this.getReadableDatabase();
        RecipeDetailsResponse details = null;
        Cursor recipeCursor = null;
        Cursor ingredientsCursor = null;

        try {
            recipeCursor = db.rawQuery(
                    "SELECT * FROM " + TABLE_RECIPES + " WHERE " + COLUMN_ID + " = ? AND " + KEY_USER_EMAIL + " = ?",
                    new String[]{String.valueOf(recipeId), userEmail}
            );

            if (!recipeCursor.moveToFirst()) {
                return null;
            }

            details = new RecipeDetailsResponse();
            details.id = recipeCursor.getInt(recipeCursor.getColumnIndexOrThrow(COLUMN_ID));
            details.title = recipeCursor.getString(recipeCursor.getColumnIndexOrThrow(COLUMN_TITLE));
            details.image = recipeCursor.getString(recipeCursor.getColumnIndexOrThrow(COLUMN_IMAGE));
            details.aggregateLikes = recipeCursor.getInt(recipeCursor.getColumnIndexOrThrow(COLUMN_LIKES));
            details.readyInMinutes = recipeCursor.getInt(recipeCursor.getColumnIndexOrThrow(COLUMN_TIME));
            details.servings = recipeCursor.getInt(recipeCursor.getColumnIndexOrThrow(COLUMN_SERVINGS));
            details.fav = recipeCursor.getInt(recipeCursor.getColumnIndexOrThrow(COLUMN_FAVORITE));

            details.extendedIngredients = new ArrayList<>();

            ingredientsCursor = db.rawQuery(
                    "SELECT i." + COLUMN_INGREDIENT_ID + ", i." + COLUMN_INGREDIENT_NAME + " " +
                            "FROM " + TABLE_INGREDIENTS + " i " +
                            "JOIN " + TABLE_RECIPE_INGREDIENTS + " ri ON i." + COLUMN_INGREDIENT_ID + " = ri." + COLUMN_INGREDIENT_ID + " " +
                            "WHERE ri." + COLUMN_RECIPE_ID + " = ?",
                    new String[]{String.valueOf(recipeId)}
            );

            while (ingredientsCursor.moveToNext()) {
                ExtendedIngredient ingredient = new ExtendedIngredient();
                ingredient.id = ingredientsCursor.getInt(0);
                ingredient.name = ingredientsCursor.getString(1);

                if (ingredient.meta == null) {
                    ingredient.meta = new ArrayList<>();
                }

                details.extendedIngredients.add(ingredient);
            }
        } finally {
            if (recipeCursor != null) {
                recipeCursor.close();
            }
            if (ingredientsCursor != null) {
                ingredientsCursor.close();
            }
            db.close();
        }
        return details;
    }
    public boolean hasIngredientsForRecipe(int recipeId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        boolean hasIngredients = false;

        try {
            cursor = db.rawQuery(
                    "SELECT COUNT(*) FROM " + TABLE_RECIPE_INGREDIENTS + " WHERE " + COLUMN_RECIPE_ID + " = ?",
                    new String[]{String.valueOf(recipeId)}
            );
            cursor.moveToFirst();
            int count = cursor.getInt(0);
            hasIngredients = count > 0;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.close();
        }
        return hasIngredients;
    }
    public boolean hasInstructionsForRecipe(int recipeId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        boolean hasInstructions = false;

        try {
            cursor = db.rawQuery(
                    "SELECT COUNT(*) FROM " + TABLE_INSTRUCTIONS + " WHERE " + COLUMN_RECIPE_ID + " = ?",
                    new String[]{String.valueOf(recipeId)}
            );
            cursor.moveToFirst();
            int count = cursor.getInt(0);
            hasInstructions = count > 0;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.close();
        }
        return hasInstructions;
    }
    public int deleteIngredientsForRecipe(int recipeId) {
        SQLiteDatabase db = null;
        int rowsDeleted = 0;

        try {
            db = this.getWritableDatabase();
            rowsDeleted = db.delete(TABLE_RECIPE_INGREDIENTS, COLUMN_RECIPE_ID + " = ?", new String[]{String.valueOf(recipeId)});
        } finally {
            if (db != null) {
                db.close();
            }
        }
        return rowsDeleted;
    }
    public int getIngredientCountForRecipe(int recipeId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        int count = 0;

        try {
            cursor = db.rawQuery(
                    "SELECT COUNT(*) FROM " + TABLE_RECIPE_INGREDIENTS + " WHERE " + COLUMN_RECIPE_ID + " = ?",
                    new String[]{String.valueOf(recipeId)}
            );
            cursor.moveToFirst();
            count = cursor.getInt(0);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.close();
        }
        return count;
    }
    public List<Recipe> getRandomRecipes(int limit, String userEmail) {
        List<Recipe> recipes = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String query = "SELECT * FROM recipes WHERE " + KEY_USER_EMAIL + " = ? ORDER BY RANDOM() LIMIT ?";
        Cursor cursor = db.rawQuery(query, new String[]{userEmail, String.valueOf(limit)});

        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID));
                String title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE));
                String image = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_IMAGE));
                int likes = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_LIKES));
                int time = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TIME));
                int servings = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_SERVINGS));
                int favorite = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_FAVORITE));

                recipes.add(new Recipe(id, title, image, likes, time, servings, favorite));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();

        return recipes;
    }

    // SHOPPING LIST METHODS
    private static final String SHOPPING_TABLE_NAME = "ShoppingList";
    private static final String SHOPPING_COLUMN_ID = "ShoppingList_ItemID";
    private static final String SHOPPING_COLUMN_NAME = "ShoppingList_ItemName";
    private static final String SHOPPING_COLUMN_QTY = "ShoppingList_ItemQTY";
    private static final String SHOPPING_COLUMN_BOUGHT = "ShoppingList_ItemBought";


    private void createShoppingListTable(SQLiteDatabase db) {
        String query = "CREATE TABLE " + SHOPPING_TABLE_NAME +
                " (" + SHOPPING_COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                SHOPPING_COLUMN_NAME + " TEXT, " +
                SHOPPING_COLUMN_QTY + " INTEGER, " +
                SHOPPING_COLUMN_BOUGHT + " INTEGER, " +
                KEY_USER_EMAIL + " TEXT, " +
                "FOREIGN KEY(" + KEY_USER_EMAIL + ") REFERENCES " + TABLE_USERS + "(" + KEY_EMAIL + ") ON DELETE CASCADE);";
        db.execSQL(query);
    }
    // Modified AddItem method
    public long AddItem(String ItemName, int ItemQty, String userEmail){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();

        cv.put(SHOPPING_COLUMN_NAME, ItemName);
        cv.put(SHOPPING_COLUMN_QTY, ItemQty);
        cv.put(SHOPPING_COLUMN_BOUGHT, 0);
        cv.put(KEY_USER_EMAIL, userEmail);

        long results = db.insert(SHOPPING_TABLE_NAME, null, cv);

        if(results == -1) {
            Toast.makeText(context, "Failed", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, "Added Successfully", Toast.LENGTH_SHORT).show();
        }
        return results;
    }

    // Modified readAllData method
    public Cursor readAllData(String userEmail){
        String query = "SELECT * FROM " + SHOPPING_TABLE_NAME + " WHERE " + KEY_USER_EMAIL + " = ?";
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = null;
        if (db != null) {
            cursor = db.rawQuery(query, new String[]{userEmail});
        }
        return cursor;
    }

    // Modified deleteItemById method
    public void deleteItemById(int id, String userEmail) {
        SQLiteDatabase db = this.getWritableDatabase();
        int result = db.delete(SHOPPING_TABLE_NAME,
                SHOPPING_COLUMN_ID + "=? AND " + KEY_USER_EMAIL + "=?",
                new String[]{String.valueOf(id), userEmail});

        if (result == -1) {
            Toast.makeText(context, "Failed to delete item", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, "Item deleted", Toast.LENGTH_SHORT).show();
        }
    }

    // Modified UpdateItemShoppingList method
    public void UpdateItemShoppingList(int id, String userEmail){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(SHOPPING_COLUMN_BOUGHT, 1);
        db.update(SHOPPING_TABLE_NAME, cv,
                SHOPPING_COLUMN_ID + "=? AND " + KEY_USER_EMAIL + "=?",
                new String[]{String.valueOf(id), userEmail});
    }

    // Modified UpdateItemBackShoppingList method
    public void UpdateItemBackShoppingList(int id, String userEmail) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(SHOPPING_COLUMN_BOUGHT, 0);
        db.update(SHOPPING_TABLE_NAME, cv,
                SHOPPING_COLUMN_ID + "=? AND " + KEY_USER_EMAIL + "=?",
                new String[]{String.valueOf(id), userEmail});
    }
    public User loadUserByEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return null;
        }

        SQLiteDatabase db = this.getReadableDatabase();
        User user = null;

        String[] columns = {
                KEY_NAME, KEY_SURNAME, KEY_EMAIL, KEY_PHONE, KEY_PASSWORD, KEY_PROFILE
        };

        String selection = KEY_EMAIL + " = ?";
        String[] selectionArgs = {email};

        Cursor cursor = db.query(TABLE_USERS, columns, selection, selectionArgs, null, null, null);

        if (cursor.moveToFirst()) {
            user = cursorToUser(cursor);
        }

        cursor.close();
        db.close();

        return user;
    }
    private User cursorToUser(Cursor cursor) {
        User user = new User();

        user.setName(cursor.getString(cursor.getColumnIndexOrThrow(KEY_NAME)));
        user.setSurname(cursor.getString(cursor.getColumnIndexOrThrow(KEY_SURNAME)));
        user.setEmail(cursor.getString(cursor.getColumnIndexOrThrow(KEY_EMAIL)));
        user.setPhone(cursor.getString(cursor.getColumnIndexOrThrow(KEY_PHONE)));
        user.setPassword(cursor.getString(cursor.getColumnIndexOrThrow(KEY_PASSWORD)));

        byte[] profileImage = cursor.getBlob(cursor.getColumnIndexOrThrow(KEY_PROFILE));
        user.setProfileImage(profileImage);

        return user;
    }
    public int updatePassword(String email, String newPassword) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_PASSWORD, newPassword);

        String whereClause = KEY_EMAIL + " = ?";
        String[] whereArgs = {email};

        int result = db.update(TABLE_USERS, values, whereClause, whereArgs);
        db.close();

        return result;
    }
}