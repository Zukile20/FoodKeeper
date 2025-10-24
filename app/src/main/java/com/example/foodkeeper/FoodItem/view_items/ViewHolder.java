package com.example.foodkeeper.FoodItem.view_items;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.foodkeeper.R;

public class ViewHolder extends RecyclerView.ViewHolder {
   public ImageView imageView;
    public TextView nameView, expiryDateView;
    public ViewHolder(@NonNull View itemView) {
        super(itemView);
        imageView = itemView.findViewById(R.id.itemImage);
        nameView = itemView.findViewById(R.id.itemName);
        expiryDateView = itemView.findViewById(R.id.itemExpiry);
    }
}
