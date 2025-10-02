package com.example.foodkeeper.Fridge;

import android.Manifest;
import android.annotation.SuppressLint;
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
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.foodkeeper.Database;
import com.example.foodkeeper.R;
import com.example.foodkeeper.SessionManager;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textfield.TextInputEditText;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class EditFridgeActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int CAMERA_REQUEST = 2;

    private Database db;
    private Fridge currentFridge;
    private int fridgeSize = 0;
    private ShapeableImageView fridgeEditImage;
    private ImageButton cameraIconButton;
    private TextInputEditText brandEditFridgeText, modelEditFridgeText, descriptionEditFridgeText;
    private EditText numberEditFridgeText;
    private ImageButton btnIncrementFridge, btnDecrementFridge;
    private Button cancelButtonFridge, saveButtonFridge, backEditFridgeBtn;
    private Bitmap selectedImage;
    private int fridgeId;
    SessionManager session;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_fridge);

        db = new Database(this);
        session = new SessionManager(this);

        fridgeEditImage = findViewById(R.id.fridgeEditImage);
        cameraIconButton = findViewById(R.id.cameraIconButton);
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

        fridgeEditImage.setOnClickListener(v -> {
            checkPermissionsAndOpenImageDialog();
        });

        cameraIconButton.setOnClickListener(v -> {
            checkPermissionsAndOpenImageDialog();
        });

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

    private void checkPermissionsAndOpenImageDialog() {
        List<String> permissionsNeeded = new ArrayList<>();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.CAMERA);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.READ_MEDIA_IMAGES);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }

        if (!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    permissionsNeeded.toArray(new String[0]),
                    PERMISSION_REQUEST_CODE);
        } else {
            showImageDialog();
        }
    }

    private void showImageDialog() {
        final CharSequence[] options = {"Take Photo", "Choose from Gallery", "Remove Photo", "Cancel"};

        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Change Fridge Photo");
        builder.setItems(options, (dialog, item) -> {
            switch (item) {
                case 0:
                    openCamera();
                    break;
                case 1:
                    openGallery();
                    break;
                case 2:
                    selectedImage = null;
                    fridgeEditImage.setImageResource(R.drawable.image_placeholder);
                    Toast.makeText(this, "Photo removed", Toast.LENGTH_SHORT).show();
                    break;
                case 3:
                    dialog.dismiss();
                    break;
            }
        });
        builder.show();
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, CAMERA_REQUEST);
        } else {
            Toast.makeText(this, "No camera app found", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0) {
                boolean allGranted = true;
                for (int result : grantResults) {
                    if (result != PackageManager.PERMISSION_GRANTED) {
                        allGranted = false;
                        break;
                    }
                }

                if (allGranted) {
                    showImageDialog();
                } else {
                    Toast.makeText(this, "Permissions needed to select images", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == PICK_IMAGE_REQUEST && data != null) {
                Uri imageUri = data.getData();
                try {
                    selectedImage = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                    fridgeEditImage.setImageBitmap(selectedImage);
                    Toast.makeText(this, "Image selected", Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show();
                }
            } else if (requestCode == CAMERA_REQUEST && data != null) {
                Bundle extras = data.getExtras();
                if (extras != null) {
                    selectedImage = (Bitmap) extras.get("data");
                    fridgeEditImage.setImageBitmap(selectedImage);
                    Toast.makeText(this, "Photo taken", Toast.LENGTH_SHORT).show();
                }
            }
        }
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
                selectedImage = bitmap;
            } catch (Exception e) {
                fridgeEditImage.setImageResource(R.drawable.image_placeholder);
            }
        } else {
            fridgeEditImage.setImageResource(R.drawable.image_placeholder);
        }
    }

    private void saveFridgeDetails() {
        String brand = brandEditFridgeText.getText().toString().trim();
        String model = modelEditFridgeText.getText().toString().trim();
        String description = descriptionEditFridgeText.getText().toString().trim();
        String sizeStr = numberEditFridgeText.getText().toString().trim();

        if (brand.isEmpty() || model.isEmpty()) {
            Toast.makeText(this, "Please fill in all details", Toast.LENGTH_SHORT).show();
            return;
        }

        if(Integer.parseInt(sizeStr) <= 0){
            Toast.makeText(this, "Size can't be 0", Toast.LENGTH_SHORT).show();
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
        } else {
            currentFridge.setImage(null);
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
            selectedImage = null;
        }
    }
}