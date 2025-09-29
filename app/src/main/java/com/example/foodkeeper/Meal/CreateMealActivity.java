package com.example.foodkeeper.Meal;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.foodkeeper.FoodItem.FoodItem;
import com.example.foodkeeper.FoodkeeperUtils.NotificationHelper;
import com.example.foodkeeper.R;
import com.example.foodkeeper.SessionManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

public class CreateMealActivity extends AppCompatActivity implements FoodSelectionAdapter.OnItemSelectionChangeListener {
    com.example.foodkeeper.Database db ;

    private boolean imageSelectedForCurrentMeal = false;
    // UI Components
    private EditText searchText;
    private EditText nameField;
    private RecyclerView recyclerView;
    private Button loadImageBtn;
    private ImageView mealImage;
    private Button backBtn,createBtn;
    private Uri selectedImageUri;
    private Button fullViewBtn;
    private TextView counterTextViewer;


    // Adapter
    private FoodSelectionAdapter foodItemAdapter;

    private ActivityResultLauncher<Intent> resultLauncher;
    private ActivityResultLauncher<Intent> fullViewLauncher;
    private SessionManager session;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.create_meal_activity);
        db=   com.example.foodkeeper.Database.getInstance(this);
        session= new SessionManager(this);
        initializeViews();
       initiaLizeData(db);
        setupAdapter();
        fullViewLauncher = registerForActivityResult( new ActivityResultContracts.StartActivityForResult(), result->
                {
                    if (result.getResultCode() == RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null) {
                            ArrayList<String> selectedItems = data.getStringArrayListExtra("selectedItems");
                            markSelectedItems(selectedItems);
                            foodItemAdapter.notifyDataSetChanged();
                            updateCounter();
                        }
                    }
                }
        );
      setupListeners();
       setupImagePicker();
    }

    private void initializeViews() {
        recyclerView = findViewById(R.id.recyclerView);
        loadImageBtn = findViewById(R.id.loadPicture);
        mealImage = findViewById(R.id.mealImage);
        nameField = findViewById(R.id.nameField);
        backBtn = findViewById(R.id.backBtn);
        searchText = findViewById(R.id.searchField);
        fullViewBtn =findViewById(R.id.fullViewBtn);
        createBtn= findViewById(R.id.createBtn);
        counterTextViewer = findViewById(R.id.counterTextView);
    }

    private void setupListeners() {
        backBtn.setOnClickListener(view -> finish());
        loadImageBtn.setOnClickListener(v -> pickImage());
        searchText.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                foodItemAdapter.getFilter().filter(s.toString());
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });

        fullViewBtn.setOnClickListener(v ->openFullView());
        createBtn.setOnClickListener(v->
        {
            String mealName = String.valueOf(nameField.getText());
          if(foodItemAdapter.getSelectedItemCount()==0 || mealName.length()==0)
          {
              if (mealName.isEmpty()) {
                  Toast.makeText(this, "Meal name cannot be empty", Toast.LENGTH_SHORT).show();
              }
              else {
                  Toast.makeText(this, "At least one item must be selected", Toast.LENGTH_SHORT).show();
              }
          } else {
              if (db != null)
              {
                  //creating the meal......with its fooditems
                   createNewMeal(mealName);
                  Toast.makeText(this,"Meal has been created successfully",Toast.LENGTH_SHORT).show();
                  setResult(RESULT_OK);
                  finish();
              }
          }

        });
    }

    private String createNewMeal(String name)
    {
     //get the id for the meal
        try {

            String imageUri = null;

            if (imageSelectedForCurrentMeal && selectedImageUri != null) {
                imageUri = saveImageToInternalStorage(selectedImageUri);
            }

            imageSelectedForCurrentMeal = false;
            selectedImageUri = null;

//            long mealID = db.createMeal(name, imageUri);
//            Meal meal = new Meal(mealID, name, R.drawable.place_holder);

//            meal.setFoodItemIDs(foodItemAdapter.getSelectedItemIds());
//            db.updateMeal(meal);//creates a new meal
            return null;
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private void initiaLizeData(com.example.foodkeeper.Database db) {
        FOOD_ITEMS.addAll(db.getUserFoodItems(session.getUserEmail()));
    }
    private void setupAdapter() {
        foodItemAdapter = new FoodSelectionAdapter(this, FOOD_ITEMS);
        foodItemAdapter.setOnItemSelectionChangeListener(this);

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(foodItemAdapter);

        updateCounter();
    }
    private void updateCounter() {
        int selectedCount = foodItemAdapter != null ? foodItemAdapter.getSelectedItemCount() : 0;
        counterTextViewer.setText("Selected : " + selectedCount);
    }
    private void setupImagePicker() {
        requestNotificationPermission();
        resultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    try {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            Uri imageUri = result.getData().getData();
                            if (imageUri != null)
                            {
                                selectedImageUri =imageUri;
                                imageSelectedForCurrentMeal = true;
                                mealImage.setImageURI(imageUri);
                                showToast("Image loaded successfully");
                            } else {
                                showToast("Failed to load image");
                            }
                        } else {
                            showToast("No image selected");
                        }
                    } catch (Exception e) {
                        showToast("Error loading image: " + e.getMessage());
                    }
                }
        );
    }

    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        resultLauncher.launch(intent);
    }
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
    private void openFullView() {


        Intent intent = new Intent(CreateMealActivity.this, FullView.class);
        intent.putExtra("foodItems", foodItemAdapter.getSelectedItemIds());
        fullViewLauncher.launch(intent);
        overridePendingTransition(R.anim.fade_in, 0);
    }

    private ArrayList<FoodItem> FOOD_ITEMS = new ArrayList<>();
    private String saveImageToInternalStorage(Uri uri) throws IOException {
        InputStream inputStream = getContentResolver().openInputStream(uri);
        File file = new File(getFilesDir(), "meal_" + System.currentTimeMillis() + ".jpg");
        OutputStream outputStream = new FileOutputStream(file);

        byte[] buffer = new byte[4096];
        int length;
        while ((length = inputStream.read(buffer)) > 0) {
            outputStream.write(buffer, 0, length);
        }

        inputStream.close();
        outputStream.close();

        return file.getAbsolutePath();
    }
    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
    }


    @Override
    public void onSelectionChanged(int selectedCount) {
        updateCounterWithCount(selectedCount);
    }

    @Override
    public void onItemClick(FoodItem item, int position) {
        String message = item.getCheckState() ? "Selected: " : "Deselected: ";
        NotificationHelper.showLowStockNotification(this,item);
        Toast.makeText(this, message + item.getName(), Toast.LENGTH_SHORT).show();
    }
    private void updateCounterWithCount(int count) {
        counterTextViewer.setText("Selected : " + count);
    }
    private void markSelectedItems(ArrayList<String> selectedItemIds) {
        for (FoodItem item : FOOD_ITEMS) {
            String itemId = String.valueOf(item.getId());
            item.setChecked(selectedItemIds.contains(itemId));
        }
    }
}