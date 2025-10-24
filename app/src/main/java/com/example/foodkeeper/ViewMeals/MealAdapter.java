package com.example.foodkeeper.ViewMeals;

import android.annotation.SuppressLint;
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
    private Context context;

    public interface OnMealActionListener {
        void onItemEdit(Meal meal, int position);
        void onItemDelete(Meal meal, int position);
        void onItemClick(Meal meal, int position);
    }

    private OnMealActionListener onItemActionListener;

    public MealAdapter(Context context, List<Meal> meals, OnMealActionListener listener) {
        this.meals = meals;
        this.onItemActionListener = listener;
        this.context = context;
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
        holder.bind(meal);
    }

    public Context getContext() {
        return context;
    }

    @Override
    public int getItemCount() {
        return meals.size();
    }

    public void updateMeals(List<Meal> newMeals) {
        this.meals = newMeals;
        notifyDataSetChanged();
    }

    // Close all open swipe buttons
    public void closeSwipeButtons() {
        notifyDataSetChanged(); // This will rebind all items and close them
    }

    @SuppressLint("ResourceAsColor")
    public class MealViewHolder extends RecyclerView.ViewHolder {
        private ImageView mealImageView;
        private TextView mealNameText;
        private View mainContent;
        private SwipeRevealLayout swipeRevealLayout;
        private LinearLayout editButton;
        private LinearLayout deleteButton;

        public MealViewHolder(@NonNull View itemView) {
            super(itemView);

            // Initialize views
            swipeRevealLayout = (SwipeRevealLayout) itemView;
            mealImageView = itemView.findViewById(R.id.mealImageView);
            mealNameText = itemView.findViewById(R.id.mealNameText);
            mainContent = itemView.findViewById(R.id.main_content_card);
            editButton = itemView.findViewById(R.id.edit_button);
            deleteButton = itemView.findViewById(R.id.delete_button);

            setupClickListeners();
        }

        private void setupClickListeners() {
            // Edit button click
            editButton.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && onItemActionListener != null) {
                    swipeRevealLayout.close(); // Close the swipe after clicking
                    onItemActionListener.onItemEdit(meals.get(position), position);
                }
            });

            // Delete button click
            deleteButton.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && onItemActionListener != null) {
                    swipeRevealLayout.close(); // Close the swipe after clicking
                    onItemActionListener.onItemDelete(meals.get(position), position);
                }
            });

            // Main content click (when not swiped)
            mainContent.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && onItemActionListener != null) {
                    // Only trigger click if not revealed
                    if (!swipeRevealLayout.isRevealed()) {
                        onItemActionListener.onItemClick(meals.get(position), position);
                    } else {
                        // If revealed, just close it
                        swipeRevealLayout.close();
                    }
                }
            });
        }

        public void bind(Meal meal) {
            mealNameText.setText(meal.getMealName());

            // Close swipe when rebinding (important for recycling)
            swipeRevealLayout.close();

            // Load image with Glide
            Glide.with(itemView.getContext())
                    .load(meal.getUri())
                    .placeholder(R.drawable.image_placeholder)
                    .error(R.drawable.place_holder)
                    .into(mealImageView);
        }

        public SwipeRevealLayout getSwipeRevealLayout() {
            return swipeRevealLayout;
        }
    }
}
