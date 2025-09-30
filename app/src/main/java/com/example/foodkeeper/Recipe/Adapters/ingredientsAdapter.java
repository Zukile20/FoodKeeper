package com.example.foodkeeper.Recipe.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;


import com.example.foodkeeper.R;
import com.example.foodkeeper.Recipe.Models.ExtendedIngredient;

import java.util.List;

public class ingredientsAdapter extends RecyclerView.Adapter<IngredientsViewHolder> {

    Context context;
    List<ExtendedIngredient> list;

    public ingredientsAdapter(Context context, List<ExtendedIngredient> list) {
        this.context = context;
        this.list = list;
    }

    @NonNull
    @Override
    public IngredientsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new IngredientsViewHolder(LayoutInflater.from(context).inflate(R.layout.list_meal_ingredients,parent,false));
    }

    @Override
    public void onBindViewHolder(@NonNull IngredientsViewHolder holder, int position) {
       holder.textView_ingredient_item.setText(list.get(position).name);
       //holder.textView_ingredient_item.setSelected(true);
    }

    @Override
    public int getItemCount() {
        return list.size();
    }
}
class IngredientsViewHolder extends RecyclerView.ViewHolder{
    TextView textView_ingredient_item;
    ImageView imageView_Icon;
    public IngredientsViewHolder(@NonNull View itemView) {
        super(itemView);
        textView_ingredient_item= itemView.findViewById(R.id.textView_ingredient_item);
        imageView_Icon= itemView.findViewById(R.id.imageView_Icon);
    }
}
