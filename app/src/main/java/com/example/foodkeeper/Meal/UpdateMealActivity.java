package com.example.foodkeeper.Meal;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.foodkeeper.FoodkeeperUtils.Database;
import com.example.foodkeeper.FoodItem.models.FoodItem;
import com.example.foodkeeper.R;
import com.example.foodkeeper.Register.SessionManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

public class UpdateMealActivity extends AppCompatActivity implements FoodSelectionAdapter.OnItemSelectionChangeListener{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_update_meal);
        iniWidgets();
        loadData();
        setupAdapter();
        setupListeners();
        initializeFields();
    }
    Meal EditMeal;
    private EditText nameField;
    ArrayList<FoodItem> FOOD_ITEMS = new ArrayList<>();
    private RecyclerView recyclerView;
    private ImageView mealImage;
    private EditText searchText;
    private TextView counterTextViewer;
    private Button saveBtn,backBtn,fullViewBtn;
    private ImageButton loadImage;
    private Database db ;
    private SessionManager session;
    FoodSelectionAdapter adapter ;
    private boolean imageSelectedForCurrentMeal = false;
    private Uri selectedImageUri ;
    ActivityResultLauncher<Intent> fullViewLauncher;
    private void iniWidgets()
    {
        db = Database.getInstance(this);

        mealImage= findViewById(R.id.mealImage);
        loadImage = findViewById(R.id.loadPicture);
        nameField =findViewById(R.id.nameField);
        backBtn= findViewById(R.id.backBtn);
        searchText = findViewById(R.id.searchField);
        counterTextViewer = findViewById(R.id.counterTextView);
        fullViewBtn= findViewById(R.id.fullViewBtn);
        recyclerView = findViewById(R.id.recyclerView);
        saveBtn = findViewById(R.id.saveBtn);
        fullViewLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result ->
                {
                    if (result.getResultCode() == RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null) {
                            ArrayList<String> selectedItems = data.getStringArrayListExtra("selectedItems");
                            markSelectedItems(selectedItems);
                            adapter.notifyDataSetChanged();
                            updateCounter();
                        }
                    }
                }
        );
    }
    private void setupAdapter()
    {
        adapter= new FoodSelectionAdapter(this,FOOD_ITEMS);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

    }
    private void LoadFoodItems()
    {
        Intent intent = getIntent();
        if(intent!=null) {
            if(intent.hasExtra("EditMeal"))
            {
               long EditMealID = intent.getLongExtra("EditMeal",0);
               EditMeal = db.getMealWithFoodItems(EditMealID);
               //loadMeal(EditMeal);
            }
        }
    }
    private void loadMeal(Meal meal)
    {
        ArrayList<FoodItem> mealFoodItems;
        mealFoodItems= (ArrayList<FoodItem>) db.getFoodItemsForMeal(meal.getMealID());
        for(int i=0;i<FOOD_ITEMS.size();i++)
        {
            FoodItem temp = FOOD_ITEMS.get(i);
            if(mealFoodItems.contains(temp))
            {
                temp.setChecked(true);
            }
        }
    }
    private void loadData()
    {
        session= new SessionManager(this);

        FOOD_ITEMS.addAll(db.getFoodItemsInConnectedFridge(session.getUserEmail()));
    }
    private void initializeFields()
    {
        LoadFoodItems();//load items
        nameField.setText(EditMeal.getMealName());//load the meal name
        //load the meal image if it has one
        if(mealImage!=null) {
            if (EditMeal.getUri() != null) {//since a meal might not have an image

                File imageFile  = new File(EditMeal.getUri());
                Glide.with(this)
                        .load(imageFile)
                        .centerCrop()
                        .placeholder(R.drawable.image_placeholder)
                        .error(R.drawable.ic_no_meals)
                        .into(mealImage);

            }
        }
        adapter.setSelectedItems(EditMeal.getFoodItemIDs());
        adapter.notifyDataSetChanged();
        updateCounter();

    }
    @SuppressLint("SetTextI18n")
    private void setupListeners() {
        backBtn.setOnClickListener(view -> finish());
        loadImage.setOnClickListener(v -> pickImage());
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
            }
        });

        fullViewBtn.setOnClickListener(v ->openFullView());
        saveBtn.setOnClickListener(v->
        {
            String mealName = String.valueOf(nameField.getText());
            if(adapter.getSelectedItemCount()==0 || mealName.length()==0)
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


                    String imageUri = null;

                    if (imageSelectedForCurrentMeal && selectedImageUri != null) {
                        try {
                            imageUri = saveImageToInternalStorage(selectedImageUri);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }

                    // Reset for next meal
                    imageSelectedForCurrentMeal = false;
                    selectedImageUri = null;

                    EditMeal.setMealName(String.valueOf(nameField.getText()));
                    EditMeal.setFoodItemIDs(adapter.getSelectedItemIds());
                    EditMeal.setUrl(imageUri);
                    db.updateMeal(EditMeal);
                    Toast.makeText(this,"Meal changes has been saved successfully",Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                }
            }

        });
        setupImagePicker();
    }
    private void openFullView() {
        Intent intent = new Intent(this, FullView.class);
        intent.putExtra("foodItems", adapter.getSelectedItemIds());
        fullViewLauncher.launch(intent);
        overridePendingTransition(R.anim.fade_in, 0);
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private ActivityResultLauncher<Intent> resultLauncher;
    private void setupImagePicker() {
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

                                String path = imageUri.toString();
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

        return file.getAbsolutePath(); // Save this path in your Meal object
    }
    private void updateCounter() {
        int selectedCount = adapter != null ? adapter.getSelectedItemCount() : 0;
        counterTextViewer.setText("Selected : " + selectedCount);
    }
    private void markSelectedItems(ArrayList<String> selectedItemIds) {
        for (FoodItem item : FOOD_ITEMS) {
            String itemId = String.valueOf(item.getId());
            item.setChecked(selectedItemIds.contains(itemId));
        }
    }

    @Override
    public void onSelectionChanged(int selectedCount)
    {
        updateCounter();
    }

    @Override
    public void onItemClick(FoodItem item, int position){
        String message = item.getCheckState() ? "Selected: " : "Deselected: ";
        Toast.makeText(this, message + item.getName(), Toast.LENGTH_SHORT).show();
    }
}