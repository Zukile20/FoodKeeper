package com.example.foodkeeper;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
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
    TextView txtSignIn, uploadImageTxt;
    Database db;
    ImageView profileImage;
    Bitmap profileBitmap;
    Uri imageUri;
    static final int PICK_IMAGE_REQUEST = 100;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);

        db = new Database(this);

        profileImage = findViewById(R.id.profileImageView);
        uploadImageTxt = findViewById(R.id.uploadImageButton);
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

        uploadImageTxt.setOnClickListener(v -> OpenImagePicker());

        createBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerUser();
            }
        });
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

        byte[] imageBlob;
        if(profileBitmap != null)
            imageBlob = imageToBlob(profileBitmap);
        else
            imageBlob = null;

        if(name.length() == 0 || surname.length() == 0 || email.length() == 0 ||
                password.length() == 0 || confirm.length() == 0 ||
                fridgeBrand.length() == 0 || fridgeModel.length() == 0 || fridgeSizeStr.length() == 0) {
            Toast.makeText(getApplicationContext(), "Please fill in all the details", Toast.LENGTH_SHORT).show();
            return;
        }

        if(!password.equals(confirm)) {
            Toast.makeText(getApplicationContext(), "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        Integer fridgeSize = null;
        String phone = phoneStr;
        if (!phone.matches("\\d+")) {
            Toast.makeText(getApplicationContext(), "Please enter a valid phone number", Toast.LENGTH_SHORT).show();
            return;
        }

        if(!isValid(password)) {
            Toast.makeText(getApplicationContext(),
                    "Password must be at least 8 characters with letter, digit and special symbol",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if(isEmailExists(email)) {
            Toast.makeText(getApplicationContext(), "Email already registered", Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            intent.putExtra("USER_EMAIL", email);
            startActivity(intent);
            finish();
        }

        db.register(name, surname, email, Integer.valueOf(phone), password, imageBlob);

        Fridge fridge = new Fridge(fridgeBrand, fridgeModel, fridgeDescription, fridgeSize, true, null);
        long fridgeId = db.addFridge(fridge,email);

        if (fridgeId != -1) {
            Toast.makeText(getApplicationContext(), "Registered Successfully with your first fridge!", Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            intent.putExtra("USER_EMAIL", email);
            startActivity(intent);
            finish();
        } else {
            Toast.makeText(getApplicationContext(), "Registration failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void OpenImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    private boolean isEmailExists(String email) {
        SQLiteDatabase db = this.db.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM users WHERE email = ?", new String[]{email});
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
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

    private byte[] imageToBlob(Bitmap bitmap){
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream);
        return outputStream.toByteArray();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            try {
                profileBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                profileImage.setImageBitmap(profileBitmap);
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
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