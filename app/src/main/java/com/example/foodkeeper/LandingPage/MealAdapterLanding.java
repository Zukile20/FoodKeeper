package com.example.foodkeeper.LandingPage;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.foodkeeper.Meal.Meal;
import com.example.foodkeeper.R;

import java.util.List;

public class MealAdapterLanding extends RecyclerView.Adapter<MealAdapterLanding.ViewHolderLanding> {

    private List<Meal> meals;
    private Context context;
    public interface OnClickListener {
        void onClick(Meal meal, int position);
    }

    private OnClickListener onClickListener;


    public MealAdapterLanding(Context context ,List<Meal> meals, OnClickListener listener) {
        this.meals = meals;
        this.onClickListener = listener;
        this.context =context;
    }

    @NonNull
    @Override
    public ViewHolderLanding onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.landing_meal_view, parent, false);
        return new ViewHolderLanding(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolderLanding holder, int position) {
        Meal meal = meals.get(position);
        holder.bind(meal);
        holder.itemView.setOnClickListener(v->{
            onClickListener.onClick(meal,position);
        });
    }


    public Context getContext() {
        return context;
    }
    @Override
    public int getItemCount() {
        return meals.size();
    }

    @SuppressLint("ResourceAsColor")
    class ViewHolderLanding extends RecyclerView.ViewHolder {
        private ImageView mealImageView;
        private TextView mealNameText;
        private TextView mealType;


        public ViewHolderLanding(@NonNull View itemView) {
            super(itemView);

            mealImageView = itemView.findViewById(R.id.meal_view);
            mealNameText = itemView.findViewById(R.id.meal_name);
            mealType = itemView.findViewById(R.id.mealType);
        }
        public void loadBase64ImageWithGlide(Context context, String base64String, ImageView imageView) {
            if (base64String == null || base64String.isEmpty()) {
                imageView.setImageResource(R.drawable.image_placeholder);
                return;
            }

            try {
                byte[] decodedBytes = Base64.decode(base64String, Base64.DEFAULT);

                Glide.with(context)
                        .asBitmap()
                        .load(decodedBytes)
                        .placeholder(R.drawable.image_placeholder)
                        .error(R.drawable.image_placeholder)
                        .centerCrop()
                        .into(imageView);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
                imageView.setImageResource(R.drawable.image_placeholder);
            } catch (Exception e) {
                e.printStackTrace();
                imageView.setImageResource(R.drawable.image_placeholder);
            }
        }

        public void bind(Meal meal) {
            mealNameText.setText(meal.getMealName());
            mealType.setText(meal.getMealType());

         loadBase64ImageWithGlide(context,meal.getUri(),mealImageView);
        }
    }
}