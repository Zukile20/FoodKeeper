package com.example.foodkeeper;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static com.example.foodkeeper.RegisterActivity.isValidPassword;

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
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textfield.TextInputEditText;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ProfileActivity extends AppCompatActivity {

    private ShapeableImageView profileImage;
    private Button backBtn;
    private ImageButton loadPicture;
    private TextView titleHeader;
    private EditText etName, etSurname, etEmail, etPhone;
    private Button btnSave, btnEdit, btnCancel, btnChangePassword, btnSavePassword, btnCancelPassword;

    private LinearLayout currentPasswordLayout, newPasswordLayout, confirmPasswordLayout;
    private TextInputLayout currentPasswordInputLayout, newPasswordInputLayout, confirmPasswordInputLayout;
    private TextInputEditText etCurrentPassword, etNewPassword, etConfirmPassword;

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
        btnSavePassword = findViewById(R.id.btnSavePassword);
        btnCancelPassword = findViewById(R.id.btnCancelPassword);

        // Password layouts (LinearLayout containers)
        currentPasswordLayout = findViewById(R.id.currentPasswordLayout);
        newPasswordLayout = findViewById(R.id.newPasswordLayout);
        confirmPasswordLayout = findViewById(R.id.confirmPasswordLayout);

        // Get TextInputLayouts from the XML
        currentPasswordInputLayout = (TextInputLayout) currentPasswordLayout.findViewById(R.id.etCurrentPassword).getParent().getParent();
        newPasswordInputLayout = (TextInputLayout) newPasswordLayout.findViewById(R.id.etNewPassword).getParent().getParent();
        confirmPasswordInputLayout = (TextInputLayout) confirmPasswordLayout.findViewById(R.id.etConfirmPassword).getParent().getParent();

        // Get TextInputEditTexts
        etCurrentPassword = findViewById(R.id.etCurrentPassword);
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);

        // Configure TextInputLayouts for proper error handling
        currentPasswordInputLayout.setErrorEnabled(true);
        currentPasswordInputLayout.setErrorIconDrawable(null);

        newPasswordInputLayout.setErrorEnabled(true);
        newPasswordInputLayout.setErrorIconDrawable(null);

        confirmPasswordInputLayout.setErrorEnabled(true);
        confirmPasswordInputLayout.setErrorIconDrawable(null);

        setFieldsEditable(false);
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

    private void setupDatabase() {
        dbHelper = new Database(this);
    }

    private void setupClickListeners() {
        backBtn.setOnClickListener(v->finish());
        loadPicture.setOnClickListener(v -> showImagePickerDialog());
        btnEdit.setOnClickListener(v -> enableEditMode());

        // Save button for profile changes
        btnSave.setOnClickListener(v -> saveUserData());

        // Cancel button for profile changes
        btnCancel.setOnClickListener(v -> {
            disableEditMode();
            loadUserData();
            if (user != null) {
                initializeFields();
            }
        });

        // Change Password button - toggles password mode
        btnChangePassword.setOnClickListener(v -> enablePasswordChangeMode());

        // Save Password button - saves password changes
        btnSavePassword.setOnClickListener(v -> savePasswordChange());

        // Cancel Password button - cancels password changes
        btnCancelPassword.setOnClickListener(v -> cancelPasswordChange());

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
        btnSave.setVisibility(VISIBLE);
        btnCancel.setVisibility(VISIBLE);
        loadPicture.setVisibility(VISIBLE);
        titleHeader.setText("Edit Profile");

        Toast.makeText(this, "You can now edit your profile", Toast.LENGTH_SHORT).show();
    }

    private void disableEditMode() {
        isEditMode = false;
        setFieldsEditable(false);

        btnEdit.setVisibility(VISIBLE);
        btnSave.setVisibility(View.GONE);
        btnCancel.setVisibility(View.GONE);
        loadPicture.setVisibility(View.GONE);
        titleHeader.setText("My Profile");
    }

    private void enablePasswordChangeMode() {
        isPasswordChangeMode = true;

        // Show password fields
        currentPasswordLayout.setVisibility(VISIBLE);
        newPasswordLayout.setVisibility(VISIBLE);
        confirmPasswordLayout.setVisibility(VISIBLE);

        // Update buttons
        btnChangePassword.setVisibility(View.GONE);
        btnSavePassword.setVisibility(VISIBLE);
        btnCancelPassword.setVisibility(VISIBLE);
        btnEdit.setVisibility(INVISIBLE);

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
        btnChangePassword.setVisibility(VISIBLE);
        btnSavePassword.setVisibility(View.GONE);
        btnCancelPassword.setVisibility(View.GONE);

        clearPasswordFields();
    }

    private void clearPasswordFields() {
        etCurrentPassword.setText("");
        etNewPassword.setText("");
        etConfirmPassword.setText("");

        // Clear all errors
        currentPasswordInputLayout.setError(null);
        newPasswordInputLayout.setError(null);
        confirmPasswordInputLayout.setError(null);
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
            currentPasswordInputLayout.setError("Current password is incorrect");
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
        } else {
            newPasswordInputLayout.setError("Password must be at least 8 characters with letter, digit and special symbol");
            etNewPassword.requestFocus();
        }
    }

    private boolean validatePasswordChange() {
        String currentPassword = etCurrentPassword.getText().toString().trim();
        String password = etNewPassword.getText().toString().trim();
        String confirm = etConfirmPassword.getText().toString().trim();

        // Clear previous errors
        currentPasswordInputLayout.setError(null);
        newPasswordInputLayout.setError(null);
        confirmPasswordInputLayout.setError(null);

        if (currentPassword.isEmpty()) {
            currentPasswordInputLayout.setError("Current password is required");
            etCurrentPassword.requestFocus();
            return false;
        }

        if (password.isEmpty()) {
            newPasswordInputLayout.setError("New password is required");
            etNewPassword.requestFocus();
            return false;
        }

        if (!isValidPassword(password)) {
            newPasswordInputLayout.setError("Password must be at least 8 characters with letter, digit and special symbol");
            etNewPassword.requestFocus();
            return false;
        }

        if (confirm.isEmpty()) {
            confirmPasswordInputLayout.setError("Please confirm your password");
            etConfirmPassword.requestFocus();
            return false;
        }

        if(!password.equals(confirm)) {
            confirmPasswordInputLayout.setError("Passwords do not match");
            etConfirmPassword.requestFocus();
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