package com.example.foodkeeper.FoodItem;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;

import com.example.foodkeeper.Database;
import com.example.foodkeeper.DeleteDialog;

import com.example.foodkeeper.R;
import com.example.foodkeeper.SessionManager;
import com.google.android.material.imageview.ShapeableImageView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class ViewAnItemActivity extends AppCompatActivity implements DeleteDialog.OnDeleteConfirmListener {
    private static final int EXPIRING_SOON_DAYS = 3;
    private static final int EDIT_ITEM_REQUEST = 1;
    private ShapeableImageView itemImage;
    private TextView itemName, itemCategory, itemExpiry, itemQuantity;
    private Button btnEdit, btnDelete;
    private Database db;
    private Button goBack;
    private FoodItem currentItem;
    SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_view_an_item);

        db = new Database(this);
        session= new SessionManager(this);


        itemImage = findViewById(R.id.itemDetailImage);
        itemName = findViewById(R.id.itemDetailName);
        itemCategory = findViewById(R.id.itemDetailCategory);
        itemExpiry = findViewById(R.id.itemDetailExpiry);
        itemQuantity = findViewById(R.id.itemDetailQuantity);
        btnEdit = findViewById(R.id.btnEditItem);
        btnDelete = findViewById(R.id.btnDeleteItem);
        goBack = findViewById(R.id.btnGoBack);

        currentItem = (FoodItem) getIntent().getSerializableExtra("foodItem");

        if (currentItem != null) {
            displayItemDetails(currentItem);
        }

        goBack.setOnClickListener(v -> {
            startActivity(new Intent(this, ItemsViewActivity.class));
        });

        btnEdit.setOnClickListener(v -> {
            if (currentItem != null) {
                editItem();
            } else {
                Toast.makeText(this, "No item selected to edit", Toast.LENGTH_SHORT).show();
            }
        });
        btnDelete.setOnClickListener(v -> deleteItem());
    }

    private void deleteItem() {
        FragmentManager fm = getSupportFragmentManager();
        DeleteDialog deleteDialog = DeleteDialog.newInstance(currentItem.getName());
        deleteDialog.setOnDeleteConfirmListener(this);
        deleteDialog.show(fm, "delete_dialog");
    }

    @Override
    public void onDeleteConfirmed() {
        boolean deleted = db.deleteFoodItem(currentItem.getId(),session.getUserEmail());
        if (deleted) {
            Toast.makeText(this, "Item deleted", Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK);
            finish();
        } else {
            Toast.makeText(this, "Failed to delete item", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDeleteCancelled() {}

    private void editItem() {
        Intent intent = new Intent(ViewAnItemActivity.this, EditItemActivity.class);
        intent.putExtra("foodItem", currentItem);
        startActivityForResult(intent, EDIT_ITEM_REQUEST);
    }

    private void displayItemDetails(FoodItem currentItem) {
        itemName.setText(currentItem.getName());
        itemCategory.setText(currentItem.getCategory());
        itemExpiry.setText(currentItem.getExpiryDate());
        itemQuantity.setText(String.valueOf(currentItem.getQuantity()));

        if (currentItem.getImage() != null) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(currentItem.getImage(), 0, currentItem.getImage().length);
            itemImage.setImageBitmap(bitmap);
        }

        int status = getExpiryStatus(currentItem.getExpiryDate());
        switch (status) {
            case -1:
                itemExpiry.setTextColor(ContextCompat.getColor(this, R.color.red));
                break;
            case 0:
                itemExpiry.setTextColor(ContextCompat.getColor(this, R.color.orange));
                break;
            case 1:
                itemExpiry.setTextColor(ContextCompat.getColor(this, R.color.green));
                break;
        }
    }

    public int getExpiryStatus(String expiryDate) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Date expiry = sdf.parse(expiryDate);
            Date today = new Date();

            Calendar expiryCal = Calendar.getInstance();
            Calendar todayCal = Calendar.getInstance();
            expiryCal.setTime(expiry);
            todayCal.setTime(today);

            expiryCal.set(Calendar.HOUR_OF_DAY, 0);
            expiryCal.set(Calendar.MINUTE, 0);
            expiryCal.set(Calendar.SECOND, 0);
            expiryCal.set(Calendar.MILLISECOND, 0);

            todayCal.set(Calendar.HOUR_OF_DAY, 0);
            todayCal.set(Calendar.MINUTE, 0);
            todayCal.set(Calendar.SECOND, 0);
            todayCal.set(Calendar.MILLISECOND, 0);

            long diffInMillis = expiryCal.getTimeInMillis() - todayCal.getTimeInMillis();
            long diffInDays = TimeUnit.MILLISECONDS.toDays(diffInMillis);

            if (diffInDays < 0) {
                return -1;
            } else if (diffInDays <= EXPIRING_SOON_DAYS) {
                return 0;
            } else {
                return 1;
            }

        } catch (ParseException e) {
            e.printStackTrace();
            return 1;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == EDIT_ITEM_REQUEST && resultCode == RESULT_OK) {
            if (data != null && data.hasExtra("updatedItem")) {
                currentItem = (FoodItem) data.getSerializableExtra("updatedItem");
                displayItemDetails(currentItem);
                setResult(RESULT_OK);
            }
        }
    }
}