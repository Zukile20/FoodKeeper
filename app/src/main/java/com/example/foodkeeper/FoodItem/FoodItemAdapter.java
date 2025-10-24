package com.example.foodkeeper.FoodItem;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.foodkeeper.FoodItem.models.FoodItem;
import com.example.foodkeeper.FoodItem.view_items.ViewHolder;
import com.example.foodkeeper.R;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class FoodItemAdapter extends RecyclerView.Adapter<ViewHolder> {
    private OnItemClickListener onItemClickListener;
    private static final int EXPIRING_SOON_DAYS = 3;
    private Context context;
    private List<FoodItem> foodItems;

    public interface OnItemClickListener{
        void onItemClick(FoodItem foodItem);
    }
    public FoodItemAdapter(Context context, List<FoodItem> foodItems) {
        this.context = context;
        this.foodItems = foodItems;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(context).inflate(R.layout.grid_item, parent, false));
    }
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FoodItem item = foodItems.get(position);

        holder.itemView.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(foodItems.get(position));
            }
        });

        holder.nameView.setText(item.getName());
        holder.expiryDateView.setText(item.getExpiryDate());

        byte[] imageData = item.getImage();
        if (imageData != null && imageData.length > 0) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.length);
            holder.imageView.setImageBitmap(bitmap);
        } else {
            holder.imageView.setImageResource(R.drawable.image_placeholder);
        }

        int expiryStatus = getExpiryStatus(item.getExpiryDate());

        switch (expiryStatus) {
            case -1:
                holder.expiryDateView.setTextColor(ContextCompat.getColor(context, R.color.red));
                holder.expiryDateView.setText("Expired: " + item.getExpiryDate());
                break;
            case 0:
                holder.expiryDateView.setTextColor(ContextCompat.getColor(context, R.color.orange));
                holder.expiryDateView.setText("Expires soon: " + item.getExpiryDate());
                break;
            case 1:
                holder.expiryDateView.setTextColor(ContextCompat.getColor(context, R.color.green));
                holder.expiryDateView.setText("Expires: " + item.getExpiryDate());
                break;
        }

    }
    @Override
    public long getItemId(int position) {
        return position;
    }
    @Override
    public int getItemCount() {
        return foodItems.size();
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
    public void updateData(List<FoodItem> newData) {
        foodItems.clear();
        foodItems.addAll(newData);
        notifyDataSetChanged();
    }
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }
}