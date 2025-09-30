package com.example.foodkeeper.Recipe.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import com.example.foodkeeper.R;
import com.example.foodkeeper.Recipe.Models.IngredientsResponse;

import java.util.List;



public class IngredientsAd extends RecyclerView.Adapter<IngredientsAdViholder> {

    Context context;
    List<IngredientsResponse> list;
    public IngredientsAd(Context context, List<IngredientsResponse> list) {
        this.context = context;
        this.list = list;
    }
    @NonNull
    @Override
    public IngredientsAdViholder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new IngredientsAdViholder(LayoutInflater.from(context).inflate(R.layout.list_ingredients,parent,false));
    }

    @Override
    public void onBindViewHolder(@NonNull IngredientsAdViholder holder, int position) {

        holder.textView_ingredient_name.setText(list.get(position).name);
        holder.recycler_ingredient_items.setHasFixedSize(true);
        holder.recycler_ingredient_items.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL,false));
        ingredientsAdapter IngreAdapter= new ingredientsAdapter(context,list.get(position).ingredientList);
        holder.recycler_ingredient_items.setAdapter(IngreAdapter);
    }

    @Override
    public int getItemCount() {
        return list.size();
    }
}
class IngredientsAdViholder extends RecyclerView.ViewHolder{

    TextView textView_ingredient_name;
    RecyclerView recycler_ingredient_items;
    public IngredientsAdViholder(@NonNull View itemView) {
        super(itemView);

        textView_ingredient_name=itemView.findViewById(R.id.textView_ingredient_name);
        recycler_ingredient_items=itemView.findViewById(R.id.recycler_ingredient_items);
    }
}
