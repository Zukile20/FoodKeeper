package com.example.foodkeeper.Fridge;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.foodkeeper.R;
import com.google.android.material.imageview.ShapeableImageView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class AddFridgeActivity extends AppCompatActivity {
    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int CAMERA_REQUEST = 2;
    private ShapeableImageView fridgeImage;
    private EditText brandEditText, modelEditText, descriptionEditText, numberEditText;
    private ImageButton btnIncrement, btnDecrement, cameraIconButton;
    private Button addFridgeButton;
    private Button backBtn;
    private Bitmap selectedImage;
    private int fridgeSize = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_fridge);

        fridgeImage = findViewById(R.id.fridgeImage);
        brandEditText = findViewById(R.id.brandEditText);
        modelEditText = findViewById(R.id.modelEditText);
        descriptionEditText = findViewById(R.id.descriptionEditText);
        numberEditText = findViewById(R.id.numberEditText);
        btnIncrement = findViewById(R.id.btnIncrement);
        btnDecrement = findViewById(R.id.btnDecrement);
        cameraIconButton = findViewById(R.id.cameraIconButton);
        addFridgeButton = findViewById(R.id.addFridgeButton);
        backBtn = findViewById(R.id.backBtn);
        numberEditText.setText(String.valueOf(fridgeSize));

        fridgeImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openImagePicker();
            }
        });

        cameraIconButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showImageSourceDialog();
            }
        });

        btnIncrement.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fridgeSize++;
                numberEditText.setText(String.valueOf(fridgeSize));
            }
        });

        btnDecrement.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (fridgeSize > 0) {
                    fridgeSize--;
                    numberEditText.setText(String.valueOf(fridgeSize));
                }
            }
        });

        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        addFridgeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addFridge();
            }
        });

        numberEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {
                    fridgeSize = Integer.parseInt(s.toString());
                } catch (NumberFormatException e) {
                    fridgeSize = 0;
                }
            }

            @Override
            public void afterTextChanged(Editable s){}
        });
    }

    private void showImageSourceDialog() {
        String[] options = {"Take Photo", "Choose from Gallery", "Cancel"};

        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Add Fridge Photo");
        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0:
                    openCamera();
                    break;
                case 1:
                    openGallery();
                    break;
                case 2:
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

    private void openImagePicker() {
        openGallery();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == PICK_IMAGE_REQUEST && data != null) {
                Uri imageUri = data.getData();
                try {
                    selectedImage = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                    fridgeImage.setImageBitmap(selectedImage);
                    Toast.makeText(this, "Fridge image selected", Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show();
                }
            } else if (requestCode == CAMERA_REQUEST && data != null) {
                Bundle extras = data.getExtras();
                if (extras != null) {
                    selectedImage = (Bitmap) extras.get("data");
                    fridgeImage.setImageBitmap(selectedImage);
                    Toast.makeText(this, "Fridge photo taken", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void addFridge() {
        String brand = brandEditText.getText().toString().trim();
        String model = modelEditText.getText().toString().trim();
        String description = descriptionEditText.getText().toString().trim();

        boolean valid = true;

        if (brand.isEmpty()) {
            brandEditText.setError("Brand is required");
            valid = false;
        }

        if (model.isEmpty()) {
            modelEditText.setError("Model is required");
            valid = false;
        }

        if (fridgeSize <= 0) {
            numberEditText.setError("Size must be greater than 0");
            valid = false;
        }

        if (!valid) {
            Toast.makeText(this, "Please correct the errors", Toast.LENGTH_SHORT).show();
            return;
        }

        byte[] imageData = null;
        if (selectedImage != null) {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            selectedImage.compress(Bitmap.CompressFormat.JPEG, 70, stream);
            imageData = stream.toByteArray();
        }

        Intent resultIntent = new Intent();
        resultIntent.putExtra("brand", brand);
        resultIntent.putExtra("model", model);
        resultIntent.putExtra("description", description);
        resultIntent.putExtra("size", fridgeSize);
        resultIntent.putExtra("image", imageData);

        setResult(RESULT_OK, resultIntent);
        finish();

        Toast.makeText(this, "Fridge added successfully", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        if (selectedImage != null && !selectedImage.isRecycled()) {
            selectedImage.recycle();
            selectedImage = null;
        }
    }
}