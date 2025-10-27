package com.example.foodkeeper.Meal;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.foodkeeper.FoodItem.models.FoodItem;
import com.example.foodkeeper.FoodkeeperUtils.Database;
import com.example.foodkeeper.R;
import com.example.foodkeeper.Register.SessionManager;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class CreateMealActivity extends AppCompatActivity implements FoodSelectionAdapter.OnItemSelectionChangeListener {
    Database db;

    private boolean imageSelectedForCurrentMeal = false;
    private TextInputEditText nameField;
    private TextInputLayout tilMealName;
    private TextView tvMealNameError;
    private EditText searchText;
    private RecyclerView recyclerView;
    private ImageButton loadImageBtn;
    private ImageView mealImage;
    private Button backBtn, createBtn;
    private Uri selectedImageUri;
    private Uri cameraImageUri;
    private Button fullViewBtn;
    private TextView counterTextViewer;

    private FoodSelectionAdapter foodItemAdapter;

    private ActivityResultLauncher<Intent> galleryLauncher;
    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<Intent> fullViewLauncher;
    private SessionManager session;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.create_meal_activity);
        db = Database.getInstance(this);
        session = new SessionManager(this);
        initializeViews();
        initiaLizeData(db);
        setupAdapter();
        fullViewLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result ->
        {
            if (result.getResultCode() == RESULT_OK) {
                Intent data = result.getData();
                if (data != null) {
                    ArrayList<String> selectedItems = data.getStringArrayListExtra("selectedItems");
                    markSelectedItems(selectedItems);
                    foodItemAdapter.setSelectedItems(selectedItems);
                    foodItemAdapter.notifyDataSetChanged();
                    updateCounter();
                }
            }
        });
        setupListeners();
        setupImagePickers();
    }

    private void initializeViews() {
        recyclerView = findViewById(R.id.recyclerView);
        loadImageBtn = findViewById(R.id.loadPicture);
        mealImage = findViewById(R.id.mealImage);
        nameField = findViewById(R.id.nameField);
        tilMealName = findViewById(R.id.tilMealName);
        tvMealNameError = findViewById(R.id.tvMealNameError);
        backBtn = findViewById(R.id.backBtn);
        searchText = findViewById(R.id.searchField);
        fullViewBtn = findViewById(R.id.fullViewBtn);
        createBtn = findViewById(R.id.createBtn);
        counterTextViewer = findViewById(R.id.counterTextView);
    }

    private void setupListeners() {
        backBtn.setOnClickListener(view -> finish());
        loadImageBtn.setOnClickListener(v -> showImagePickerDialog());

        nameField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String mealName = s.toString().trim();
                if (mealName.isEmpty()) {
                    showError(tvMealNameError, "Meal name cannot be empty");
                } else {
                    hideError(tvMealNameError);
                }
            }
        });

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

        fullViewBtn.setOnClickListener(v -> openFullView());
        createBtn.setOnClickListener(v ->
        {
            String mealName = String.valueOf(nameField.getText()).trim();

            // Validation
            boolean hasError = false;

            if (mealName.isEmpty()) {
                showError(tvMealNameError, "Meal name cannot be empty");
                nameField.requestFocus();
                hasError = true;
            }

            if (foodItemAdapter.getSelectedItemCount() == 0) {
                Toast.makeText(this, "At least one item must be selected", Toast.LENGTH_SHORT).show();
                hasError = true;
            }

            if (!hasError) {
                if (db != null) {
                    //creating the meal......with its fooditems
                    createNewMeal(mealName);
                    Toast.makeText(this, "Meal has been created successfully", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                }
            }
        });
    }

    private void showImagePickerDialog() {
        final CharSequence[] options = {"Take Photo", "Choose from Gallery", "Remove Photo", "Cancel"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Image");
        builder.setItems(options, (dialog, item) -> {
            if (options[item].equals("Take Photo")) {
                if (checkCameraPermission()) {
                    openCamera();
                } else {
                    requestCameraPermission();
                }
            } else if (options[item].equals("Choose from Gallery")) {
                pickImageFromGallery();
            } else if (options[item].equals("Remove Photo")) {
                removePhoto();
            } else if (options[item].equals("Cancel")) {
                dialog.dismiss();
            }
        });
        builder.show();
    }

    private boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 102);
    }

    private void openCamera() {
        try {
            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

            File photoFile = createImageFile();
            if (photoFile != null) {
                cameraImageUri = FileProvider.getUriForFile(this,
                        getApplicationContext().getPackageName() + ".provider",
                        photoFile);
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);
                cameraLauncher.launch(cameraIntent);
            }
        } catch (IOException e) {
            showToast("Error creating image file");
        }
    }

    private File createImageFile() throws IOException {
        String imageFileName = "MEAL_" + System.currentTimeMillis();
        File storageDir = getExternalFilesDir(null);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    private void pickImageFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(intent);
    }

    private void removePhoto() {
        selectedImageUri = null;
        cameraImageUri = null;
        imageSelectedForCurrentMeal = false;

        mealImage.setImageResource(R.drawable.image_placeholder);
        showToast("Photo removed");
    }

    private void showError(TextView errorTextView, String message) {
        errorTextView.setText(message);
        errorTextView.setVisibility(View.VISIBLE);

        if (errorTextView == tvMealNameError) {
            tilMealName.setBoxStrokeErrorColor(getResources().getColorStateList(R.color.red));
            tilMealName.setError(" ");
        }
    }

    private void hideError(TextView errorTextView) {
        errorTextView.setText("");
        errorTextView.setVisibility(View.GONE);

        if (errorTextView == tvMealNameError) {
            tilMealName.setError(null);
        }
    }

    private String createNewMeal(String name) {
        try {
            String imageBase64 = null;

            if (imageSelectedForCurrentMeal && selectedImageUri != null) {
                imageBase64 = saveImageToStorage(this, selectedImageUri);
            }

            imageSelectedForCurrentMeal = false;
            selectedImageUri = null;

            Meal meal = new Meal(name, imageBase64, db.getConnectedFridgeForUser(session.getUserEmail()).getId());
            long mealID = db.createMeal(meal, db.getConnectedFridgeForUser(session.getUserEmail()).getId());
            meal.setMealID(mealID);
            meal.setFoodItemIDs(foodItemAdapter.getSelectedItemIds());
            db.updateMeal(meal);
            return null;
        } catch (IOException e) {
            Toast.makeText(this, "Error processing image", Toast.LENGTH_SHORT).show();
            throw new RuntimeException(e);
        }
    }

    public String saveImageToStorage(Context context, Uri uri) throws IOException {
        InputStream inputStream = context.getContentResolver().openInputStream(uri);

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(inputStream, null, options);
        inputStream.close();

        inputStream = context.getContentResolver().openInputStream(uri);
        options.inSampleSize = calculateInSampleSize(options, 1024, 1024);
        options.inJustDecodeBounds = false;

        Bitmap bitmap = BitmapFactory.decodeStream(inputStream, null, options);
        inputStream.close();

        if (bitmap == null) {
            throw new IOException("Failed to decode bitmap");
        }

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        byteArrayOutputStream.close();
        bitmap.recycle();

        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    private void initiaLizeData(Database db) {
        FOOD_ITEMS.addAll(db.getUserFoodItems(session.getUserEmail()));
    }

    private void setupAdapter() {
        foodItemAdapter = new FoodSelectionAdapter(this, FOOD_ITEMS);
        foodItemAdapter.setOnItemSelectionChangeListener(this);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(foodItemAdapter);

        updateCounter();
    }

    private void updateCounter() {
        int selectedCount = foodItemAdapter != null ? foodItemAdapter.getSelectedItemCount() : 0;
        counterTextViewer.setText("Selected : " + selectedCount);
    }

    private void setupImagePickers() {
        requestNotificationPermission();

        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    try {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            Uri imageUri = result.getData().getData();
                            if (imageUri != null) {
                                selectedImageUri = imageUri;
                                imageSelectedForCurrentMeal = true;

                                Glide.with(this)
                                        .load(imageUri)
                                        .placeholder(R.drawable.image_placeholder)
                                        .error(R.drawable.image_placeholder)
                                        .centerCrop()
                                        .into(mealImage);

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

        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    try {
                        if (result.getResultCode() == RESULT_OK) {
                            if (cameraImageUri != null) {
                                selectedImageUri = cameraImageUri;
                                imageSelectedForCurrentMeal = true;

                                Glide.with(this)
                                        .load(cameraImageUri)
                                        .placeholder(R.drawable.image_placeholder)
                                        .error(R.drawable.image_placeholder)
                                        .centerCrop()
                                        .into(mealImage);

                                showToast("Photo captured successfully");
                            } else {
                                showToast("Failed to capture photo");
                            }
                        } else {
                            showToast("Photo capture cancelled");
                        }
                    } catch (Exception e) {
                        showToast("Error capturing photo: " + e.getMessage());
                    }
                }
        );
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
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 102) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                showToast("Camera permission is required to take photos");
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

    @Override
    protected void onResume() {
        super.onResume();
        refreshFoodItems();
    }

    private void refreshFoodItems() {
        if (db != null && session != null) {
            try {
                ArrayList<String> selectedIds = new ArrayList<>();
                if (foodItemAdapter != null) {
                    selectedIds = foodItemAdapter.getSelectedItemIds();
                }

                List<FoodItem> freshItems = db.getUserFoodItems(session.getUserEmail());

                for (FoodItem item : freshItems) {
                    if (selectedIds.contains(String.valueOf(item.getId()))) {
                        item.setChecked(true);
                    }
                }

                if (foodItemAdapter != null) {
                    foodItemAdapter.updateItems(freshItems);
                }

                updateCounter();

            } catch (Exception e) {
                Toast.makeText(this, "Error loading items", Toast.LENGTH_SHORT).show();
            }
        }
    }
}