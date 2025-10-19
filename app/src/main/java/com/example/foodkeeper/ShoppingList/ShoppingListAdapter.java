package com.example.foodkeeper.ShoppingList;

import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.foodkeeper.Database;
import com.example.foodkeeper.R;

import java.util.ArrayList;

public class ShoppingListAdapter extends RecyclerView.Adapter<ShoppingListAdapter.MyViewHolder> {
    private Context context;
    private ArrayList itemName, itemQty;
    private ArrayList<Integer> itemBought;
    private ArrayList<String> itemId;
    private Database myDB;
    private String userEmail; // Add userEmail field

    public ShoppingListAdapter(Context context, ArrayList<String> itemName, ArrayList<String> itemQty,
                               ArrayList<Integer> itemBought, ArrayList<String> itemId, String userEmail) {
        this.context = context;
        this.itemName = itemName;
        this.itemQty = itemQty;
        this.itemBought = itemBought;
        this.itemId = itemId;
        this.userEmail = userEmail; // Store userEmail
        this.myDB = new Database(context);
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.list_view, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        holder.item_Name.setText(itemName.get(position).toString());
        holder.item_Qty.setText(itemQty.get(position).toString());

        holder.checkboxBought.setOnCheckedChangeListener(null); // reset listener first

        boolean isChecked = itemBought.get(position) == 1;
        holder.checkboxBought.setChecked(isChecked);

        if (isChecked) {
            holder.item_Name.setPaintFlags(holder.item_Name.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            holder.item_Name.setPaintFlags(holder.item_Name.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
        }

        holder.checkboxBought.setOnCheckedChangeListener((buttonView, isChecked1) -> {
            itemBought.set(position, isChecked1 ? 1 : 0);
            if (isChecked1) {
                // Pass userEmail to UpdateItemShoppingList
                myDB.UpdateItemShoppingList(Integer.parseInt(itemId.get(position)), userEmail);
                holder.item_Name.setPaintFlags(holder.item_Name.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                Toast.makeText(context, "Don't forget to add items to your fridge!", Toast.LENGTH_LONG).show();
            } else {
                // Pass userEmail to UpdateItemBackShoppingList
                myDB.UpdateItemBackShoppingList(Integer.parseInt(itemId.get(position)), userEmail);
                holder.item_Name.setPaintFlags(holder.item_Name.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            }
        });
    }

    @Override
    public int getItemCount() {
        return itemName.size();
    }

    public void updateList(ArrayList names, ArrayList Qty_s, ArrayList<Integer> bought) {
        this.itemName = names;
        this.itemQty = Qty_s;
        this.itemBought = bought;
        notifyDataSetChanged();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        TextView item_Name, item_Qty;
        CheckBox checkboxBought;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            item_Name = itemView.findViewById(R.id.item_name);
            item_Qty = itemView.findViewById(R.id.item_qty);
            checkboxBought = itemView.findViewById(R.id.item_checkbox);
        }
    }
}