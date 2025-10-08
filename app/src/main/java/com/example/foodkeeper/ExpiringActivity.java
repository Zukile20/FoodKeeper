package com.example.foodkeeper;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.foodkeeper.FoodItem.FoodItem;
import com.example.foodkeeper.FoodItem.FoodItemAdapter;
import com.example.foodkeeper.FoodItem.ItemsViewActivity;
import com.example.foodkeeper.FoodItem.ViewAnItemActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ExpiringActivity extends AppCompatActivity {
    private EditText searchEditText;
    private RecyclerView recyclerView;
    private LinearLayout emptyState;
    private TextView empty;
    private FoodItemAdapter foodItemAdapter;
    private List<FoodItem> allFoodItems;
    private Database db;
    private BottomNavigationView bottomNav;
    private  SessionManager session;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_expiring);

        db = new Database(this);
        session = new SessionManager(this);

        searchEditText = findViewById(R.id.search_barExp);
        recyclerView = findViewById(R.id.recyclerViewExp);
        bottomNav = findViewById(R.id.bottomNavExp);
        emptyState = findViewById(R.id.emptyStateExp);
        empty = findViewById(R.id.emptyTextExp);

        bottomNav.setOnNavigationItemSelectedListener(item ->{
            int id = item.getItemId();

            if(id == R.id.nav_home){
                startActivity(new Intent(ExpiringActivity.this, LandingPageActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if(id == R.id.nav_search){
                startActivity(new Intent(ExpiringActivity.this, SearchActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if(id == R.id.nav_view){
                startActivity(new Intent(ExpiringActivity.this, ItemsViewActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            else if(id == R.id.nav_expiring) {

                return true;
            } else if (id == R.id.nav_profileMenu) {
                startActivity(new Intent(ExpiringActivity.this, MenuActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else {
                return false;
            }

        });
        bottomNav.setSelectedItemId(R.id.nav_expiring);

        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        foodItemAdapter = new FoodItemAdapter(this, new ArrayList<>());
        recyclerView.setAdapter(foodItemAdapter);

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
                filterItems(s.toString());
            }
        });

        loadSoonToExpireItems();
    }
    private void filterItems(String query) {
        List<FoodItem> filtered = new ArrayList<>();
        String lowerCaseQuery = query.toLowerCase();

        for (FoodItem item : allFoodItems) {
            String name = item.getName();

            if(name != null) {
                name = item.getName().toLowerCase();
            } else {
                name = "";
            }

            if (name.contains(lowerCaseQuery)) {
                filtered.add(item);
            }
        }

        foodItemAdapter.updateData(filtered);
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
    private void showEmptyState(String message) {
        recyclerView.setVisibility(View.GONE);
        emptyState.setVisibility(View.VISIBLE);
        empty.setText(message);
    }
    private void hideEmptyState() {
        recyclerView.setVisibility(View.VISIBLE);
        emptyState.setVisibility(View.GONE);
    }
    @Override
    protected void onDestroy() {
        db.close();
        super.onDestroy();
    }
}