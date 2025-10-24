package com.example.foodkeeper.Fridge;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.foodkeeper.FoodkeeperUtils.Database;
import com.example.foodkeeper.R;
import com.example.foodkeeper.Register.SessionManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;

public class ManageFridgeActivity extends AppCompatActivity {
    private RecyclerView fridgeRecyclerView;
    private FloatingActionButton fabAddItem;
    private ArrayList<Fridge> fridgeList;
    private FridgeAdapter adapter;
    private ImageButton backBtn;
    private Database db;
    private SessionManager session;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_manage_fridge);

        db = new Database(this);
        session = new SessionManager(this);
        fridgeRecyclerView = findViewById(R.id.fridgeRecyclerView);
        fabAddItem = findViewById(R.id.fabAddItem);
        fridgeList = new ArrayList<>(db.getUserFridges(session.getUserEmail()));
        adapter = new FridgeAdapter(fridgeList, this);
        fridgeRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        fridgeRecyclerView.setAdapter(adapter);

        refreshList();

        fabAddItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ManageFridgeActivity.this, AddFridgeActivity.class);
                startActivityForResult(intent, 1);
            }
        });

        adapter.setOnItemClickListener(new FridgeAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                Fridge clickedFridge = fridgeList.get(position);

                Intent intent = new Intent(ManageFridgeActivity.this, ViewFridgeDetails.class);
                intent.putExtra("FRIDGE_ID", clickedFridge.getId());
                startActivityForResult(intent, 2);
            }
        });
        backBtn = findViewById(R.id.backFridgeBtn);
        backBtn.setOnClickListener(v -> finish());
    }

    private void refreshList() {
        fridgeList = new ArrayList<>(db.getUserFridges(session.getUserEmail()));
        if(adapter == null){
            adapter = new FridgeAdapter(fridgeList, this);
            fridgeRecyclerView.setLayoutManager(new LinearLayoutManager(this));
            fridgeRecyclerView.setAdapter(adapter);
        } else{
            adapter.updateFridgeList(fridgeList);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1 && resultCode == RESULT_OK && data != null) {
            String brand = data.getStringExtra("brand");
            String model = data.getStringExtra("model");
            String description = data.getStringExtra("description");
            int size = data.getIntExtra("size", 0);
            byte[] imageData = data.getByteArrayExtra("image");

            Fridge newFridge = new Fridge(brand, model, description, size, false, imageData);
            long result = db.addFridge(newFridge,session.getUserEmail());
            if(result != -1){
                newFridge.setId((int)result);
                fridgeList.add(newFridge);
                adapter.notifyItemInserted(fridgeList.size() - 1);
            }
        }
        else if (requestCode == 2 && resultCode == RESULT_OK && data != null) {
            if (data.hasExtra("DELETED_FRIDGE_ID")) {
                int deletedFridgeId = data.getIntExtra("DELETED_FRIDGE_ID", -1);
                if (deletedFridgeId != -1) {
                    for (int i = 0; i < fridgeList.size(); i++) {
                        if (fridgeList.get(i).getId() == deletedFridgeId) {
                            fridgeList.remove(i);
                            adapter.notifyItemRemoved(i);
                            break;
                        }
                    }
                }
                refreshList();
            }
            else if (data.hasExtra("UPDATED_FRIDGE_ID")) {
                int updatedFridgeId = data.getIntExtra("UPDATED_FRIDGE_ID", -1);
                if (updatedFridgeId != -1) {
                    refreshList();
                }
            }
            else if (data.hasExtra("CONNECTED_FRIDGE_ID")) {
                int connectedFridgeId = data.getIntExtra("CONNECTED_FRIDGE_ID", -1);
                if (connectedFridgeId != -1) {
                    refreshList();
                }
            }
        }
    }
    @Override
    protected void onDestroy(){
        super.onDestroy();
        if(db != null)
            db.close();
    }
}