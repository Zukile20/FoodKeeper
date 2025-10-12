package com.example.foodkeeper;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ProfileActivity extends AppCompatActivity {

    private ShapeableImageView profileImage;
    private Button backBtn;
    private Button loadPicture;
    private TextView titleHeader;
    private EditText etName, etSurname, etEmail, etPhone;
    private Button btnSave, btnEdit, btnCancel, btnChangePassword;

    // Password fields with toggle functionality
    private LinearLayout currentPasswordLayout, newPasswordLayout, confirmPasswordLayout;
    private EditText etCurrentPassword, etNewPassword, etConfirmPassword;
    private ImageButton btnToggleCurrentPassword, btnToggleNewPassword, btnToggleConfirmPassword;
    private boolean isCurrentPasswordVisible = false;
    private boolean isNewPasswordVisible = false;
    private boolean isConfirmPasswordVisible = false;

    private Database dbHelper;
    private boolean isEditMode = false;
    private boolean isPasswordChangeMode = false;

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int CAMERA_REQUEST = 2;
    private static final int PERMISSION_REQUEST_CODE = 100;

    // Database constants
    private static final String KEY_NAME = "name";
    private static final String KEY_SURNAME = "surname";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_PHONE = "phone";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_PROFILE = "profile_image";

    User user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_activtty);

        initializeViews();
        setupDatabase();
        setupClickListeners();
        loadUserData();

        if (user != null) {
            initializeFields();
        } else {
            Toast.makeText(this, "Error loading user data", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initializeViews() {
        // Find views from layout
        profileImage = findViewById(R.id.profileImageView);
        loadPicture = findViewById(R.id.loadPicture);
        backBtn = findViewById(R.id.backBtn);
        titleHeader = findViewById(R.id.titleHeader);

        etName = findViewById(R.id.etName);
        etSurname = findViewById(R.id.etSurname);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);

        btnEdit = findViewById(R.id.btnEdit);
        btnSave = findViewById(R.id.btnSave);
        btnCancel = findViewById(R.id.btnCancel);
        btnChangePassword = findViewById(R.id.btnChangePassword);

        // Password fields and layouts
        currentPasswordLayout = findViewById(R.id.currentPasswordLayout);
        newPasswordLayout = findViewById(R.id.newPasswordLayout);
        confirmPasswordLayout = findViewById(R.id.confirmPasswordLayout);
        etCurrentPassword = findViewById(R.id.etCurrentPassword);
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);

        // Create password toggle buttons programmatically
        createPasswordToggleButtons();

        // Initially set fields to read-only
        setFieldsEditable(false);
    }

    private void createPasswordToggleButtons() {
        // Create toggle buttons for password visibility
        btnToggleCurrentPassword = new ImageButton(this);
        btnToggleCurrentPassword.setBackground(null);
        btnToggleCurrentPassword.setPadding(8, 8, 8, 8);
        btnToggleCurrentPassword.setOnClickListener(v -> togglePasswordVisibility(etCurrentPassword, 1));

        btnToggleNewPassword = new ImageButton(this);
        btnToggleNewPassword.setBackground(null);
        btnToggleNewPassword.setPadding(8, 8, 8, 8);
        btnToggleNewPassword.setOnClickListener(v -> togglePasswordVisibility(etNewPassword, 2));

        btnToggleConfirmPassword = new ImageButton(this);
        btnToggleConfirmPassword.setBackground(null);
        btnToggleConfirmPassword.setPadding(8, 8, 8, 8);
        btnToggleConfirmPassword.setOnClickListener(v -> togglePasswordVisibility(etConfirmPassword, 3));

        // Add toggle buttons to password layouts
        addToggleButtonToLayout(currentPasswordLayout, btnToggleCurrentPassword);
        addToggleButtonToLayout(newPasswordLayout, btnToggleNewPassword);
        addToggleButtonToLayout(confirmPasswordLayout, btnToggleConfirmPassword);
    }

    private void addToggleButtonToLayout(LinearLayout passwordLayout, ImageButton toggleButton) {
        LinearLayout horizontalLayout = new LinearLayout(this);
        horizontalLayout.setOrientation(LinearLayout.HORIZONTAL);
        horizontalLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        EditText passwordField = null;
        for (int i = 0; i < passwordLayout.getChildCount(); i++) {
            View child = passwordLayout.getChildAt(i);
            if (child instanceof EditText) {
                passwordField = (EditText) child;
                break;
            }
        }

        if (passwordField != null) {
            passwordLayout.removeView(passwordField);

            LinearLayout.LayoutParams editTextParams = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f
            );
            passwordField.setLayoutParams(editTextParams);

            LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                    48, 48
            );
            buttonParams.setMargins(8, 0, 0, 0);
            toggleButton.setLayoutParams(buttonParams);

            horizontalLayout.addView(passwordField);
            horizontalLayout.addView(toggleButton);

            passwordLayout.addView(horizontalLayout);
        }
    }
    public static boolean isValid(String password) {
        int f1 = 0, f2 = 0, f3 = 0;
        if(password.length() < 8){
            return false;
        } else {
            for (int p = 0; p < password.length(); p++){
                if(Character.isLetter(password.charAt(p))){
                    f1 = 1;
                }
            }
            for (int r = 0; r < password.length(); r++){
                if(Character.isDigit(password.charAt(r))){
                    f2 = 1;
                }
            }
            for (int s = 0; s < password.length(); s++){
                char c = password.charAt(s);
                if(c >= 33 && c <= 46 || c == 64){
                    f3 = 1;
                }
            }
            if (f1 == 1 && f2 == 1 && f3 == 1) {
                return true;
            }
            return false;
        }
    }
    private void togglePasswordVisibility(EditText editText, int passwordType) {
        boolean isVisible = false;

        switch (passwordType) {
            case 1: // Current password
                isCurrentPasswordVisible = !isCurrentPasswordVisible;
                isVisible = isCurrentPasswordVisible;
                btnToggleCurrentPassword.setImageResource(
                        isVisible ? android.R.drawable.ic_secure : android.R.drawable.ic_menu_view
                );
                break;
            case 2: // New password
                isNewPasswordVisible = !isNewPasswordVisible;
                isVisible = isNewPasswordVisible;
                btnToggleNewPassword.setImageResource(
                        isVisible ? android.R.drawable.ic_secure : android.R.drawable.ic_menu_view
                );
                break;
            case 3: // Confirm password
                isConfirmPasswordVisible = !isConfirmPasswordVisible;
                isVisible = isConfirmPasswordVisible;
                btnToggleConfirmPassword.setImageResource(
                        isVisible ? android.R.drawable.ic_secure : android.R.drawable.ic_menu_view
                );
                break;
        }

        // Toggle input type
        if (isVisible) {
            editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        } else {
            editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        }

        editText.setSelection(editText.getText().length());
    }

    private void setupDatabase() {
        dbHelper = new Database(this);
    }

    private void setupClickListeners() {backBtn.setOnClickListener(v->finish());
        loadPicture.setOnClickListener(v -> showImagePickerDialog());
        btnEdit.setOnClickListener(v -> enableEditMode());

        btnSave.setOnClickListener(v -> {
            if (isPasswordChangeMode) {
                savePasswordChange();
            } else {
                saveUserData();
            }
        });

        btnCancel.setOnClickListener(v -> {
            if (isPasswordChangeMode) {
                cancelPasswordChange();
            } else {
                disableEditMode();
                loadUserData();
                if (user != null) {
                    initializeFields();
                }
            }
        });

        btnChangePassword.setOnClickListener(v -> {
            if (isPasswordChangeMode) {
                cancelPasswordChange();
            } else {
                enablePasswordChangeMode();
            }
        });

        profileImage.setOnClickListener(v -> {
            if (isEditMode && !isPasswordChangeMode) {
                showImagePickerDialog();
            }
        });
    }

    private void loadUserData() {
        try {
            SessionManager session = new SessionManager(this);
            String userEmail = session.getUserEmail();

            if (userEmail != null && !userEmail.isEmpty()) {
                user = dbHelper.loadUserByEmail(userEmail);
                if (user == null) {
                    Toast.makeText(this, "User data not found", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "No user session found", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error loading user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void initializeFields() {
        if (user == null) return;

        etName.setText(user.getName() != null ? user.getName() : "");
        etSurname.setText(user.getSurname() != null ? user.getSurname() : "");
        etEmail.setText(user.getEmail() != null ? user.getEmail() : "");
        etPhone.setText(user.getPhone() != null ? user.getPhone() : "");

        // Initialize profile image
        if (user.getProfileImage() != null && user.getProfileImage().length > 0) {
            try {
                Glide.with(this)
                        .load(user.getProfileImage())
                        .placeholder(R.drawable.image_placeholder)
                        .error(R.drawable.image_placeholder)
                        .into(profileImage);
            } catch (Exception e) {
                profileImage.setImageResource(R.drawable.image_placeholder);
            }
        } else {
            profileImage.setImageResource(R.drawable.image_placeholder);
        }
    }

    private void enableEditMode() {
        isEditMode = true;
        setFieldsEditable(true);

        btnEdit.setVisibility(View.GONE);
        btnSave.setVisibility(View.VISIBLE);
        btnCancel.setVisibility(View.VISIBLE);
        loadPicture.setVisibility(View.VISIBLE);
        titleHeader.setText("Edit Profile");

        Toast.makeText(this, "You can now edit your profile", Toast.LENGTH_SHORT).show();
    }

    private void disableEditMode() {
        isEditMode = false;
        setFieldsEditable(false);

        btnEdit.setVisibility(View.VISIBLE);
        btnSave.setVisibility(View.GONE);
        btnCancel.setVisibility(View.GONE);
        loadPicture.setVisibility(View.GONE);
        titleHeader.setText("My Profile");
    }

    private void enablePasswordChangeMode() {
        isPasswordChangeMode = true;

        // Hide profile edit functionality
        etName.setEnabled(false);
        etSurname.setEnabled(false);
        etPhone.setEnabled(false);
        loadPicture.setVisibility(View.GONE);

        // Show password fields
        currentPasswordLayout.setVisibility(View.VISIBLE);
        newPasswordLayout.setVisibility(View.VISIBLE);
        confirmPasswordLayout.setVisibility(View.VISIBLE);

        // Update buttons
        btnEdit.setVisibility(View.GONE);
        btnSave.setVisibility(View.VISIBLE);
        btnCancel.setVisibility(View.VISIBLE);
        btnChangePassword.setText("Cancel Password Change");
        btnChangePassword.setBackground(ContextCompat.getDrawable(this, R.drawable.grey_button));
        btnChangePassword.setBackgroundTintList(ColorStateList.valueOf(Color.RED));
        btnChangePassword.setTextColor(ContextCompat.getColor(this, R.color.white));
        titleHeader.setText("Change Password");

        // Clear and reset password fields
        clearPasswordFields();
        Toast.makeText(this, "Enter your current and new password", Toast.LENGTH_SHORT).show();
    }

    private void cancelPasswordChange() {
        isPasswordChangeMode = false;

        // Hide password fields
        currentPasswordLayout.setVisibility(View.GONE);
        newPasswordLayout.setVisibility(View.GONE);
        confirmPasswordLayout.setVisibility(View.GONE);

        // Reset buttons
        btnEdit.setVisibility(View.VISIBLE);
        btnSave.setVisibility(View.GONE);
        btnCancel.setVisibility(View.GONE);
        btnChangePassword.setText("Change Password");
        btnChangePassword.setBackground(ContextCompat.getDrawable(this, R.drawable.grey_button));
        btnChangePassword.setBackgroundTintList(ColorStateList.valueOf(
                ContextCompat.getColor(this, R.color.light_pink)
        ));

        btnChangePassword.setTextColor(ContextCompat.getColor(this, R.color.dark_blue));
        titleHeader.setText("My Profile");

        // Reset edit mode if it was active
        if (isEditMode) {
            disableEditMode();
            loadUserData();
            if (user != null) {
                initializeFields();
            }
        }

        clearPasswordFields();
    }

    private void clearPasswordFields() {
        etCurrentPassword.setText("");
        etNewPassword.setText("");
        etConfirmPassword.setText("");

        // Reset password visibility
        isCurrentPasswordVisible = false;
        isNewPasswordVisible = false;
        isConfirmPasswordVisible = false;

        etCurrentPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        etNewPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        etConfirmPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
    }

    private void savePasswordChange() {
        if (user == null) {
            Toast.makeText(this, "User data not available", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!validatePasswordChange()) {
            return;
        }

        String currentPassword = etCurrentPassword.getText().toString().trim();
        String newPassword = etNewPassword.getText().toString().trim();

        if (!verifyCurrentPassword(currentPassword)) {
            etCurrentPassword.setError("Current password is incorrect");
            etCurrentPassword.requestFocus();
            return;
        }
        if(isValid(newPassword)) {
            int result = dbHelper.updatePassword(user.getEmail(), newPassword);
            if (result > 0) {
                Toast.makeText(this, "Password changed successfully!", Toast.LENGTH_SHORT).show();
                cancelPasswordChange();
            } else {
                Toast.makeText(this, "Failed to change password", Toast.LENGTH_SHORT).show();
            }
        }


    }

    private boolean validatePasswordChange() {
        String currentPassword = etCurrentPassword.getText().toString().trim();
        String newPassword = etNewPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        if (currentPassword.isEmpty()) {
            etCurrentPassword.setError("Current password is required");
            etCurrentPassword.requestFocus();
            return false;
        }

        if (newPassword.isEmpty()) {
            etNewPassword.setError("New password is required");
            etNewPassword.requestFocus();
            return false;
        }

        if (newPassword.length() < 6) {
            etNewPassword.setError("Password must be at least 6 characters");
            etNewPassword.requestFocus();
            return false;
        }

        if (!newPassword.equals(confirmPassword)) {
            etConfirmPassword.setError("Passwords do not match");
            etConfirmPassword.requestFocus();
            return false;
        }

        if (currentPassword.equals(newPassword)) {
            etNewPassword.setError("New password must be different from current password");
            etNewPassword.requestFocus();
            return false;
        }

        return true;
    }

    private boolean verifyCurrentPassword(String hashedPassword) {
        if (user == null || hashedPassword == null) {
            return false;
        }

        try {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            String[] columns = {KEY_PASSWORD};
            String selection = KEY_EMAIL + " = ?";
            String[] selectionArgs = {user.getEmail()};

            Cursor cursor = db.query("users", columns, selection, selectionArgs, null, null, null);

            boolean isValid = false;
            if (cursor.moveToFirst()) {
                String storedPassword = cursor.getString(cursor.getColumnIndexOrThrow(KEY_PASSWORD));
                isValid = hashedPassword.equals(storedPassword);
            }

            cursor.close();
            return isValid;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void setFieldsEditable(boolean editable) {
        etName.setEnabled(editable);
        etSurname.setEnabled(editable);
        etEmail.setEnabled(false); // Email should not be editable
        etPhone.setEnabled(editable);
    }

    private void saveUserData() {
        if (user == null) {
            Toast.makeText(this, "User data not available", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!validateInputs()) {
            return;
        }

        try {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            ContentValues values = new ContentValues();

            values.put(KEY_NAME, etName.getText().toString().trim());
            values.put(KEY_SURNAME, etSurname.getText().toString().trim());
            values.put(KEY_PHONE, etPhone.getText().toString().trim());

            // Handle profile image
            if (selectedImageUri != null) {
                Bitmap selectedImage = MediaStore.Images.Media.getBitmap(
                        this.getContentResolver(), selectedImageUri);
                profileImage.setImageBitmap(selectedImage);

                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                selectedImage.compress(Bitmap.CompressFormat.PNG, 100, stream);
                byte[] imageBytes = stream.toByteArray();

                values.put(KEY_PROFILE, imageBytes);
            }
            else
            {
             values.put(KEY_PROFILE, (byte[]) null);
            }

            String whereClause = KEY_EMAIL + " = ?";
            String[] whereArgs = {user.getEmail()};

            int rowsAffected = db.update("users", values, whereClause, whereArgs);

            if (rowsAffected > 0) {
                Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                // Update user object with new data
                user.setName(etName.getText().toString().trim());
                user.setSurname(etSurname.getText().toString().trim());
                user.setPhone(etPhone.getText().toString().trim());
                SessionManager sess= new SessionManager(this);
                sess.createLoginSession(user.getEmail(),user.getName());
                disableEditMode();
            } else {
                Toast.makeText(this, "Failed to update profile", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error updating profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private boolean validateInputs() {
        if (etName.getText().toString().trim().isEmpty()) {
            etName.setError("Name is required");
            etName.requestFocus();
            return false;
        }

        if (etSurname.getText().toString().trim().isEmpty()) {
            etSurname.setError("Surname is required");
            etSurname.requestFocus();
            return false;
        }

        String phone = etPhone.getText().toString().trim();
        if (!phone.isEmpty() && !isValidPhoneNumber(phone)) {
            etPhone.setError("Please enter a valid phone number");
            etPhone.requestFocus();
            return false;
        }

        return true;
    }

    private boolean isValidPhoneNumber(String phone) {
        return phone.matches("\\+?[0-9]{10,15}");
    }

    private void showImagePickerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Profile Picture");

        String[] options = {"Camera", "Gallery", "Remove Picture"};

        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0:
                    if (checkCameraPermission()) {
                        openCamera();
                    } else {
                        requestCameraPermission();
                    }
                    break;
                case 1:
                    openGallery();
                    break;
                case 2:
                    profileImage.setImageResource(R.drawable.image_placeholder);
                    selectedImageUri=null;
                    break;
            }
        });

        builder.show();
    }

    private boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CODE);
    }

    private void openCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(cameraIntent, CAMERA_REQUEST);
        }
    }

    private void openGallery() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    private Uri selectedImageUri;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == PICK_IMAGE_REQUEST && data != null && data.getData() != null) {
                Uri imageUri = data.getData();
                selectedImageUri = imageUri;
                try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                    Bitmap scaledBitmap = getScaledBitmap(bitmap, 300, 300);
                    profileImage.setImageBitmap(scaledBitmap);
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                }
            } else if (requestCode == CAMERA_REQUEST && data != null) {
                Bundle extras = data.getExtras();
                if (extras != null) {
                    Bitmap bitmap = (Bitmap) extras.get("data");
                    if (bitmap != null) {
                        Bitmap scaledBitmap = getScaledBitmap(bitmap, 300, 300);
                        profileImage.setImageBitmap(scaledBitmap);
                    }
                }
            }
        }
    }

    private Bitmap getScaledBitmap(Bitmap bitmap, int maxWidth, int maxHeight) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        float scaleWidth = ((float) maxWidth) / width;
        float scaleHeight = ((float) maxHeight) / height;
        float scale = Math.min(scaleWidth, scaleHeight);

        Matrix matrix = new Matrix();
        matrix.postScale(scale, scale);

        return Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, false);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}