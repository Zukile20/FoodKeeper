package com.example.foodkeeper.Fridge;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.foodkeeper.Database;
import com.example.foodkeeper.R;
import com.example.foodkeeper.SessionManager;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textfield.TextInputEditText;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class EditFridgeActivity extends AppCompatActivity {
    private Database db;
    private Fridge currentFridge;
    private int fridgeSize = 0;
    private ShapeableImageView fridgeEditImage;
    private TextInputEditText brandEditFridgeText, modelEditFridgeText, descriptionEditFridgeText;
    private EditText numberEditFridgeText;
    private ImageButton btnIncrementFridge, btnDecrementFridge;
    private Button cancelButtonFridge, saveButtonFridge, backEditFridgeBtn;
    private Bitmap selectedImage;
    private static final int PICK_IMAGE_REQUEST = 1;
    private int fridgeId;
    SessionManager session;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_fridge);

        db = new Database(this);
        session = new SessionManager(this);

        fridgeEditImage = findViewById(R.id.fridgeEditImage);
        brandEditFridgeText = findViewById(R.id.brandEditFridgeText);
        modelEditFridgeText = findViewById(R.id.modelEditFridgeText);
        descriptionEditFridgeText = findViewById(R.id.descriptionEditFridgeText);
        numberEditFridgeText = findViewById(R.id.numberEditFridgeText);
        btnIncrementFridge = findViewById(R.id.btnIncrementFridge);
        btnDecrementFridge = findViewById(R.id.btnDecrementFridge);
        cancelButtonFridge = findViewById(R.id.cancelButtonFridge);
        saveButtonFridge = findViewById(R.id.saveButtonFridge);
        backEditFridgeBtn = findViewById(R.id.backEditFridgeBtn);

        backEditFridgeBtn.setOnClickListener(v -> onBackPressed());

        fridgeEditImage.setOnClickListener(v -> openImagePicker());

        btnIncrementFridge.setOnClickListener(v -> {
            fridgeSize++;
            numberEditFridgeText.setText(String.valueOf(fridgeSize));
        });

        btnDecrementFridge.setOnClickListener(v -> {
            if (fridgeSize > 0) {
                fridgeSize--;
                numberEditFridgeText.setText(String.valueOf(fridgeSize));
            }
        });

        cancelButtonFridge.setOnClickListener(v -> onBackPressed());

        saveButtonFridge.setOnClickListener(v -> saveFridgeDetails());

        fridgeId = getIntent().getIntExtra("FRIDGE_ID", -1);
        if (fridgeId == -1) {
            Toast.makeText(this, "Fridge not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadFridgeDetails(fridgeId);
        numberEditFridgeText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                try {
                    fridgeSize = Integer.parseInt(s.toString());
                } catch (NumberFormatException e) {
                    fridgeSize = 0;
                }
            }
        });
    }

    private void loadFridgeDetails(int fridgeId) {
        currentFridge = db.getFridgeById(fridgeId, session.getUserEmail());

        if (currentFridge == null) {
            Toast.makeText(this, "Failed to load fridge details", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        brandEditFridgeText.setText(currentFridge.getBrand());
        modelEditFridgeText.setText(currentFridge.getModel());
        descriptionEditFridgeText.setText(currentFridge.getDescription());
        fridgeSize = currentFridge.getSize();
        numberEditFridgeText.setText(String.valueOf(fridgeSize));

        byte[] imageData = currentFridge.getImage();
        if (imageData != null && imageData.length > 0) {
            try {
                Bitmap bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.length);
                fridgeEditImage.setImageBitmap(bitmap);
            } catch (Exception e) {
                fridgeEditImage.setImageResource(R.drawable.image_placeholder);
            }
        } else {
            fridgeEditImage.setImageResource(R.drawable.image_placeholder);
        }
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            try {
                selectedImage = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                fridgeEditImage.setImageBitmap(selectedImage);
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void saveFridgeDetails() {
        String brand = brandEditFridgeText.getText().toString().trim();
        String model = modelEditFridgeText.getText().toString().trim();
        String description = descriptionEditFridgeText.getText().toString().trim();
        String sizeStr = numberEditFridgeText.getText().toString().trim();

        if (brand.isEmpty() || model.isEmpty() || Integer.parseInt(sizeStr) <= 0) {
            Toast.makeText(this, "Please fill in all details", Toast.LENGTH_SHORT).show();
            return;
        }

        currentFridge.setBrand(brand);
        currentFridge.setModel(model);
        currentFridge.setDescription(description);
        currentFridge.setSize(fridgeSize);

        byte[] imageData = null;
        if (selectedImage != null) {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            selectedImage.compress(Bitmap.CompressFormat.JPEG, 70, stream);
            imageData = stream.toByteArray();
            currentFridge.setImage(imageData);
        }

        boolean updated = db.updateFridge(currentFridge, session.getUserEmail());

        if (updated) {
            Toast.makeText(this, "Fridge updated successfully", Toast.LENGTH_SHORT).show();
            Intent resultIntent = new Intent();
            resultIntent.putExtra("UPDATED_FRIDGE_ID", fridgeId);
            setResult(RESULT_OK, resultIntent);
            finish();
        } else {
            Toast.makeText(this, "Failed to update fridge", Toast.LENGTH_SHORT).show();
        }

    }

    @SuppressLint("GestureBackNavigation")
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(0, 0);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (db != null) {
            db.close();
        }
        if (selectedImage != null && !selectedImage.isRecycled()) {
            selectedImage.recycle();
        }
    }
}