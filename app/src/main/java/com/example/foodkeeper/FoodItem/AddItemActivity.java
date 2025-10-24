package com.example.foodkeeper.FoodItem;

import android.Manifest;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.foodkeeper.FoodItem.models.Category;
import com.example.foodkeeper.FoodItem.models.FoodItem;
import com.example.foodkeeper.FoodItem.view_items.ItemsViewActivity;
import com.example.foodkeeper.FoodkeeperUtils.Database;

import com.example.foodkeeper.R;
import com.example.foodkeeper.Register.LoginActivity;
import com.example.foodkeeper.Register.RegisterActivity;
import com.example.foodkeeper.Register.SessionManager;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textfield.TextInputLayout;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class AddItemActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE = 100;
    private int quantity = 0;
    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int CAMERA_REQUEST = 2;
    EditText edName, edCategory, edExpiryDate;
    TextInputLayout txtDate;
    TextView tvQuantity;
    RadioGroup radioBtns;
    Database db;
    ShapeableImageView foodImage;
    ImageButton cameraIconButton;
    Bitmap selectedImage;
    Button btnMinus, btnPlus, btnSavePopup, btnCancelPopup, btnSave, backBtn;
    SessionManager session;
    private List<Category> categories;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_item);

        db = new Database(this);
        session = new SessionManager(this);
        categories = db.getAllCategories();
        edName = findViewById(R.id.nameEditText);
        edCategory = findViewById(R.id.categoryEditText);
        edExpiryDate = findViewById(R.id.expiryDateEditText);
        txtDate = findViewById(R.id.txtDateLayout);
        tvQuantity = findViewById(R.id.tv_quantity);
        btnMinus = findViewById(R.id.btn_minus);
        btnPlus = findViewById(R.id.btn_plus);
        btnSave = findViewById(R.id.saveButton);
        backBtn = findViewById(R.id.backBtn);
        foodImage = findViewById(R.id.foodImage);
        cameraIconButton = findViewById(R.id.cameraIconButton);

        foodImage.setOnClickListener(v -> {
            checkPermissionsAndOpenImageDialog();
        });

        cameraIconButton.setOnClickListener(v -> {
            checkPermissionsAndOpenImageDialog();
        });

        edCategory.setOnClickListener(v -> {
            showPopUp();
        });

        txtDate.setEndIconOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showCalendar();
            }
        });

        btnMinus.setOnClickListener(v -> {
            if(quantity > 0) {
                quantity--;
                updateQuantityViews();
            }
        });

        btnPlus.setOnClickListener(v -> {
            quantity++;
            updateQuantityViews();
        });

        btnSave.setOnClickListener(v -> {
            String name = edName.getText().toString();
            String category = edCategory.getText().toString();
            String expiryDate = edExpiryDate.getText().toString();
            int quantity = Integer.parseInt(tvQuantity.getText().toString());

            boolean isValid = true;

            if (name.isEmpty()) {
                edName.setError("Item name is required");
                isValid = false;
            }

            if (category.isEmpty()) {
                edCategory.setError("Category is required");
                isValid = false;
            }

            if (expiryDate.isEmpty()) {
                edExpiryDate.setError("Expiry date is required");
                isValid = false;
            }

            if (!isValid) {
                Toast.makeText(this, "Please correct errors", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                if (quantity <= 0) {
                    Toast.makeText(this, "Quantity must be greater than 0", Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid quantity", Toast.LENGTH_SHORT).show();
                return;
            }


            byte[] imageBytes = null;
            if (selectedImage != null) {
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                selectedImage.compress(Bitmap.CompressFormat.PNG, 100, stream);
                imageBytes = stream.toByteArray();
            }

            int categoryId = findCategoryIdByName(category);

            db.addFoodItem(new FoodItem(name, String.valueOf(categoryId), expiryDate, quantity, imageBytes),session.getUserEmail());
            startActivity(new Intent(AddItemActivity.this, ItemsViewActivity.class));
        });

        backBtn.setOnClickListener(v -> {
            startActivity(new Intent(this, ItemsViewActivity.class));
        });
    }

    private int findCategoryIdByName(String categoryName) {
        if (categories == null || categoryName == null) {
            return 15;
        }

        for (Category category : categories) {
            if (category.getName().equals(categoryName)) {
                return category.getId();
            }
        }

        for (Category category : categories) {
            if ("Other".equals(category.getName())) {
                return category.getId();
            }
        }

        return 15;
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

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Photo (Optional)");
        builder.setItems(options, (dialog, item) -> {
            switch (item) {
                case 0:
                    Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    if (cameraIntent.resolveActivity(getPackageManager()) != null) {
                        startActivityForResult(cameraIntent, CAMERA_REQUEST);
                    } else {
                        Toast.makeText(this, "No camera app found", Toast.LENGTH_SHORT).show();
                    }
                    break;
                case 1:
                    Intent galleryIntent = new Intent(Intent.ACTION_PICK,
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(galleryIntent, PICK_IMAGE_REQUEST);
                    break;
                case 2:
                    selectedImage = null;
                    foodImage.setImageResource(R.drawable.image_placeholder);
                    Toast.makeText(this, "Photo removed", Toast.LENGTH_SHORT).show();
                    break;
                case 3:
                    dialog.dismiss();
                    break;
            }
        });
        builder.show();
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

    private void showPopUp() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View popupView = getLayoutInflater().inflate(R.layout.custom_popup, null);

        builder.setView(popupView);

        btnSavePopup = popupView.findViewById(R.id.savePopup);
        btnCancelPopup = popupView.findViewById(R.id.cancelPopup);
        radioBtns = popupView.findViewById(R.id.radioGroup);

        radioBtns.removeAllViews();

        if (categories == null || categories.isEmpty()) {
            Toast.makeText(this, "No categories available", Toast.LENGTH_SHORT).show();
            return;
        }

        for (Category category : categories) {
            RadioButton radioButton = new RadioButton(this);
            radioButton.setText(category.getName());
            radioButton.setTextSize(15);
            radioButton.setTextColor(getResources().getColor(android.R.color.black));
            radioButton.setPadding(0, 16, 0, 16);

            RadioGroup.LayoutParams params = new RadioGroup.LayoutParams(
                    RadioGroup.LayoutParams.MATCH_PARENT,
                    RadioGroup.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 0, 0, 16);

            radioBtns.addView(radioButton, params);
        }

        AlertDialog dialog = builder.create();

        btnCancelPopup.setOnClickListener(v -> dialog.dismiss());

        btnSavePopup.setOnClickListener(v -> {
            int selectedId = radioBtns.getCheckedRadioButtonId();
            if(selectedId != -1){
                RadioButton selectedBtn = popupView.findViewById(selectedId);
                String selectedCategory = selectedBtn.getText().toString();
                edCategory.setText(selectedCategory);
                dialog.dismiss();
            } else {
                Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show();
            }
        });
        dialog.show();
    }

    private void showCalendar() {
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    String date = selectedDay + "/" + (selectedMonth + 1) + "/" + selectedYear;
                    edExpiryDate.setText(date);
                },
                year, month, day
        );
        datePickerDialog.show();
    }

    private void updateQuantityViews() {
        tvQuantity.setText(String.valueOf(quantity));
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = Math.max(0, Math.min(quantity, 999999));
        updateQuantityViews();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == PICK_IMAGE_REQUEST && data != null) {
                Uri selectedImageUri = data.getData();
                try {
                    selectedImage = MediaStore.Images.Media.getBitmap(
                            this.getContentResolver(), selectedImageUri);
                    foodImage.setImageBitmap(selectedImage);
                    Toast.makeText(this, "Image selected", Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show();
                }
            } else if (requestCode == CAMERA_REQUEST && data != null) {
                selectedImage = (Bitmap) data.getExtras().get("data");
                foodImage.setImageBitmap(selectedImage);
                Toast.makeText(this, "Photo taken", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (selectedImage != null && !selectedImage.isRecycled()) {
            selectedImage.recycle();
            selectedImage = null;
        }
    }
}