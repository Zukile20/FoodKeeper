package com.example.foodkeeper.FoodItem;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
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
import com.google.android.material.textfield.TextInputLayout;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EditItemActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int CAMERA_REQUEST = 2;

    private FoodItem currentItem;
    private Database db;
    private EditText edName, edCategory, edExpiryDate;
    private TextInputLayout txtDateLayout;
    private TextView tvQuantity;
    private ShapeableImageView foodImage;
    private ImageButton cameraIconButton;
    private Button btnMinus, btnPlus, btnCancel, btnSave, btnClose;
    private Bitmap selectedImage;
    private Calendar calendar;
    private DatePickerDialog datePickerDialog;
    private Dialog categoryDialog;
    private RadioGroup categoryRadioGroup;
    private String selectedCategory = "";
    SessionManager session;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_item);

        db = new Database(this);
        session = new SessionManager(this);
        currentItem = (FoodItem) getIntent().getSerializableExtra("foodItem");

        edName = findViewById(R.id.nameEditText);
        edCategory = findViewById(R.id.categoryEditText);
        edExpiryDate = findViewById(R.id.expiryDateEditText);
        txtDateLayout = findViewById(R.id.txtDateLayout);
        tvQuantity = findViewById(R.id.tv_quantity);
        btnMinus = findViewById(R.id.btn_minus);
        btnPlus = findViewById(R.id.btn_plus);
        foodImage = findViewById(R.id.editItemImage);
        cameraIconButton = findViewById(R.id.cameraIconButton);
        btnCancel = findViewById(R.id.cancelButton);
        btnSave = findViewById(R.id.saveButton);
        btnClose = findViewById(R.id.backEditBtn);

        calendar = Calendar.getInstance();

        edCategory.setFocusable(false);
        edCategory.setOnClickListener(v -> showCategoryPopup());

        edExpiryDate.setFocusable(false);
        edExpiryDate.setOnClickListener(v -> showDatePicker());

        foodImage.setOnClickListener(v -> {
            checkPermissionsAndOpenImageDialog();
        });

        cameraIconButton.setOnClickListener(v -> {
            checkPermissionsAndOpenImageDialog();
        });

        btnPlus.setOnClickListener(v -> incrementQuantity());
        btnMinus.setOnClickListener(v -> decrementQuantity());

        if (currentItem != null) {
            edName.setText(currentItem.getName());
            edCategory.setText(currentItem.getCategory());
            edExpiryDate.setText(currentItem.getExpiryDate());
            tvQuantity.setText(String.valueOf(currentItem.getQuantity()));

            if (currentItem.getImage() != null) {
                selectedImage = BitmapFactory.decodeByteArray(
                        currentItem.getImage(), 0, currentItem.getImage().length);
                foodImage.setImageBitmap(selectedImage);
            }
        }

        btnClose.setOnClickListener(v -> finish());

        btnCancel.setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> saveItem());
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
        builder.setTitle("Change Food Photo");
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

    private void decrementQuantity() {
        int currentQuantity = Integer.parseInt(tvQuantity.getText().toString());
        if (currentQuantity > 1) {
            currentQuantity--;
            tvQuantity.setText(String.valueOf(currentQuantity));
        } else {
            Toast.makeText(this, "Quantity cannot be less than 1 or delete item", Toast.LENGTH_SHORT).show();
        }
    }

    private void incrementQuantity() {
        int currentQuantity = Integer.parseInt(tvQuantity.getText().toString());
        currentQuantity++;
        tvQuantity.setText(String.valueOf(currentQuantity));
    }

    private void showDatePicker() {
        if(!currentItem.getExpiryDate().isEmpty()){
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                Date date = sdf.parse(currentItem.getExpiryDate());
                if (date != null) {
                    calendar.setTime(date);
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        datePickerDialog = new DatePickerDialog(
                this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    String formattedDate = String.format(Locale.getDefault(),
                            "%02d/%02d/%d", selectedDay, (selectedMonth + 1), selectedYear);
                    edExpiryDate.setText(formattedDate);
                },
                year, month, day);

        datePickerDialog.show();
    }

    private void showCategoryPopup() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View popupView = getLayoutInflater().inflate(R.layout.custom_popup, null);

        builder.setView(popupView);

        categoryRadioGroup = popupView.findViewById(R.id.radioGroup);
        Button cancelButton = popupView.findViewById(R.id.cancelPopup);
        Button saveButton = popupView.findViewById(R.id.savePopup);

        if (!currentItem.getCategory().isEmpty()) {
            int radioId = getRadioButtonIdForCategory(currentItem.getCategory());
            if (radioId != -1) {
                categoryRadioGroup.check(radioId);
            }
        }

        AlertDialog dialog = builder.create();

        cancelButton.setOnClickListener(v -> dialog.dismiss());

        saveButton.setOnClickListener(v -> {
            int selectedId = categoryRadioGroup.getCheckedRadioButtonId();
            if (selectedId != -1) {
                RadioButton radioButton = popupView.findViewById(selectedId);
                selectedCategory = radioButton.getText().toString();
                edCategory.setText(selectedCategory);
            }
            dialog.dismiss();
        });

        dialog.show();
    }

    private int getRadioButtonIdForCategory(String category) {
        switch (category) {
            case "Fats & Oils": return R.id.radioButton;
            case "Fruit": return R.id.radioButton2;
            case "Vegetable": return R.id.radioButton3;
            case "Dairy": return R.id.radioButton4;
            case "Beverage": return R.id.radioButton5;
            case "Meat": return R.id.radioButton6;
            case "Sweet": return R.id.radioButton7;
            case "Junk": return R.id.radioButton8;
            default: return -1;
        }
    }

    private void saveItem() {
        String name = edName.getText().toString().trim();
        String category = edCategory.getText().toString().trim();
        String expiryDate = edExpiryDate.getText().toString().trim();
        int quantity = Integer.parseInt(tvQuantity.getText().toString());

        if(name.isEmpty() || category.isEmpty() || expiryDate.isEmpty() || quantity <=0){
            Toast.makeText(this, "Please fill in all details", Toast.LENGTH_SHORT).show();
            return;
        }

        byte[] imageBytes = null;
        if (selectedImage != null) {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            selectedImage.compress(Bitmap.CompressFormat.PNG, 100, stream);
            imageBytes = stream.toByteArray();
        }

        currentItem.setName(name);
        currentItem.setCategory(category);
        currentItem.setExpiryDate(expiryDate);
        currentItem.setQuantity(quantity);
        currentItem.setImage(imageBytes);

        boolean updated = db.updateFoodItem(currentItem,session.getUserEmail());

        if (updated) {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("updatedItem", currentItem);
            setResult(RESULT_OK, resultIntent);
            finish();
        } else {
            Toast.makeText(this, "Failed to update item", Toast.LENGTH_SHORT).show();
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