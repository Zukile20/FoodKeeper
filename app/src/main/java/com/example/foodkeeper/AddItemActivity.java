package com.example.foodkeeper;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputLayout;

import java.util.Calendar;
import java.util.List;

public class AddItemActivity extends AppCompatActivity {
    private int quantity = 1;
    EditText edName, edCategory, edExpiryDate;
    TextInputLayout txt;
    TextView tvQuantity;
    RadioGroup radioBtns;
    Button btnMinus, btnPlus, btnSavePopup, btnCancelPopup, btnSave, btnCancel;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_item);

        edName = findViewById(R.id.nameEditText);
        edCategory = findViewById(R.id.categoryEditText);
        edExpiryDate = findViewById(R.id.expiryDateEditText);
        txt = findViewById(R.id.txtLayout);
        tvQuantity = findViewById(R.id.tv_quantity);
        btnMinus = findViewById(R.id.btn_minus);
        btnPlus = findViewById(R.id.btn_plus);
        btnSave = findViewById(R.id.saveButton);
        btnCancel = findViewById(R.id.cancelButton);

        edCategory.setOnClickListener(v -> {
            showPopUp();
        });

        txt.setEndIconOnClickListener(new View.OnClickListener() {
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
            saveAnItem();
        });

        btnCancel.setOnClickListener(v -> {
            finish();
        });
    }
    private void saveAnItem(){
        String name = edName.getText().toString();
        String category = edCategory.getText().toString();
        String expiryDate = edExpiryDate.getText().toString();
        int quantity = Integer.parseInt(tvQuantity.getText().toString());
        String email = getUserEmail();

        if(name.isEmpty() || category.isEmpty() || expiryDate.isEmpty() || quantity <=0){
            Toast.makeText(this, "Please fill in all details", Toast.LENGTH_SHORT).show();
            return;
        }

        Database db = new Database(this);
        long result = db.addFoodItem(name, category, expiryDate, quantity, email);

        if(result != -1){
            Toast.makeText(this, "Item saved!", Toast.LENGTH_SHORT).show();
//            edName.setText("");
//            tvQuantity.setText("0");
//            edExpiryDate.setText("");

            finish();
        } else {
            Toast.makeText(this, "FAILED!!!", Toast.LENGTH_SHORT).show();
        }
    }
    private String getUserEmail(){
        SharedPreferences shPref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        return shPref.getString("user_email", "");
    }
    private void showPopUp() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View popupView = getLayoutInflater().inflate(R.layout.custom_popup, null);

        builder.setView(popupView);

        btnSavePopup = popupView.findViewById(R.id.savePopup);
        btnCancelPopup = popupView.findViewById(R.id.cancelPopup);
        radioBtns = popupView.findViewById(R.id.radioGroup);

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

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
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
        btnMinus.setEnabled(quantity > 0);
        btnPlus.setEnabled(quantity < 9999999);
    }
    public int getQuantity() {
        return quantity;
    }
    public void setQuantity(int quantity) {
        this.quantity = Math.max(1, Math.min(quantity, 99));
        updateQuantityViews();
    }
}
