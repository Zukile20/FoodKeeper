package com.example.foodkeeper;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.foodkeeper.FoodItem.AddItemActivity;
import com.example.foodkeeper.FoodItem.FoodItem;
import com.example.foodkeeper.FoodItem.FoodItemAdapter;
import com.example.foodkeeper.FoodItem.ItemsViewActivity;
import com.example.foodkeeper.FoodItem.ViewAnItemActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class SearchActivity extends AppCompatActivity {
    private EditText searchEditText;
    private LinearLayout emptyState;
    private FoodItemAdapter foodItemAdapter;
    private List<FoodItem> allFoodItems;
    private FloatingActionButton fabAddItem;
    private TextView soonToExpireTab, allTab, empty;
    private Database db;
    private BottomNavigationView bottomNav;
    private Spinner spinnerCategory;
    private RecyclerView recyclerView;
    String[] items = {"Category", "Fats & Oils", "Fruit", "Vegetable", "Dairy", "Beverage", "Meat", "Sweet", "Junk"};
    SessionManager session;
    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        db = new Database(this);
        session= new SessionManager(this);

        SessionManager userSession = new SessionManager(this);

        if (!userSession.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        searchEditText = findViewById(R.id.search_bar);
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

        bottomNav.setOnNavigationItemSelectedListener(item ->{
            int id = item.getItemId();

            if(id == R.id.nav_home){
                startActivity(new Intent(SearchActivity.this, LandingPageActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if(id == R.id.nav_search){

                return true;
            } else if(id == R.id.nav_view){
                startActivity(new Intent(SearchActivity.this, ItemsViewActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            else if(id == R.id.nav_expiring) {
                startActivity(new Intent(SearchActivity.this, ExpiringActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_profileMenu) {
                startActivity(new Intent(SearchActivity.this, MenuActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else {
                return false;
            }

        });
        bottomNav.setSelectedItemId(R.id.nav_search);

        loadFoodItems();

        foodItemAdapter.setOnItemClickListener(new FoodItemAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(FoodItem foodItem) {
                showItemDetails(foodItem);
            }
        });

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {}

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterItems(s.toString(),"");
            }
        });
        AdapterView.OnItemSelectedListener categorySpinnerListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                filterItems(items[position], "category");
                soonToExpireTab.setTextColor(getResources().getColor(android.R.color.black));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        };

        allTab.setOnClickListener(v -> {
            spinnerCategory.setOnItemSelectedListener(null);
            spinnerCategory.getBackground().setColorFilter(ContextCompat.getColor(this, R.color.black), PorterDuff.Mode.SRC_ATOP);
            soonToExpireTab.setTextColor(getResources().getColor(android.R.color.black));
            spinnerCategory.setSelection(0);
            allTab.setTextColor(getResources().getColor(android.R.color.darker_gray));
            loadFoodItems();

            spinnerCategory.post(() -> {
                spinnerCategory.setOnItemSelectedListener(categorySpinnerListener);
            });
        });

        setupSpinnerCategory(categorySpinnerListener);
        soonToExpireTab.setOnClickListener(v -> {
            spinnerCategory.setOnItemSelectedListener(null);

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
                startActivity(new Intent(SearchActivity.this, AddItemActivity.class));
            }
        });
        allTab.performClick();
    }
    private void setupSpinnerCategory(AdapterView.OnItemSelectedListener categorySpinnerListener)
    {
        CustomSpinnerAdapter spinnerAdapter = new CustomSpinnerAdapter(this, android.R.layout.simple_spinner_item, items);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(spinnerAdapter);
        spinnerCategory.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        spinnerCategory.setOnItemSelectedListener(categorySpinnerListener);
    }
    private void filterItems(String query,String based) {
        if (allFoodItems == null || allFoodItems.isEmpty()) return;

        List<FoodItem> filtered = new ArrayList<>();
        String lowerCaseQuery = query.toLowerCase();
        if(Objects.equals(based, "category"))
        {
            for (FoodItem item : allFoodItems) {
                String name = item.getCategory() != null ? item.getCategory().toLowerCase() : "";
                if (name.contains(lowerCaseQuery)) {
                    filtered.add(item);
                }
            }
        }
        else {
            for (FoodItem item : allFoodItems) {
                String name = item.getName() != null ? item.getName().toLowerCase() : "";
                if (name.contains(lowerCaseQuery)) {
                    filtered.add(item);
                }
            }
        }

        foodItemAdapter.updateData(filtered);
        if (filtered.isEmpty()) {
            showEmptyState("No items match your search");
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
        Toast.makeText(this, selectedItem.getName() + " clicked", Toast.LENGTH_SHORT).show();
    }
    private void loadSoonToExpireItems() {
        List<FoodItem> userItems = db.getUserFoodItems(session.getUserEmail());
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
    protected void onResume(){
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