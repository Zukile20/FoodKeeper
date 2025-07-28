package com.example.foodkeeper;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;

public class ItemsViewActivity extends AppCompatActivity {
    private GridView gridView;
    private ArrayAdapter<String> adapter;
    private ArrayList<String> foodItems = new ArrayList<>();
    private FloatingActionButton fabAddItem;
    private TextView categoryTab, soonToExpireTab;
    private Database dbHelper;
    private String currentUserEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_items_view);

        dbHelper = new Database(this);

        currentUserEmail = "user@example.com";

        gridView = findViewById(R.id.gridView);
        fabAddItem = findViewById(R.id.fabAddItem);
        categoryTab = findViewById(R.id.categoryTab);
        soonToExpireTab = findViewById(R.id.soonToExpireTab);
        ImageButton backButton = findViewById(R.id.backButton);

        adapter = new ArrayAdapter<String>(this, R.layout.item_food, R.id.foodItemText, foodItems) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = view.findViewById(R.id.foodItemText);
                textView.setText(getItem(position));
                return view;
            }
        };
        gridView.setAdapter(adapter);

        loadFoodItems();

        categoryTab.setOnClickListener(v -> {
            categoryTab.setTextColor(getResources().getColor(android.R.color.black));
            soonToExpireTab.setTextColor(getResources().getColor(android.R.color.darker_gray));
            loadFoodItems();
        });

        soonToExpireTab.setOnClickListener(v -> {
            soonToExpireTab.setTextColor(getResources().getColor(android.R.color.black));
            categoryTab.setTextColor(getResources().getColor(android.R.color.darker_gray));
            loadSoonToExpireItems();
        });

        fabAddItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(), "CLICKED", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(ItemsViewActivity.this, AddItemActivity.class));
            }
        });
    }

    private void loadFoodItems() {
        foodItems.clear();
        Cursor cursor = dbHelper.getAllFoodItems(currentUserEmail);
        if (cursor.moveToFirst()) {
            do {
                @SuppressLint("Range") String itemName = cursor.getString(cursor.getColumnIndex("item_name"));
                foodItems.add(itemName.toUpperCase());
            } while (cursor.moveToNext());
        }
        cursor.close();
        adapter.notifyDataSetChanged();
    }

    private void loadSoonToExpireItems() {
        foodItems.clear();
        Cursor cursor = dbHelper.getItemsByCategory("perishable", currentUserEmail);
        if (cursor.moveToFirst()) {
            do {
                @SuppressLint("Range") String itemName = cursor.getString(cursor.getColumnIndex("item_name"));
                foodItems.add(itemName.toUpperCase());
            } while (cursor.moveToNext());
        }
        cursor.close();
        adapter.notifyDataSetChanged();
    }

    @Override
    protected void onDestroy() {
        dbHelper.close();
        super.onDestroy();
    }
}