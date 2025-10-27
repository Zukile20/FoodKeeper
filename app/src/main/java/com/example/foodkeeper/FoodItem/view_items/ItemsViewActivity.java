package com.example.foodkeeper.FoodItem.view_items;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.foodkeeper.FoodItem.AddItemActivity;
import com.example.foodkeeper.FoodItem.models.Category;
import com.example.foodkeeper.FoodItem.expiring.ExpiringActivity;
import com.example.foodkeeper.FoodItem.expiring.ExpiryCheckReceiver;
import com.example.foodkeeper.FoodItem.models.FoodItem;
import com.example.foodkeeper.FoodItem.FoodItemAdapter;
import com.example.foodkeeper.FoodItem.ViewAnItemActivity;
import com.example.foodkeeper.FoodkeeperUtils.Database;

import com.example.foodkeeper.LandingPage.LandingPageActivity;
import com.example.foodkeeper.menu_page.MenuActivity;
import com.example.foodkeeper.R;
import com.example.foodkeeper.FoodItem.SearchActivity;
import com.example.foodkeeper.Register.SessionManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ItemsViewActivity extends AppCompatActivity {
    private LinearLayout emptyState;
    private FoodItemAdapter foodItemAdapter;
    private List<FoodItem> allFoodItems=new ArrayList<>();
    private FloatingActionButton fabAddItem;
    private TextView soonToExpireTab, allTab, empty;
    private Database db;
    private BottomNavigationView bottomNav;
    private Spinner spinnerCategory;
    private RecyclerView recyclerView;
    private List<Category> categories;
    private String[] categoryNames;
    SessionManager session;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_items_view);

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, ExpiryCheckReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        long interval = AlarmManager.INTERVAL_DAY;
        long triggerAt = System.currentTimeMillis() + 5;

        alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                interval,
                pendingIntent
        );

        db = new Database(this);
        session = new SessionManager(this);

        loadCategories();

        recyclerView = findViewById(R.id.recyclerView);
        fabAddItem = findViewById(R.id.fabAddItem);
        allTab = findViewById(R.id.allTab);
        spinnerCategory = findViewById(R.id.categoryTab);
        soonToExpireTab = findViewById(R.id.soonToExpireTab);
        bottomNav = findViewById(R.id.bottomNav);
        emptyState = findViewById(R.id.emptyState);
        empty = findViewById(R.id.emptyText);

        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        foodItemAdapter = new FoodItemAdapter(this, new ArrayList<>());
        recyclerView.setAdapter(foodItemAdapter);

        bottomNav.setOnNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                startActivity(new Intent(ItemsViewActivity.this, LandingPageActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_search) {
                startActivity(new Intent(ItemsViewActivity.this, SearchActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_view) {
                return true;
            } else if (id == R.id.nav_expiring) {
                startActivity(new Intent(ItemsViewActivity.this, ExpiringActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_profileMenu) {
                startActivity(new Intent(ItemsViewActivity.this, MenuActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else {
                return false;
            }
        });
        bottomNav.setSelectedItemId(R.id.nav_view);

        loadFoodItems();

        foodItemAdapter.setOnItemClickListener(new FoodItemAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(FoodItem foodItem) {
                showItemDetails(foodItem);
            }
        });

        AdapterView.OnItemSelectedListener categorySpinnerListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                allTab.setTextColor(getResources().getColor(android.R.color.black));
                soonToExpireTab.setTextColor(getResources().getColor(android.R.color.black));

                if (position == 0) {
                    foodItemAdapter.updateData(new ArrayList<>(allFoodItems));
                    if (allFoodItems.isEmpty()) {
                        showEmptyState("You don't have any items yet");
                    } else {
                        hideEmptyState();
                    }
                } else {
                    Category selectedCategory = categories.get(position - 1);
                    filterItemsByCategoryId(selectedCategory.getId());
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        };

        allTab.setOnClickListener(v -> {
            spinnerCategory.setOnItemSelectedListener(null);
            spinnerCategory.getBackground().setColorFilter(ContextCompat.getColor(this, R.color.black), PorterDuff.Mode.SRC_ATOP);
            soonToExpireTab.setTextColor(getResources().getColor(android.R.color.black));
            allTab.setTextColor(getResources().getColor(android.R.color.darker_gray));
            spinnerCategory.setSelection(0);

            foodItemAdapter.updateData(new ArrayList<>(allFoodItems));
            if (allFoodItems.isEmpty()) {
                showEmptyState("You don't have any items yet");
            } else {
                hideEmptyState();
            }

            spinnerCategory.post(() -> {
                spinnerCategory.setOnItemSelectedListener(categorySpinnerListener);
            });
        });

        setupSpinnerCategory(categorySpinnerListener);

        soonToExpireTab.setOnClickListener(v -> {
            spinnerCategory.setOnItemSelectedListener(null);
            allTab.setTextColor(getResources().getColor(android.R.color.black));
            soonToExpireTab.setTextColor(getResources().getColor(android.R.color.darker_gray));
            spinnerCategory.getBackground().setColorFilter(ContextCompat.getColor(this, R.color.black), PorterDuff.Mode.SRC_ATOP);
            spinnerCategory.setSelection(0);
            loadSoonToExpireItems();

            spinnerCategory.post(() -> {
                spinnerCategory.setOnItemSelectedListener(categorySpinnerListener);
            });
        });

        fabAddItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ItemsViewActivity.this, AddItemActivity.class));
            }
        });
    }

    private void loadCategories() {
        categories = db.getAllCategories();

        categoryNames = new String[categories.size() + 1];
        categoryNames[0] = "Category";

        for (int i = 0; i < categories.size(); i++) {
            categoryNames[i + 1] = categories.get(i).getName();
        }
    }

    private void setupSpinnerCategory(AdapterView.OnItemSelectedListener categorySpinnerListener) {
        CustomSpinnerAdapter spinnerAdapter = new CustomSpinnerAdapter(this, android.R.layout.simple_spinner_item, categoryNames);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(spinnerAdapter);
        spinnerCategory.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        spinnerCategory.setOnItemSelectedListener(categorySpinnerListener);
    }

    private void filterItemsByCategoryId(int categoryId) {
        if (allFoodItems == null || allFoodItems.isEmpty()) {
            showEmptyState("You don't have any items yet");
            return;
        }

        List<FoodItem> filtered = new ArrayList<>();

        for (FoodItem item : allFoodItems) {
            String itemCategoryId = item.getCategory();
            if (itemCategoryId != null) {
                try {
                    int itemCatId = Integer.parseInt(itemCategoryId.trim());
                    if (itemCatId == categoryId) {
                        filtered.add(item);
                    }
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
        }

        foodItemAdapter.updateData(filtered);
        if (filtered.isEmpty()) {
            showEmptyState("No items in this category");
        } else {
            hideEmptyState();
        }
    }

    private void showEmptyState(String message) {
        recyclerView.setVisibility(View.GONE);
        emptyState.setVisibility(View.VISIBLE);
        empty.setText(message);
    }

    private void hideEmptyState() {
        recyclerView.setVisibility(View.VISIBLE);
        emptyState.setVisibility(View.GONE);
    }

    private void loadFoodItems() {
        List<FoodItem> userItems = db.getFoodItemsInConnectedFridge(session.getUserEmail());
        if (userItems.isEmpty()) {
            showEmptyState("You don't have any items yet");
        } else {
            hideEmptyState();
            allFoodItems = userItems;
            foodItemAdapter.updateData(new ArrayList<>(allFoodItems));
        }
    }

    private void showItemDetails(FoodItem selectedItem) {
        Intent intent = new Intent(this, ViewAnItemActivity.class);
        intent.putExtra("foodItem", selectedItem);
        startActivity(intent);
    }

    private void loadSoonToExpireItems() {
        List<FoodItem> userItems = db.getFoodItemsInConnectedFridge(session.getUserEmail());
        List<FoodItem> soonToExpireItems = new ArrayList<>();

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Date today = calendar.getTime();

        calendar.add(Calendar.DAY_OF_YEAR, 3);
        Date thresholdDate = calendar.getTime();

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

        for (FoodItem item : userItems) {
            try {
                Date expiryDate = dateFormat.parse(item.getExpiryDate().trim());

                if (expiryDate != null && !expiryDate.before(today) && !expiryDate.after(thresholdDate)) {
                    soonToExpireItems.add(item);
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        foodItemAdapter.updateData(soonToExpireItems);

        if (soonToExpireItems.isEmpty()) {
            showEmptyState("No items expiring within the next 3 days");
        } else {
            hideEmptyState();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadFoodItems();
    }

    @Override
    protected void onDestroy() {
        db.close();
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            loadFoodItems();
        }
    }
}