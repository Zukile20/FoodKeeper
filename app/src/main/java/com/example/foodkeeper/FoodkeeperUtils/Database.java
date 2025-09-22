package com.example.foodkeeper.FoodkeeperUtils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.Nullable;

import com.example.foodkeeper.FoodItem.FoodItem;
import com.example.foodkeeper.Meal.Meal;
import com.example.foodkeeper.MealPlan.CalendarUtils;
import com.example.foodkeeper.MealPlan.MealPlan;
import com.example.foodkeeper.R;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Database extends SQLiteOpenHelper {
    private static Database instance;
    private static final String DB_NAME = "foodkeeper.db";
    private static final int DB_VERSION = 1;

    public Database(@Nullable Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createTables(db);

        preloadCategories(db);
    }



    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Handle database upgrades here
        // For now, we'll just recreate the tables
        db.execSQL("DROP TABLE IF EXISTS MealPlan");
        db.execSQL("DROP TABLE IF EXISTS MealFoodItem");
        db.execSQL("DROP TABLE IF EXISTS Meal");
        onCreate(db);
    }
    public List<FoodItem> getAllFoodItems() {
        List<FoodItem> foodItemList = new ArrayList<>();

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM FoodItem", null);

        if (cursor.moveToFirst()) {
            do {
                int foodItemID = cursor.getInt(cursor.getColumnIndexOrThrow("foodItemID"));
                String name = cursor.getString(cursor.getColumnIndexOrThrow("foodItemName"));
                String expiryDate = cursor.getString(cursor.getColumnIndexOrThrow("expiryDate"));
                byte[] imageUri = cursor.getBlob(cursor.getColumnIndexOrThrow("imageUri"));
                int qty = cursor.getInt(cursor.getColumnIndexOrThrow("foodItemQty"));
                String categoryID = cursor.getString(cursor.getColumnIndexOrThrow("categoryID"));
                int inShoppingList = cursor.getInt(cursor.getColumnIndexOrThrow("isInShoppingList"));

                // Check if shoppingListID is null
                FoodItem foodItem = new FoodItem(foodItemID,name,categoryID.toString(),expiryDate.toString(),qty,imageUri,inShoppingList);
                foodItemList.add(foodItem);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();

        return foodItemList;
    }

    private void createTables(SQLiteDatabase db) {
        // Creating User table
        String createUserTable = "CREATE TABLE User(" +
                "userID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "userLName TEXT NOT NULL, " +
                "userFName TEXT NOT NULL, " +
                "userEmailAddress TEXT NOT NULL, " +
                "userPhoneNum TEXT, " +
                "userPassword TEXT NOT NULL" +
                ")";
        db.execSQL(createUserTable);

        // Creating Fridge table
        String createFridgeTable = "CREATE TABLE Fridge (" +
                "fridgeID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "fridgeBrand TEXT NOT NULL, " +
                "fridgeModel TEXT NOT NULL, " +
                "userID INTEGER NOT NULL, " +
                "FOREIGN KEY (userID) REFERENCES User(userID) ON DELETE CASCADE" +
                ")";
        db.execSQL(createFridgeTable);

        // Creating Category table
        String createCategoryTable = "CREATE TABLE Category (" +
                "categoryID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "categoryName TEXT NOT NULL" +
                ")";
        db.execSQL(createCategoryTable);

        // Create FoodItem table with foreign keys to Category and ShoppingList
        String createFoodItemTable = "CREATE TABLE FoodItem (" +
                "foodItemID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "foodItemName TEXT NOT NULL, " +
                "imageUri BLOB, " +
                "expiryDate TEXT NOT NULL, " +
                "foodItemQty INTEGER NOT NULL, " +
                "categoryID INTEGER NOT NULL, " +
                "isInShoppingList INTEGER, " +
                "FOREIGN KEY (categoryID) REFERENCES Category(categoryID) ON DELETE CASCADE " +
                ")";
        db.execSQL(createFoodItemTable);
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

    private void preloadCategories(SQLiteDatabase db) {
        String[] categories = {
                "Fruits",         // Moved to first position since preloaded items are fruits
                "Dairy",          // Milk, cheese, yogurt, etc.
                "Meat",           // Chicken, beef, pork, etc.
                "Seafood",        // Fish, shrimp, etc.
                "Vegetables",     // Lettuce, carrots, broccoli, etc.
                "Beverages",      // Juice, soda, water, etc.
                "Condiments",     // Ketchup, mustard, mayo, etc.
                "Desserts",       // Pudding, custard, etc.
                "Fermented",      // Sauerkraut, kimchi, etc.
        };

        for (String category : categories) {
            ContentValues values = new ContentValues();
            values.put("categoryName", category);
            db.insert("Category", null, values);
        }
    }

    public long createMeal(String mealName, String uri) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            ContentValues values = new ContentValues();
            values.put("mealName", mealName);
            if(uri!=null)
            {
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

                // Set IDs directly as Long objects, or convert appropriately
                plan.setBreakFast(breakfastID);
                plan.setLunch(lunchID);
                plan.setDinner(dinnerID);
                plan.setSnack(snackID);

                return plan;
            }
        } catch (SQLException e) {
            // Log the error with context
            Log.e("MealPlanDB", "Error fetching meal plan for date: " + planDay, e);
            throw new RuntimeException("Failed to fetch meal plan", e);
        }

        return null; // No meal plan found for this date
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

        // Update the existing MealPlan row for the given date
        int rowsAffected = db.update("MealPlan", values, "planDay = ?", new String[]{planDayStr});

        //  if no row exists yet, insert a new one
        if (rowsAffected == 0) {
            values.put("planDay", planDayStr);
            db.insert("MealPlan", null, values);
        }
    }
    public List<FoodItem> getFoodItemsForMeal(long mealId) {
        List<FoodItem> foodItems = new ArrayList<>();

        SQLiteDatabase db = this.getReadableDatabase();

        String query = "SELECT f.foodItemID, f.foodItemName, f.imageUri, f.expiryDate, " +
                "f.foodItemQty, f.categoryID, f.shoppingListID " +
                "FROM FoodItem f " +
                "INNER JOIN MealFoodItem mfi ON f.foodItemID = mfi.foodItemID " +
                "WHERE mfi.mealID = ?";

        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(mealId)});

        if (cursor.moveToFirst()) {
            do {
                int foodItemID = cursor.getInt(cursor.getColumnIndexOrThrow("foodItemID"));
                String name = cursor.getString(cursor.getColumnIndexOrThrow("foodItemName"));
                byte[]  imageUri = cursor.getBlob(cursor.getColumnIndexOrThrow("imageUri"));
                int qty = cursor.getInt(cursor.getColumnIndexOrThrow("foodItemQty"));

                String expiryDateStr = cursor.getString(cursor.getColumnIndexOrThrow("expiryDate"));
                LocalDate expiryDate = LocalDate.parse(expiryDateStr);

                String categoryID = cursor.getString(cursor.getColumnIndexOrThrow("categoryID"));
                int isInShoppingList= cursor.getInt(cursor.getColumnIndexOrThrow("shoppingListID"));

                FoodItem foodItem = new FoodItem(foodItemID,name,categoryID.toString(),expiryDate.toString(),qty,imageUri,isInShoppingList);
                foodItems.add(foodItem);
            } while (cursor.moveToNext());
        }

        cursor.close();
        return foodItems;
    }
    // Recipes table
    public static final String TABLE_RECIPES = "recipes";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_TITLE = "Recipe_Name";
    public static final String COLUMN_IMAGE = "Recipe_image";
    public static final String COLUMN_LIKES = "Recipe_likes";
    public static final String COLUMN_TIME = "Cooking_time";
    public static final String COLUMN_SERVINGS = "Recipe_servings";
    public static final String COLUMN_FAVORITE = "Favorite";


    // Ingredients table
    public static final String TABLE_INGREDIENTS = "Ingredients";
    public static final String COLUMN_INGREDIENT_ID = "Ingredient_id";
    public static final String COLUMN_INGREDIENT_NAME = "Ingredient_Name";

    // Recipe_Ingredients
    public static final String TABLE_RECIPE_INGREDIENTS = "Recipe_Ingredients";


    // Instructions table
    public static final String TABLE_INSTRUCTIONS = "Instructions";
    public static final String COLUMN_INSTRUCTION_ID = "Instruction_id";
    public static final String COLUMN_STEP_NUMBER = "Step_number";
    public static final String COLUMN_STEPS = "Step";
    public static final String COLUMN_RECIPE_ID="Recipe_id";
    private void createRecipeTables(SQLiteDatabase db)
    {
        db.execSQL("PRAGMA foreign_keys=ON");

        // Recipes table
        db.execSQL("CREATE TABLE " + TABLE_RECIPES + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY, " +
                COLUMN_TITLE + " TEXT, " +
                COLUMN_IMAGE + " TEXT, " +
                COLUMN_LIKES + " INTEGER, " +
                COLUMN_TIME + " INTEGER, " +
                COLUMN_SERVINGS + " INTEGER, " +
                COLUMN_FAVORITE + " INTEGER DEFAULT 0)");

        // Ingredients table
        db.execSQL("CREATE TABLE " + TABLE_INGREDIENTS + " (" +
                COLUMN_INGREDIENT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_INGREDIENT_NAME + " TEXT NOT NULL UNIQUE)");

        // Recipe_Ingredients link table
        db.execSQL("CREATE TABLE " + TABLE_RECIPE_INGREDIENTS + " (" +
                COLUMN_RECIPE_ID + " INTEGER, " +
                COLUMN_INGREDIENT_ID + " INTEGER, " +
                "PRIMARY KEY (" + COLUMN_RECIPE_ID + ", " + COLUMN_INGREDIENT_ID + "), " +
                "FOREIGN KEY (" + COLUMN_RECIPE_ID + ") REFERENCES " + TABLE_RECIPES + "(" + COLUMN_ID + ") ON DELETE CASCADE, " +
                "FOREIGN KEY (" + COLUMN_INGREDIENT_ID + ") REFERENCES " + TABLE_INGREDIENTS + "(" + COLUMN_INGREDIENT_ID + ") ON DELETE CASCADE)");

        // Instructions table
        db.execSQL("CREATE TABLE " + TABLE_INSTRUCTIONS + " (" +
                COLUMN_INSTRUCTION_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_RECIPE_ID + " INTEGER, " +
                COLUMN_STEP_NUMBER + " INTEGER, " +
                COLUMN_STEPS + " TEXT, " +
                "FOREIGN KEY (" + COLUMN_RECIPE_ID + ") REFERENCES " + TABLE_RECIPES + "(" + COLUMN_ID + ") ON DELETE CASCADE)");


    }

    public void deleteMealplan(LocalDate date)
    {
       SQLiteDatabase db= this.getWritableDatabase();
        String sql = "DELETE FROM mealPlan WHERE planDay = ?";
        db.execSQL(sql,new Object[]{date.toString()});
    }
    public  void deleteExpiredMealPlans()
    {
        SQLiteDatabase db = this.getWritableDatabase();

        String sql ="DELETE FROM mealPlan WHERE planDay < ?";
        LocalDate curDate = CalendarUtils.selectedDate;
        db.execSQL(sql,new Object[]{curDate.toString()});
    }
    public void deleteMealPlan(LocalDate planDay)
    {
        SQLiteDatabase db = this.getWritableDatabase();
        String sql = "DELETE FROM mealPlan WHERE planDay = ?";
        db.execSQL(sql,new Object[]{planDay.toString()});
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
            if(meal.getLastUsed()!=null)
            {
                mealValues.put("lastUsed",meal.getLastUsed().toString());
            }

            if (mealValues.size() > 0) {
                int mealRowsAffected = db.update("Meal", mealValues, "mealID = ?",
                        new String[]{String.valueOf(meal.getMealID())});

                if (mealRowsAffected == 0) {
                    // Meal doesn't exist
                    return false;
                }
            }

            db.delete("MealFoodItem", "mealID = ?",
                    new String[]{String.valueOf(meal.getMealID())});

            if (meal.getFoodItemIDs() != null && !meal.getFoodItemIDs().isEmpty()) {
                for (String foodItemID : meal.getFoodItemIDs()) {
                    if (foodItemID != null && Integer.parseInt(foodItemID )> 0) {

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

            // Create meal object
            meal = new Meal(mealID, name, R.drawable.image_placeholder);
            meal.setUrl(image);
            if (lastUsed != null) {
                meal.setLastUsed(LocalDate.parse(lastUsed));
            }

            // Getting associated food item IDs from MealFoodItem table
            Cursor foodItemsCursor = db.rawQuery(
                    "SELECT foodItemID FROM MealFoodItem WHERE mealID = ?",
                    new String[]{String.valueOf(mealID)}
            );

            ArrayList<String> foodItemIDs = new ArrayList<>();
            while (foodItemsCursor.moveToNext()) {
                String foodItemID = String.valueOf(foodItemsCursor.getLong(foodItemsCursor.getColumnIndexOrThrow("foodItemID")));
                foodItemIDs.add(foodItemID);
            }

            // Setting the food item IDs to the meal
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
    public void deleteMeal(Meal meal)
    {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete("Meal","mealID =?",new String[]{String.valueOf(meal.getMealID())});
    }
    private static  final String SHOPPING_TABLE_NAME= "my_shoppingList";

    private static final String SHOPPING_COLUMN_ID="ShoppingList_ItemID";
    private static final String SHOPPING_COLUMN_NAME="ShoppingList_ItemName";
    private static final String SHOPPING_COLUMN_QTY="ShoppingList_ItemQTY";
    private static final String SHOPPING_COLUMN_BOUGHT="ShoppingList_ItemBought";
    private void createShoppingListTable(SQLiteDatabase db)
    {

        String query= "CREATE TABLE " + SHOPPING_TABLE_NAME +
                " (" + SHOPPING_COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                SHOPPING_COLUMN_NAME + " TEXT, " +
                SHOPPING_COLUMN_QTY + " INTEGER ," +
                SHOPPING_COLUMN_BOUGHT + " INTEGER);";
        db.execSQL(query);
    }
    private void createMealPlanMealTables(SQLiteDatabase db)
    {
        // Create Meal table
        String createMealTable = "CREATE TABLE Meal (" +
                "mealID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "mealName TEXT NOT NULL," +
                "mealImage TEXT ," +
                "lastUsed TEXT"+
                ")";
        db.execSQL(createMealTable);

        // Create MealFoodItem join table with quantity and foreign keys
        // Create MealFoodItem table with COMPOSITE PRIMARY KEY
        String createMealFoodItemTable = "CREATE TABLE MealFoodItem (" +
                "mealID INTEGER NOT NULL, " +
                "foodItemID INTEGER NOT NULL, " +
                "PRIMARY KEY (mealID, foodItemID), " +
                "FOREIGN KEY (mealID) REFERENCES Meal(mealID) ON DELETE CASCADE, " +
                "FOREIGN KEY (foodItemID) REFERENCES FoodItem(foodItemID) ON DELETE CASCADE" +
                ")";
        db.execSQL(createMealFoodItemTable);


        // Creating MealPlan table
        String createMealPlanTable = "CREATE TABLE MealPlan (" +
                "    planDay TEXT NOT NULL," +
                "    breakfastMealID INTEGER," +
                "    lunchMealID INTEGER," +
                "    dinnerMealID INTEGER," +
                "    snackMealID INTEGER," +
                "    FOREIGN KEY (breakfastMealID) REFERENCES Meal(mealID)," +
                "    FOREIGN KEY (lunchMealID) REFERENCES Meal(mealID)," +
                "    FOREIGN KEY (dinnerMealID) REFERENCES Meal(mealID)," +
                "    FOREIGN KEY (snackMealID) REFERENCES Meal(mealID)" +
                ")";
        db.execSQL(createMealPlanTable);
    }

}