package com.example.foodkeeper.Meal;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.foodkeeper.Database;
import com.example.foodkeeper.FoodItem.FoodItem;
import com.example.foodkeeper.R;
import com.example.foodkeeper.SessionManager;

import java.util.ArrayList;

public class FullView extends AppCompatActivity implements FoodSelectionAdapter.OnItemSelectionChangeListener {

    private ArrayList<FoodItem> FOOD_ITEMS = new ArrayList<>();
    private TextView counterTextView;
    private FoodSelectionAdapter adapter;
    private RecyclerView recyclerView;
    private Button backBtn;
    private EditText searchText;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_view);

        getIntentData();
        initializeViews();
        setupRecyclerView();
        setupListeners();
    }
    private void getIntentData() {
        Database db = Database.getInstance(this);
        session= new SessionManager(this);
        Intent intent = getIntent();
        if (intent != null) {
            // Get the FoodItem objects directly from prev.ious activity
            if (intent.hasExtra("foodItems")) {
                ArrayList<String> selectedItems = intent.getStringArrayListExtra("foodItems");
               FOOD_ITEMS = (ArrayList<FoodItem>)db.getFoodItemsInConnectedFridge(session.getUserEmail());
               markSelectedItems(selectedItems);

            }
        }
    }
    private void markSelectedItems(ArrayList<String> selectedItemIds) {
        for (FoodItem item : FOOD_ITEMS) {
            String itemId = String.valueOf(item.getId());
            item.setChecked(selectedItemIds.contains(itemId));
        }
    }

    private void setResultAndFinish() {
        // Get currently selected items from the adapter
        ArrayList<String> selectedItemIds = adapter.getSelectedItemIds();

        Intent resultIntent = new Intent();
        resultIntent.putStringArrayListExtra("selectedItems", selectedItemIds);
        setResult(RESULT_OK, resultIntent);
        finish();
        overridePendingTransition(0, R.anim.zoom_out);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        setResultAndFinish();
    }

    private void initializeViews() {
        backBtn = findViewById(R.id.backBtn);
        searchText = findViewById(R.id.searchField);
        recyclerView = findViewById(R.id.recyclerView); // Changed from listView to recyclerView
        counterTextView = findViewById(R.id.counterTextView_f);
    }

    private void setupRecyclerView() {
        // Create adapter
        adapter = new FoodSelectionAdapter(this, FOOD_ITEMS);
        adapter.setOnItemSelectionChangeListener(this);

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        updateCounter();
    }

    private void setupListeners() {
        backBtn.setOnClickListener(v -> setResultAndFinish());

        // Handle search functionality
        searchText.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                // Filter the adapter when search text changes
                adapter.getFilter().filter(s.toString());
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not needed
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Not needed
            }
        });
    }

    // Implement OnItemSelectionChangeListener interface
    @Override
    public void onSelectionChanged(int selectedCount) {
        updateCounterWithCount(selectedCount);
    }

    @Override
    public void onItemClick(FoodItem item, int position) {
        String message = item.getCheckState() ? "Selected: " : "Deselected: ";
        Toast.makeText(this, message + item.getName(), Toast.LENGTH_SHORT).show();
    }

    /**
     * Update the counter display
     */
    @SuppressLint("SetTextI18n")
    private void updateCounter() {
        int selectedCount = adapter != null ? adapter.getSelectedItemCount() : 0;
        counterTextView.setText("Selected : " + selectedCount);
    }

    /**
     * Update counter with specific count (called from callback)
     */
    @SuppressLint("SetTextI18n")
    private void updateCounterWithCount(int count) {
        counterTextView.setText("Selected : " + count);
    }

    /**
     * Optional: Get current selection count
     */
    public int getSelectionCount() {
        return adapter != null ? adapter.getSelectedItemCount() : 0;
    }

    /**
     * Optional: Add selected items programmatically
     */
    public void setSelectedItems(ArrayList<String> selectedItemIds) {
        if (adapter != null) {
            adapter.setSelectedItems(selectedItemIds);
        }
    }
}