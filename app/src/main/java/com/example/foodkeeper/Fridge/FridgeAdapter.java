package com.example.foodkeeper.Fridge;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.foodkeeper.R;

import java.util.ArrayList;
import java.util.List;

public class FridgeAdapter extends RecyclerView.Adapter<FridgeAdapter.FridgeViewHolder> {

    private List<Fridge> fridgeList;
    private Context context;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public static class FridgeViewHolder extends RecyclerView.ViewHolder {
        public ImageView fridgeImageView;
        public TextView fridgeNameTextView;
        public TextView statusTextView;

        public FridgeViewHolder(View itemView, final OnItemClickListener listener) {
            super(itemView);
            fridgeImageView = itemView.findViewById(R.id.fridgeImageView);
            fridgeNameTextView = itemView.findViewById(R.id.fridgeNameTextView);
            statusTextView = itemView.findViewById(R.id.statusTextView);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            listener.onItemClick(position);
                        }
                    }
                }
            });
        }
    }
    public FridgeAdapter(List<Fridge> fridgeList, Context context) {
        this.fridgeList = fridgeList;
        this.context = context;
    }
    @NonNull
    @Override
    public FridgeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fridge_item, parent, false);
        return new FridgeViewHolder(view, listener);
    }
    @Override
    public void onBindViewHolder(@NonNull FridgeViewHolder holder, int position) {
        Fridge fridge = fridgeList.get(position);

        holder.fridgeNameTextView.setText(fridge.getBrand() + " " + fridge.getModel());

        byte[] imageData = fridge.getImage();
        if (imageData != null && imageData.length > 0) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.length);
            holder.fridgeImageView.setImageBitmap(bitmap);
        } else {
            holder.fridgeImageView.setImageResource(R.drawable.image_placeholder);
        }

        if (fridge.isLoggedIn()) {
            holder.statusTextView.setText("Logged In");
            holder.statusTextView.setBackgroundColor(Color.parseColor("#E8F5E9"));
            holder.statusTextView.setTextColor(Color.parseColor("#2E7D32"));
        } else {
            holder.statusTextView.setText("Logged Off");
            holder.statusTextView.setBackgroundColor(Color.parseColor("#FFEBEE"));
            holder.statusTextView.setTextColor(Color.parseColor("#C62828"));
        }
    }

    @Override
    public int getItemCount() {
        return fridgeList.size();
    }
    public void updateFridgeList(ArrayList<Fridge> list){
        this.fridgeList = list;
        notifyDataSetChanged();
    }
}