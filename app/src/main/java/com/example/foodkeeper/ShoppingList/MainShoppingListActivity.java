package com.example.foodkeeper.ShoppingList;

import android.app.AlertDialog;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.foodkeeper.FoodkeeperUtils.Database;
import com.example.foodkeeper.R;
import com.example.foodkeeper.Register.SessionManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;

public class MainShoppingListActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    FloatingActionButton add_button;
    private Button back_btn;
    private LinearLayout emptyStateLayout;

    EditText Name;
    EditText QTY;
    Button add,btnPlus,btnMinus;
    Database myDB;
    ArrayList<String> itemId,itemName,itemQty;
    ArrayList<Integer> itemBought;
    ShoppingListAdapter customAdapter;

    private SessionManager sessionManager;
    private String userEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_shoppinglist);

        sessionManager = new SessionManager(this);
        userEmail = sessionManager.getUserEmail();


        iniViews();
        setupListeners();


        storedDataInArrays();
        customAdapter = new ShoppingListAdapter(MainShoppingListActivity.this, itemName, itemQty, itemBought, itemId, userEmail);
        recyclerView.setAdapter(customAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(MainShoppingListActivity.this));
        deleteItem();

        updateEmptyState();
        int mode= getIntent().getIntExtra("notificationAdd",0);
        if(mode==1)
        {
            String itemName = getIntent().getStringExtra("item_name");
            showAddItemDialog(itemName);
        }
    }

    private void iniViews(){
        recyclerView = findViewById(R.id.recylerView);
        add_button = findViewById(R.id.add_button);
        back_btn = findViewById(R.id.back_btn);
        emptyStateLayout = findViewById(R.id.empty_state_layout);
    }

    private void setupListeners(){
        add_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddItemDialog();
            }
        });

        back_btn.setOnClickListener(v -> {
            finish();
        });
    }

    private void showAddItemDialog() {
        showAddItemDialog(null);
    }

    private void showAddItemDialog(String prefilledItemName) {
        AlertDialog.Builder mBuilder = new AlertDialog.Builder(MainShoppingListActivity.this);
        View mView = getLayoutInflater().inflate(R.layout.add_dialog, null);
        Name = mView.findViewById(R.id.ItemName);
        QTY = mView.findViewById(R.id.ItemQty);
        add = mView.findViewById(R.id.Add);
        btnMinus = mView.findViewById(R.id.btn_minus);
        btnPlus = mView.findViewById(R.id.btn_plus);

        if (prefilledItemName != null && !prefilledItemName.isEmpty()) {
            Name.setText(prefilledItemName);
            QTY.requestFocus();
        }

        mBuilder.setView(mView);
        AlertDialog dialog = mBuilder.create();
        dialog.show();

        btnPlus.setOnClickListener(v -> incrementQuantity());
        btnMinus.setOnClickListener(v -> decrementQuantity());

        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String item_name = Name.getText().toString().trim();
                String itemQtyStr = QTY.getText().toString().trim();

                if (item_name.isEmpty() || itemQtyStr.isEmpty()) {
                    Toast.makeText(MainShoppingListActivity.this, "Please enter both name and quantity", Toast.LENGTH_SHORT).show();
                    return;
                }

                if(item_name.length() > 50) {
                    Toast.makeText(MainShoppingListActivity.this, "The name is too long", Toast.LENGTH_SHORT).show();
                    return;
                }

                int item_qty = Integer.parseInt(QTY.getText().toString().trim());
                if (item_qty == 0) {
                    Toast.makeText(MainShoppingListActivity.this, "Quantity must be at least 1", Toast.LENGTH_SHORT).show();
                    return;
                }

                Database myDB = new Database(MainShoppingListActivity.this);

                long results = myDB.AddItem(item_name, item_qty, userEmail);
                itemId.add(String.valueOf(results));
                itemName.add(item_name);
                itemQty.add(String.valueOf(item_qty));
                itemBought.add(0);
                customAdapter.updateList(itemName, itemQty, itemBought);

                updateEmptyState();

                dialog.dismiss();
            }
        });
    }

    private void storedDataInArrays(){
        myDB = new Database(MainShoppingListActivity.this);
        itemId = new ArrayList<>();
        itemName = new ArrayList<>();
        itemQty = new ArrayList<>();
        itemBought = new ArrayList<>();

        Cursor cursor = myDB.readAllData(userEmail);
        while(cursor.moveToNext()){
            itemId.add(cursor.getString(0));
            itemName.add(cursor.getString(1));
            itemQty.add(cursor.getString(2));
            itemBought.add(cursor.getInt(3));
        }
    }

    private void updateEmptyState() {
        if (itemName == null || itemName.isEmpty()) {
            emptyStateLayout.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyStateLayout.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void deleteItem() {
        ItemTouchHelper helper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();

                String idToDelete = itemId.get(position);
                String nameToDelete = itemName.get(position);

                DeleteDialog deleteDialog = DeleteDialog.newInstance(nameToDelete);
                deleteDialog.setOnDeleteConfirmListener(new DeleteDialog.OnDeleteConfirmListener() {
                    @Override
                    public void onDeleteConfirmed() {
                        myDB.deleteItemById(Integer.parseInt(idToDelete), userEmail);

                        itemId.remove(position);
                        itemName.remove(position);
                        itemQty.remove(position);
                        itemBought.remove(position);

                        customAdapter.notifyItemRemoved(position);

                        updateEmptyState();
                    }

                    @Override
                    public void onDeleteCancelled() {
                        customAdapter.notifyItemChanged(position);
                    }
                });

                deleteDialog.show(getSupportFragmentManager(), "DeleteDialog");
            }
        });
        helper.attachToRecyclerView(recyclerView);
    }

    private void decrementQuantity() {
        String qtyText = QTY.getText().toString().trim();
        int currentQuantity = 0;
        if (!qtyText.isEmpty()) {
            try {
                currentQuantity = Integer.parseInt(qtyText);
            } catch (NumberFormatException e) {
                currentQuantity = 0;
            }
        }

        if (currentQuantity > 0) {
            currentQuantity--;
            QTY.setText(String.valueOf(currentQuantity));
        } else {
            Toast.makeText(this, "Quantity cannot be less than 0", Toast.LENGTH_SHORT).show();
        }
    }

    private void incrementQuantity() {
        String qtyText = QTY.getText().toString().trim();
        int currentQuantity = 0;
        if (!qtyText.isEmpty()) {
            try {
                currentQuantity = Integer.parseInt(qtyText);
            } catch (NumberFormatException e) {
                currentQuantity = 0;
            }
        }
        if (currentQuantity < 999) {
            currentQuantity++;
            QTY.setText(String.valueOf(currentQuantity));
        } else {
            Toast.makeText(this, "Maximum quantity reached", Toast.LENGTH_SHORT).show();
        }
    }
}