package com.example.foodkeeper.Meal;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
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
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class UpdateMealActivity extends AppCompatActivity implements FoodSelectionAdapter.OnItemSelectionChangeListener {

    Meal EditMeal;
    private TextInputEditText nameField;
    private TextInputLayout tilMealName;
    private TextView tvMealNameError;
    ArrayList<FoodItem> FOOD_ITEMS = new ArrayList<>();
    private RecyclerView recyclerView;
    private ImageView mealImage;
    private EditText searchText;
    private TextView counterTextViewer;
    private Button saveBtn, backBtn, fullViewBtn;
    private ImageButton loadImage;
    private Database db;
    private SessionManager session;
    FoodSelectionAdapter adapter;
    private boolean imageSelectedForCurrentMeal = false;
    private Uri selectedImageUri;
    ActivityResultLauncher<Intent> fullViewLauncher;
    private ActivityResultLauncher<Intent> resultLauncher;

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

    private void iniWidgets() {
        db = Database.getInstance(this);

        mealImage = findViewById(R.id.mealImage);
        loadImage = findViewById(R.id.loadPicture);
        nameField = findViewById(R.id.nameField);
        tilMealName = findViewById(R.id.tilMealName);
        tvMealNameError = findViewById(R.id.tvMealNameError);
        backBtn = findViewById(R.id.backBtn);
        searchText = findViewById(R.id.searchField);
        counterTextViewer = findViewById(R.id.counterTextView);
        fullViewBtn = findViewById(R.id.fullViewBtn);
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
        });
    }

    private void setupAdapter() {
        adapter = new FoodSelectionAdapter(this, FOOD_ITEMS);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    private void LoadFoodItems() {
        Intent intent = getIntent();
        if (intent != null) {
            if (intent.hasExtra("EditMeal")) {
                long EditMealID = intent.getLongExtra("EditMeal", 0);
                EditMeal = db.getMealWithFoodItems(EditMealID);
            }
        }
    }

    private void loadData() {
        session = new SessionManager(this);
        FOOD_ITEMS.addAll(db.getFoodItemsInConnectedFridge(session.getUserEmail()));
    }

    private void initializeFields() {
        LoadFoodItems();
        nameField.setText(EditMeal.getMealName());

        if (mealImage != null && EditMeal.getUri() != null && !EditMeal.getUri().isEmpty()) {
            loadBase64ImageWithGlide(this, EditMeal.getUri(), mealImage);
        }

        adapter.setSelectedItems(EditMeal.getFoodItemIDs());
        adapter.notifyDataSetChanged();
        updateCounter();
    }

    @SuppressLint("SetTextI18n")
    private void setupListeners() {
        backBtn.setOnClickListener(view -> finish());
        loadImage.setOnClickListener(v -> pickImage());

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
                adapter.getFilter().filter(s.toString());
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });

        fullViewBtn.setOnClickListener(v -> openFullView());
        saveBtn.setOnClickListener(v ->
        {
            String mealName = String.valueOf(nameField.getText()).trim();

            boolean hasError = false;

            if (mealName.isEmpty()) {
                showError(tvMealNameError, "Meal name cannot be empty");
                nameField.requestFocus();
                hasError = true;
            }

            if (adapter.getSelectedItemCount() == 0) {
                Toast.makeText(this, "At least one item must be selected", Toast.LENGTH_SHORT).show();
                hasError = true;
            }

            if (!hasError) {
                if (db != null) {
                    try {
                        String imageBase64 = EditMeal.getUri();

                        if (imageSelectedForCurrentMeal && selectedImageUri != null) {
                            imageBase64 = saveImageToStorage(this, selectedImageUri);
                        }

                        imageSelectedForCurrentMeal = false;
                        selectedImageUri = null;

                        EditMeal.setMealName(mealName);
                        EditMeal.setFoodItemIDs(adapter.getSelectedItemIds());
                        EditMeal.setUrl(imageBase64);
                        db.updateMeal(EditMeal);
                        Toast.makeText(this, "Meal changes have been saved successfully", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    } catch (IOException e) {
                        Toast.makeText(this, "Error saving image", Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    }
                }
            }
        });
        setupImagePicker();
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

    private void openFullView() {
        Intent intent = new Intent(this, FullView.class);
        intent.putExtra("foodItems", adapter.getSelectedItemIds());
        fullViewLauncher.launch(intent);
        overridePendingTransition(R.anim.fade_in, 0);
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void setupImagePicker() {
        resultLauncher = registerForActivityResult(
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
    }

    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        resultLauncher.launch(intent);
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

    public void loadBase64ImageWithGlide(Context context, String base64String, ImageView imageView) {
        if (base64String == null || base64String.isEmpty()) {
            imageView.setImageResource(R.drawable.image_placeholder);
            return;
        }

        try {
            byte[] decodedBytes = Base64.decode(base64String, Base64.DEFAULT);

            Glide.with(context)
                    .asBitmap()
                    .load(decodedBytes)
                    .placeholder(R.drawable.image_placeholder)
                    .error(R.drawable.image_placeholder)
                    .centerCrop()
                    .into(imageView);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            imageView.setImageResource(R.drawable.image_placeholder);
        } catch (Exception e) {
            e.printStackTrace();
            imageView.setImageResource(R.drawable.image_placeholder);
        }
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
    public void onSelectionChanged(int selectedCount) {
        updateCounter();
    }

    @Override
    public void onItemClick(FoodItem item, int position) {
        String message = item.getCheckState() ? "Selected: " : "Deselected: ";
        Toast.makeText(this, message + item.getName(), Toast.LENGTH_SHORT).show();
    }
}