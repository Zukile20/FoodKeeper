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

import com.example.foodkeeper.Database;
import com.example.foodkeeper.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;

public class MainShoppingListActivity extends AppCompatActivity {

    //main Activity
    RecyclerView recyclerView;
    FloatingActionButton add_button;
    private Button back_btn;
    private LinearLayout emptyStateLayout;  // Add this for empty state
    //dialog
    EditText Name;
    EditText QTY;
    Button add;
    Database myDB;
    ArrayList<String> itemId,itemName,itemQty;
    ArrayList<Integer> itemBought;
    ShoppingListAdapter customAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_shoppinglist);

        iniViews();
        setupListeners();
        storedDataInArrays();
        customAdapter= new ShoppingListAdapter(MainShoppingListActivity.this,itemName,itemQty,itemBought,itemId);
        recyclerView.setAdapter(customAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(MainShoppingListActivity.this));
        deleteItem();

        // Check empty state after setup
        updateEmptyState();
    }
    private void iniViews(){
        recyclerView= findViewById(R.id.recylerView);
        add_button=findViewById(R.id.add_button);
        back_btn=findViewById(R.id.back_btn);
        emptyStateLayout = findViewById(R.id.empty_state_layout);  // Initialize empty state view
    }
    private void setupListeners(){

        add_button.setOnClickListener(new View.OnClickListener() {   //clicking add button
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
        AlertDialog.Builder mBuilder= new AlertDialog.Builder(MainShoppingListActivity.this);
        View mView= getLayoutInflater().inflate(R.layout.add_dialog,null);
        Name= mView.findViewById(R.id.ItemName);
        QTY = mView.findViewById(R.id.ItemQty);
        add = mView.findViewById(R.id.Add);

        mBuilder.setView(mView);// dialog to pop up
        AlertDialog dialog = mBuilder.create();
        dialog.show();

        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String item_name = Name.getText().toString().trim();
                String itemQtyStr = QTY.getText().toString().trim();

                if (item_name.isEmpty() || itemQtyStr.isEmpty()) {
                    Toast.makeText(MainShoppingListActivity.this, "Please enter both name and quantity", Toast.LENGTH_SHORT).show();
                    return;
                }
                if(item_name.length()>50)
                {
                    Toast.makeText(MainShoppingListActivity.this, "The name is too long", Toast.LENGTH_SHORT).show();
                    return;
                }
                int item_qty = Integer.parseInt(QTY.getText().toString().trim());
                if (item_qty == 0) {
                    Toast.makeText(MainShoppingListActivity.this, "Quantity must be at least 1", Toast.LENGTH_SHORT).show();
                    return;
                }
                Database myDB = new Database(MainShoppingListActivity.this);

                long results = myDB.AddItem(item_name, item_qty);
                itemId.add(String.valueOf(results));
                itemName.add(item_name);
                itemQty.add(String.valueOf(item_qty));
                itemBought.add(0);
                customAdapter.updateList(itemName, itemQty,itemBought);

                // Update empty state after adding item
                updateEmptyState();

                dialog.dismiss();
            }
        });
    }
    private void storedDataInArrays(){
        myDB= new Database(MainShoppingListActivity.this);
        itemId= new ArrayList<>();
        itemName= new ArrayList<>();
        itemQty= new ArrayList<>();
        itemBought = new ArrayList<>();

        Cursor cursor = myDB.readAllData();
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
                        // Delete from database
                        myDB.deleteItemById(Integer.parseInt(idToDelete));

                        // Remove from local lists
                        itemId.remove(position);
                        itemName.remove(position);
                        itemQty.remove(position);
                        itemBought.remove(position);  // Don't forget to remove from itemBought too!

                        // Update adapter
                        customAdapter.notifyItemRemoved(position);

                        // Update empty state after deletion
                        updateEmptyState();
                    }

                    @Override
                    public void onDeleteCancelled() {
                        // Restore swiped item visually
                        customAdapter.notifyItemChanged(position);
                    }
                });

                // Show the dialog fragment
                deleteDialog.show(getSupportFragmentManager(), "DeleteDialog");
            }
        });
        helper.attachToRecyclerView(recyclerView);
    }
}