package com.example.foodkeeper.profile_activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.foodkeeper.FoodkeeperUtils.Database;
import com.example.foodkeeper.R;
import com.example.foodkeeper.Register.SessionManager;
import com.example.foodkeeper.Fridge.models.User;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textfield.TextInputEditText;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ProfileActivity extends AppCompatActivity {

    private TextInputEditText etName, etSurname, etEmail, etPhone;
    private ShapeableImageView ivProfile;
    private ImageButton btnSelectImage;
    private Button btnEditProfile, btnSaveProfile, btnChangePassword, backBtn;

    private Database dbHelper;
    private String currentEmail;
    private byte[] profileImage;
    private boolean isEditMode = false;
    SessionManager session;

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int TAKE_PHOTO_REQUEST = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_activtty);

        etName = findViewById(R.id.etName);
        etSurname = findViewById(R.id.etSurname);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        ivProfile = findViewById(R.id.ivProfile);
        btnSelectImage = findViewById(R.id.btnSelectImage);
        btnEditProfile = findViewById(R.id.btnEditProfile);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        btnChangePassword = findViewById(R.id.btnChangePassword);
        backBtn = findViewById(R.id.backBtn);

        dbHelper = new Database(this);
        session = new SessionManager(this);
        currentEmail = session.getUserEmail();

        if (currentEmail == null || currentEmail.isEmpty()) {
            Toast.makeText(this, "Error: User not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadUserProfile();

        btnSelectImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showImagePickerDialog();
            }
        });

        btnEditProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                enableEditMode();
            }
        });

        btnSaveProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveProfile();
            }
        });

        btnChangePassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openChangePasswordDialog();
            }
        });

        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void loadUserProfile() {
        User user = dbHelper.loadUserByEmail(currentEmail);

        if (user != null) {
            etName.setText(user.getName());
            etSurname.setText(user.getSurname());
            etEmail.setText(user.getEmail());
            etPhone.setText(user.getPhone());

            if (user.getProfileImage() != null) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(
                        user.getProfileImage(), 0, user.getProfileImage().length);
                ivProfile.setImageBitmap(bitmap);
                profileImage = user.getProfileImage();
            }
        }
    }

    private void enableEditMode() {
        isEditMode = true;

        etName.setEnabled(true);
        etSurname.setEnabled(true);
        etPhone.setEnabled(true);
        etEmail.setEnabled(false);

        btnSelectImage.setVisibility(View.VISIBLE);

        btnEditProfile.setVisibility(View.GONE);
        btnSaveProfile.setVisibility(View.VISIBLE);

        Toast.makeText(this, "Edit mode enabled", Toast.LENGTH_SHORT).show();
    }

    private void disableEditMode() {
        isEditMode = false;

        etName.setEnabled(false);
        etSurname.setEnabled(false);
        etEmail.setEnabled(false);
        etPhone.setEnabled(false);

        btnSelectImage.setVisibility(View.GONE);

        btnEditProfile.setVisibility(View.VISIBLE);
        btnSaveProfile.setVisibility(View.GONE);
    }

    private void showImagePickerDialog() {
        if (!isEditMode) {
            Toast.makeText(this, "Please enable edit mode first", Toast.LENGTH_SHORT).show();
            return;
        }

        final CharSequence[] options = {"Take Photo", "Choose from Gallery", "Remove Photo", "Cancel"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Option");
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                if (options[item].equals("Take Photo")) {
                    takePhoto();
                } else if (options[item].equals("Choose from Gallery")) {
                    chooseFromGallery();
                } else if (options[item].equals("Remove Photo")) {
                    removePhoto();
                } else if (options[item].equals("Cancel")) {
                    dialog.dismiss();
                }
            }
        });
        builder.show();
    }

    private void takePhoto() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, TAKE_PHOTO_REQUEST);
        } else {
            Toast.makeText(this, "Camera not available", Toast.LENGTH_SHORT).show();
        }
    }

    private void chooseFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    private void removePhoto() {
        ivProfile.setImageResource(R.drawable.image_placeholder);
        profileImage = null;
        Toast.makeText(this, "Photo removed", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && data != null) {
            if (requestCode == PICK_IMAGE_REQUEST && data.getData() != null) {
                Uri imageUri = data.getData();
                try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                    ivProfile.setImageBitmap(bitmap);
                    profileImage = getByteArrayFromBitmap(bitmap);
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                }
            } else if (requestCode == TAKE_PHOTO_REQUEST) {
                Bundle extras = data.getExtras();
                Bitmap bitmap = (Bitmap) extras.get("data");
                ivProfile.setImageBitmap(bitmap);
                profileImage = getByteArrayFromBitmap(bitmap);
            }
        }
    }

    private byte[] getByteArrayFromBitmap(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        return stream.toByteArray();
    }

    private void saveProfile() {
        String name = etName.getText().toString().trim();
        String surname = etSurname.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();

        if (name.isEmpty() || surname.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean isUpdated = dbHelper.updateUserProfile(currentEmail, name, surname,
                currentEmail, phone, profileImage);

        if (isUpdated) {
            Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
            disableEditMode();
        } else {
            Toast.makeText(this, "Failed to update profile", Toast.LENGTH_SHORT).show();
        }
    }

    private void openChangePasswordDialog() {
        Intent intent = new Intent(this, ChangePasswordActivity.class);
        intent.putExtra("USER_EMAIL", currentEmail);
        startActivity(intent);
    }
}