package com.example.foodkeeper.ViewMeals;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.foodkeeper.FoodkeeperUtils.Database;
import com.example.foodkeeper.FoodkeeperUtils.DeleteConfirmationDialog;
import com.example.foodkeeper.Meal.CreateMealActivity;
import com.example.foodkeeper.Meal.Meal;
import com.example.foodkeeper.Meal.UpdateMealActivity;
import com.example.foodkeeper.Meal.ViewMealActivity;
import com.example.foodkeeper.R;
import com.example.foodkeeper.Register.SessionManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class mealsViewActivity extends AppCompatActivity  {

    //Initialize the ui components
    private ImageButton backButton;
    private FloatingActionButton createMealButton;
    private LinearLayout emptyStateLayout,loadingLayout;
    private Spinner sortSpinner;
    private RecyclerView mealsRecyclerView;
    Database db ;
    //Data
    private MealAdapter mealAdapter;
    private List<Meal> allMeals;
    private List<Meal> filteredMeals;
    private TextView selectTitle;

    //constants
    private static final int REQUEST_CREATE_MEAL = 1001;
    private String mealType;

    private ItemTouchHelper itemTouchHelper;
   // SwipeRevealCallback swipeRevealCallback;

    private int ACTIVITY_MODE =0;
    private final int SELECTION_MODE=1;
    SessionManager sess;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_meals_view);
        getMealType();
        initViews();
        setUpSpinner();
        setupRecyclerView();
        setOnClickListeners();
       // setupSwipeToDelete();
        loadMeals();
    }
    public void getMealType()
    {
        Intent intent = getIntent();
        mealType = intent.getStringExtra("mealType");
    }
    @SuppressLint("ClickableViewAccessibility")
    private void setupRecyclerView() {
        allMeals = new ArrayList<>();
        filteredMeals = new ArrayList<>();
        mealAdapter = new MealAdapter( this,filteredMeals, new MealAdapter.OnMealActionListener() {
            public void showDeleteConfirmation(Meal meal,int position,String message) {
                if (position >= 0 && position < filteredMeals.size()) {

                    DeleteConfirmationDialog dialog = DeleteConfirmationDialog.newInstance("meal",message,"Delete all");

                    dialog.setOnDeleteConfirmListener(new DeleteConfirmationDialog.OnDeleteConfirmListener() {
                        @Override
                        public void onDeleteConfirmed() {
                            db.deleteMealPlansForMeal((int) meal.getMealID());

                            try {
                                db.deleteMeal(meal);
                                mealAdapter.notifyItemRemoved(position);
                                Toast.makeText(mealsViewActivity.this,
                                        "Meal removed from meal plans and deleted",
                                        Toast.LENGTH_SHORT).show();
                            } catch (Exception e) {
                                Toast.makeText(mealsViewActivity.this,
                                        "Error: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            }
                            filteredMeals.remove(position);
                            mealAdapter.notifyItemRemoved(position);
                            mealAdapter.notifyItemRangeChanged(position, filteredMeals.size());
                            updateEmptyState();// if the meals now are empty
                            Toast.makeText(mealsViewActivity.this, "Meal deleted", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onDeleteCancelled() {
                            mealAdapter.notifyItemChanged(position);
                        }
                    });

                        dialog.show(mealsViewActivity.this.getSupportFragmentManager(), "delete_dialog");
                }

            }

            @Override
            public void onItemEdit(Meal meal, int position) {
                Intent intent = new Intent(mealsViewActivity.this, UpdateMealActivity.class);
                intent.putExtra("EditMeal", meal.getMealID());
                startActivity(intent);
            }

            @Override
            public void onItemDelete(Meal meal, int position) {
                try {
                    db.deleteMeal(meal);
                    showDeleteConfirmation(meal,position,"");
                } catch (Exception e) {
                    showDeleteConfirmation(meal,position,e.getMessage());
                }
            }

            @Override
            public void onItemClick(Meal meal, int position) {
                if(ACTIVITY_MODE==SELECTION_MODE)
                {
                    Intent resIntent= new Intent();
                    resIntent.putExtra("selectedMeal",meal.getMealID());
                    resIntent.putExtra("mealType",mealType);
                    setResult(RESULT_OK,resIntent);
                    finish();
                }
                else
                {
                    //The selection must lead to viewing an item
                    Intent intent = new Intent(getApplicationContext() , ViewMealActivity.class);
                    intent.putExtra("viewMeal",meal.getMealID());
                    startActivity(intent);
                }

            }
        });
        mealsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mealsRecyclerView.setAdapter(mealAdapter);

    }
    private void initViews()
    {
        db =   Database.getInstance(this);
        sess=new SessionManager(this);
        ACTIVITY_MODE = getIntent().getIntExtra("ACTIVITY_MODE",0);

        backButton = findViewById(R.id.backButton);
        sortSpinner = findViewById(R.id.sortSpinner);
        mealsRecyclerView = findViewById(R.id.mealsRecyclerView);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        loadingLayout = findViewById(R.id.loadingLayout);
        createMealButton = findViewById(R.id.createMealButton);
        selectTitle = findViewById(R.id.selectTitle);

        if(ACTIVITY_MODE==SELECTION_MODE)
        {
            selectTitle.setText("Select your "+ mealType.toLowerCase()+" meal");
            selectTitle.setVisibility(VISIBLE);
        }
        else
        {
            selectTitle.setVisibility(GONE);

        }
    }
    private  void setUpSpinner()
    {
        String[] sortOptions =  {"A-Z", "Z-A", "Recent"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, sortOptions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sortSpinner.setAdapter(adapter);
        sortSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                sortMeals(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
             }
        });


    }

    private void setOnClickListeners() {
        backButton.setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });
        createMealButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, CreateMealActivity.class);
            startActivityForResult(intent, REQUEST_CREATE_MEAL);
        });
    }
    private void sortMeals(int sortType) {
        switch (sortType) {
            case 0: // A-Z
                Collections.sort(filteredMeals, (m1, m2) -> m1.getMealName().compareToIgnoreCase(m2.getMealName()));
                break;
            case 1: // Z-A
                Collections.sort(filteredMeals, (m1, m2) -> m2.getMealName().compareToIgnoreCase(m1.getMealName()));
                break;
            case 2:
                Collections.sort(filteredMeals, (m1, m2) -> {
                    LocalDate lastUsed1 = m1.getLastUsed();
                    LocalDate lastUsed2 = m2.getLastUsed();

                    if (lastUsed1 == null && lastUsed2 == null) {
                        return 0; // Both null, consider equal
                    }
                    if (lastUsed1 == null) {
                        return 1; // null goes to end (less recent)
                    }
                    if (lastUsed2 == null) {
                        return -1; // null goes to end (less recent)
                    }

                    return lastUsed2.compareTo(lastUsed1);
                });
                break;
        }
        mealAdapter.notifyDataSetChanged();
    }
    private List<Meal> loadData() {

        List<Meal> meals = new ArrayList<>();
        meals.addAll(db.getMealsInConnectedFridge(sess.getUserEmail()));
        return meals;
    }
    private void loadMeals() {
        showLoadingState();

        new Thread(() -> {
            try {
                Thread.sleep(100); // Simulate network delay

                List<Meal> meals = loadData();


                runOnUiThread(() -> {
                    allMeals.clear();
                    allMeals.addAll(meals);
                    filteredMeals.clear();
                    filteredMeals.addAll(meals);
                    mealAdapter.notifyDataSetChanged();

                    hideLoadingState();
                    updateEmptyState();
                });

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }
    private void showLoadingState() {
        loadingLayout.setVisibility(VISIBLE);
        mealsRecyclerView.setVisibility(GONE);
        emptyStateLayout.setVisibility(GONE);
    }

    private void hideLoadingState() {
        loadingLayout.setVisibility(GONE);
    //    mealsRecyclerView.setVisibility(View.VISIBLE);
    }

    private void updateEmptyState() {
        if (filteredMeals.isEmpty()) {
            emptyStateLayout.setVisibility(VISIBLE);
            mealsRecyclerView.setVisibility(GONE);
        } else {
            emptyStateLayout.setVisibility(GONE);
            mealsRecyclerView.setVisibility(VISIBLE);
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CREATE_MEAL && resultCode == RESULT_OK) {
            loadMeals();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadMeals();
    }
}