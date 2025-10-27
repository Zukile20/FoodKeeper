package com.example.foodkeeper.Fridge;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import com.example.foodkeeper.FoodkeeperUtils.Database;
import com.example.foodkeeper.FoodkeeperUtils.DeleteDialog;
import com.example.foodkeeper.Fridge.models.Fridge;
import com.example.foodkeeper.R;
import com.example.foodkeeper.Register.SessionManager;

import java.util.ArrayList;

public class ViewFridgeDetails extends AppCompatActivity implements DeleteDialog.OnDeleteConfirmListener {

    private ImageView fridgeDetailImage;
    private TextView fridgeDetailName, fridgeDetailBrand, fridgeDetailModel;
    private TextView fridgeDetailSize, fridgeDetailDescription, fridgeDetailStatus;
    private Button btnEditFridge, btnDeleteFridge, btnGoBack, btnConnect;
    private Database db;
    private Fridge currentFridge;
    private int fridgeId;
    SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_fridge_details);

        db = new Database(this);
        session = new SessionManager(this);

        fridgeDetailImage = findViewById(R.id.fridgeDetailImage);
        fridgeDetailName = findViewById(R.id.fridgeDetailName);
        fridgeDetailBrand = findViewById(R.id.fridgeDetailBrand);
        fridgeDetailModel = findViewById(R.id.fridgeDetailModel);
        fridgeDetailSize = findViewById(R.id.fridgeDetailSize);
        fridgeDetailDescription = findViewById(R.id.fridgeDetailDescription);
        fridgeDetailStatus = findViewById(R.id.fridgeDetailStatus);
        btnEditFridge = findViewById(R.id.btnEditFridge);
        btnDeleteFridge = findViewById(R.id.btnDeleteFridge);
        btnGoBack = findViewById(R.id.btnGoBack);
        btnConnect = findViewById(R.id.btnConnectFridge);

        fridgeId = getIntent().getIntExtra("FRIDGE_ID", -1);

        if (fridgeId == -1) {
            Toast.makeText(this, "Fridge not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadFridgeDetails(fridgeId);

        btnGoBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        btnEditFridge.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ViewFridgeDetails.this, EditFridgeActivity.class);
                intent.putExtra("FRIDGE_ID", currentFridge.getId());
                startActivityForResult(intent, 1);
            }
        });
        btnDeleteFridge.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteFridge();
            }
        });
        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connectFridge();
            }
        });
    }

    private void connectFridge() {
        if (currentFridge != null) {
            boolean success = db.logIntoFridge(currentFridge.getId(),session.getUserEmail());

            if (success) {
                Toast.makeText(this, "Fridge connected successfully", Toast.LENGTH_SHORT).show();

                currentFridge.setLoggedIn(true);
                updateConnectionStatus();

                Intent resultIntent = new Intent();
                resultIntent.putExtra("CONNECTED_FRIDGE_ID", currentFridge.getId());
                setResult(RESULT_OK, resultIntent);
            } else {
                Toast.makeText(this, "Failed to connect to fridge", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void updateConnectionStatus() {
        if (currentFridge.isLoggedIn()) {
            fridgeDetailStatus.setText("Connected");
            fridgeDetailStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            btnConnect.setVisibility(View.GONE);
        } else {
            fridgeDetailStatus.setText("Disconnected");
            fridgeDetailStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            btnConnect.setVisibility(View.VISIBLE);
        }
    }
    private void deleteFridge() {
        ArrayList<Fridge> allFridges = new ArrayList<>(db.getUserFridges(session.getUserEmail()));

        if (allFridges.size() <= 1) {
            Toast.makeText(this, "Cannot delete the only fridge in the system", Toast.LENGTH_LONG).show();
            return;
        }

        FragmentManager fm = getSupportFragmentManager();
        DeleteDialog deleteDialog = DeleteDialog.newInstance(currentFridge.getBrand() + " " + currentFridge.getModel());
        deleteDialog.setOnDeleteConfirmListener(this);
        deleteDialog.show(fm, "delete_dialog");
    }

    @Override
    public void onDeleteConfirmed() {
        boolean wasConnected = currentFridge.isLoggedIn();

        boolean deleted = db.deleteFridge(currentFridge.getId(),session.getUserEmail());
        if (deleted) {
            Toast.makeText(this, "Fridge deleted", Toast.LENGTH_SHORT).show();

            Intent resultIntent = new Intent();
            resultIntent.putExtra("DELETED_FRIDGE_ID", currentFridge.getId());

            if (wasConnected) {
                ArrayList<Fridge> allFridges = new ArrayList<>(db.getUserFridges(session.getUserEmail()));
                if (!allFridges.isEmpty()) {
                    Fridge nextFridge = allFridges.get(0);
                    db.logIntoFridge(nextFridge.getId(),session.getUserEmail());

                    resultIntent.putExtra("CONNECTED_FRIDGE_ID", nextFridge.getId());
                }
            }

            setResult(RESULT_OK, resultIntent);
            finish();
        } else {
            Toast.makeText(this, "Failed to delete fridge", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDeleteCancelled() {}

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK) {
            if (data != null && data.hasExtra("UPDATED_FRIDGE_ID")) {
                int updatedFridgeId = data.getIntExtra("UPDATED_FRIDGE_ID", -1);
                loadFridgeDetails(updatedFridgeId);

                Intent resultIntent = new Intent();
                resultIntent.putExtra("UPDATED_FRIDGE_ID", updatedFridgeId);
                setResult(RESULT_OK, resultIntent);
            }

        }
    }
    private void loadFridgeDetails(int fridgeId) {
        currentFridge = db.getFridgeById(fridgeId,session.getUserEmail());

        if (currentFridge == null) {
            Toast.makeText(this, "Failed to load fridge details", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        fridgeDetailName.setText(currentFridge.getBrand() + " " + currentFridge.getModel());
        fridgeDetailBrand.setText(currentFridge.getBrand());
        fridgeDetailModel.setText(currentFridge.getModel());
        fridgeDetailSize.setText(currentFridge.getSize() + " L");
        fridgeDetailDescription.setText(currentFridge.getDescription());

        if (currentFridge.isLoggedIn()) {
            fridgeDetailStatus.setText("Connected");
            fridgeDetailStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        } else {
            fridgeDetailStatus.setText("Disconnected");
            fridgeDetailStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        }

        updateConnectionStatus();

        byte[] imageData = currentFridge.getImage();
        if (imageData != null && imageData.length > 0) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.length);
            fridgeDetailImage.setImageBitmap(bitmap);
        } else {
            fridgeDetailImage.setImageResource(R.drawable.image_placeholder);
        }
    }
}