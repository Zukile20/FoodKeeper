package com.example.foodkeeper;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.foodkeeper.Fridge.Fridge;
import com.google.android.material.textfield.TextInputEditText;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class RegisterActivity extends AppCompatActivity {
    TextInputEditText edName, edSurname, edEmail, edPhone, edPassword, edConfirm, edFridgeBrand, edFridgeModel, edFridgeSize, edFridgeDescription;
    Button createBtn;
    TextView txtSignIn;
    ImageButton uploadImageBtn;
    Database db;
    ImageView profileImage;
    Bitmap profileBitmap;
    Uri imageUri;
    static final int PICK_IMAGE_REQUEST = 100;
    static final int CAMERA_REQUEST = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);

        db = new Database(this);

        profileImage = findViewById(R.id.profileImageView);
        uploadImageBtn = findViewById(R.id.uploadImageButton);
        edName = findViewById(R.id.nameEditText);
        edSurname = findViewById(R.id.surnameEditText);
        edEmail = findViewById(R.id.emailEditText);
        edPhone = findViewById(R.id.phoneEditText);
        edPassword = findViewById(R.id.passwordEditText);
        edConfirm = findViewById(R.id.confirmPasswordEditText);
        edFridgeBrand = findViewById(R.id.fridgeBrandEditText);
        edFridgeModel = findViewById(R.id.fridgeModelEditText);
        edFridgeSize = findViewById(R.id.fridgeSizeEditText);
        edFridgeDescription = findViewById(R.id.fridgeDescriptionEditText);
        createBtn = findViewById(R.id.createProfileButton);
        txtSignIn = findViewById(R.id.textViewExistingUser);

        txtSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                finish();
            }
        });

        uploadImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showImagePickerOptions();
            }
        });

        createBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerUser();
            }
        });
    }

    private void showImagePickerOptions() {
        openGallery();
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
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

    private void registerUser() {
        String name = edName.getText().toString();
        String surname = edSurname.getText().toString();
        String email = edEmail.getText().toString();
        String phoneStr = edPhone.getText().toString();
        String password = edPassword.getText().toString();
        String confirm = edConfirm.getText().toString();
        String fridgeBrand = edFridgeBrand.getText().toString();
        String fridgeModel = edFridgeModel.getText().toString();
        String fridgeSizeStr = edFridgeSize.getText().toString();
        String fridgeDescription = edFridgeDescription.getText().toString();

        boolean isValid = true;

        if (name.isEmpty()) {
            edName.setError("Name is required");
            isValid = false;
        }
        if (surname.isEmpty()) {
            edSurname.setError("Surname is required");
            isValid = false;
        }
        if (email.isEmpty()) {
            edEmail.setError("Email is required");
            isValid = false;
        }
        if (phoneStr.isEmpty()) {
            edPhone.setError("Phone number is required");
            isValid = false;
        }
        if (password.isEmpty()) {
            edPassword.setError("Password is required");
            isValid = false;
        }
        if (confirm.isEmpty()) {
            edConfirm.setError("Please confirm your password");
            isValid = false;
        }
        if (fridgeBrand.isEmpty()) {
            edFridgeBrand.setError("Fridge brand is required");
            isValid = false;
        }
        if (fridgeModel.isEmpty()) {
            edFridgeModel.setError("Fridge model is required");
            isValid = false;
        }
        if (fridgeSizeStr.isEmpty()) {
            edFridgeSize.setError("Fridge size is required");
            isValid = false;
        }
        if (fridgeDescription.isEmpty()) {
            edFridgeDescription.setError("Fridge description is required");
            isValid = false;
        }

        if (!isValid) {
            Toast.makeText(this, "Please correct errors", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isValidEmail(email)) {
            edEmail.setError("Invalid email format");
            return;
        }

        if (!phoneStr.matches("\\d+")) {
            edPhone.setError("Phone number must contain only digits");
            return;
        }

        if (!isValidPassword(password)) {
            edPassword.setError("Password must be at least 8 chars with a letter, digit, and symbol");
            return;
        }

        if (!password.equals(confirm)) {
            edConfirm.setError("Passwords do not match");
            return;
        }

        if (isEmailExists(email)) {
            edEmail.setError("Email already registered");
            Toast.makeText(this, "Email already exists, please log in", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            intent.putExtra("USER_EMAIL", email);
            startActivity(intent);
            finish();
            return;
        }

        byte[] imageBlob = null;
        if(profileBitmap != null) {
            imageBlob = imageToBlob(profileBitmap);
        }

        db.register(name, surname, email, phoneStr, password, imageBlob);

        Fridge fridge = new Fridge(fridgeBrand, fridgeModel, fridgeDescription, Integer.parseInt(fridgeSizeStr), true, null);
        long fridgeId = db.addFridge(fridge, email);

        if (fridgeId != -1) {
            Toast.makeText(getApplicationContext(), "Registered Successfully with your first fridge!", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            intent.putExtra("USER_EMAIL", email);
            startActivity(intent);
            finish();
        } else {
            Toast.makeText(getApplicationContext(), "Registration completed but fridge addition failed", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            intent.putExtra("USER_EMAIL", email);
            startActivity(intent);
            finish();
        }
    }

    private boolean isEmailExists(String email) {
        SQLiteDatabase db = this.db.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM users WHERE email = ?", new String[]{email});
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    public boolean isValidEmail(String email){
        return email != null && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    public static boolean isValidPassword(String password) {
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

    private byte[] imageToBlob(Bitmap bitmap){
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream);
        return outputStream.toByteArray();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == PICK_IMAGE_REQUEST && data != null && data.getData() != null) {
                imageUri = data.getData();
                try {
                    profileBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                    profileImage.setImageBitmap(profileBitmap);
                    Toast.makeText(this, "Profile image selected", Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                }
            } else if (requestCode == CAMERA_REQUEST && data != null) {
                Bundle extras = data.getExtras();
                if (extras != null) {
                    profileBitmap = (Bitmap) extras.get("data");
                    profileImage.setImageBitmap(profileBitmap);
                    Toast.makeText(this, "Profile photo taken", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (db != null) {
            db.close();
        }
        super.onDestroy();
    }
}