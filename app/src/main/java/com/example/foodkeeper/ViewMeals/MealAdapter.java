package com.example.foodkeeper.ViewMeals;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Base64;
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
            editButton.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && onItemActionListener != null) {
                    swipeRevealLayout.close(); // Close the swipe after clicking
                    onItemActionListener.onItemEdit(meals.get(position), position);
                }
            });

            deleteButton.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && onItemActionListener != null) {
                    swipeRevealLayout.close(); // Close the swipe after clicking
                    onItemActionListener.onItemDelete(meals.get(position), position);
                }
            });

            mainContent.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && onItemActionListener != null) {
                    if (!swipeRevealLayout.isRevealed()) {
                        onItemActionListener.onItemClick(meals.get(position), position);
                    } else {
                        swipeRevealLayout.close();
                    }
                }
            });
        }

        public void bind(Meal meal) {
            mealNameText.setText(meal.getMealName());

            swipeRevealLayout.close();

            loadBase64ImageWithGlide(context,meal.getUri(),mealImageView);
        }
        public void loadBase64ImageWithGlide(Context context, String base64String, ImageView imageView) {
            if (base64String == null || base64String.isEmpty()) {
                imageView.setImageResource(R.drawable.image_placeholder);
                return;
            }

            try {
                byte[] decodedBytes = Base64.decode(base64String, Base64.DEFAULT);

                Glide.with(context)
                        .load(decodedBytes)
                        .placeholder(R.drawable.image_placeholder)
                        .error(R.drawable.image_placeholder)
                        .centerCrop()
                        .into(imageView);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
                imageView.setImageResource(R.drawable.image_placeholder);
            }
        }

    }
}
