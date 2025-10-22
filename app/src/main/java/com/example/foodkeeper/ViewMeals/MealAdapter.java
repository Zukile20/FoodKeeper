package com.example.foodkeeper.ViewMeals;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.foodkeeper.FoodkeeperUtils.SwipeRevealLayout;
import com.example.foodkeeper.Meal.Meal;
import com.example.foodkeeper.R;

import java.util.List;

public class MealAdapter extends RecyclerView.Adapter<MealAdapter.MealViewHolder> {
    private List<Meal> meals;
    private OnMealActionListener listener;
    private SwipeRevealLayout currentlyOpenLayout = null;

    public interface OnMealActionListener {
        void onEdit(Meal meal, int position);
        void onDelete(Meal meal, int position);
        void onItemClick(Meal meal, int position);
    }

    public MealAdapter(List<Meal> meals, OnMealActionListener listener) {
        this.meals = meals;
        this.listener = listener;
    }

    @NonNull
    @Override
    public MealViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.swipeable_item, parent, false);
        return new MealViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MealViewHolder holder, int position) {
        Meal meal = meals.get(position);
        holder.bind(meal, position);
    }

    @Override
    public int getItemCount() {
        return meals.size();
    }

    public void removeMeal(int position) {
        meals.remove(position);
        notifyItemRemoved(position);
    }

    public void closeAllItems() {
        if (currentlyOpenLayout != null) {
            currentlyOpenLayout.close();
            currentlyOpenLayout = null;
        }
    }

    class MealViewHolder extends RecyclerView.ViewHolder {
        private SwipeRevealLayout swipeLayout;
        private ImageView mealImageView;
        private TextView mealNameText;
        private LinearLayout editButton;
        private LinearLayout deleteButton;
        private Context context;

        public MealViewHolder(@NonNull View itemView) {
            super(itemView);
            swipeLayout = itemView.findViewById(R.id.swipe_container);
            mealImageView = itemView.findViewById(R.id.mealImageView);
            mealNameText = itemView.findViewById(R.id.mealNameText);
            editButton = itemView.findViewById(R.id.edit_button);
            deleteButton = itemView.findViewById(R.id.delete_button);
            context =itemView.getRootView().getContext();
        }

        public void bind(Meal meal, int position) {
            mealNameText.setText(meal.getMealName());
            Glide.with(context)
                    .load(meal.getUri())
                    .placeholder(R.drawable.image_placeholder)
                    .error(R.drawable.place_holder)
                    .into(mealImageView);

            // Close this item if another is opened
            swipeLayout.setOnTouchListener((v, event) -> {
                if (currentlyOpenLayout != null && currentlyOpenLayout != swipeLayout) {
                    currentlyOpenLayout.close();
                }
                currentlyOpenLayout = swipeLayout;
                return false;
            });

            // Edit button click
            editButton.setOnClickListener(v -> {
                swipeLayout.close();
                if (listener != null) {
                    listener.onEdit(meal, position);
                }
            });

            // Delete button click
            deleteButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDelete(meal, position);
                }
            });

            // Main content click
            itemView.findViewById(R.id.main_content_card).setOnClickListener(v -> {
                if (!swipeLayout.isRevealed() && listener != null) {
                    listener.onItemClick(meal, position);
                }
            });
        }
    }
}