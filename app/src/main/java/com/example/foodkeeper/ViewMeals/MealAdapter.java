package com.example.foodkeeper.ViewMeals;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.foodkeeper.FoodkeeperUtils.DeleteConfirmationDialog;
import com.example.foodkeeper.Meal.Meal;
import com.example.foodkeeper.R;

import java.util.List;

public class MealAdapter extends RecyclerView.Adapter<MealAdapter.MealViewHolder> {

    private List<Meal> meals;
    private Context context;
    public interface OnItemActionListener {
        void onItemEdit(Meal meal, int position);
        void onItemDelete(Meal meal, int position);
        void onItemClick(Meal meal,int position);
    }

    private OnItemActionListener onItemActionListener;



    public MealAdapter(Context context ,List<Meal> meals, OnItemActionListener listener) {
        this.meals = meals;
        this.onItemActionListener = listener;
        this.context =context;
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

    public void updateMeals(List<Meal> newMeals)
    {
        this.meals = newMeals;
        notifyDataSetChanged();
    }
    public void deleteItem(int position) {
        if (position >= 0 && position < meals.size()) {
            showDeleteConfirmation( position);
            onItemActionListener.onItemDelete(meals.get(position),position);
        }
    }
    public void editItem(int position) {
        if (position >= 0 && position < meals.size() && onItemActionListener != null) {
            onItemActionListener.onItemEdit(meals.get(position), position);
        }
        notifyItemChanged(position);
    }
    public void showDeleteConfirmation(int position) {
        if (position >= 0 && position < meals.size()) {

            DeleteConfirmationDialog dialog = DeleteConfirmationDialog.newInstance("meal");

            dialog.setOnDeleteConfirmListener(new DeleteConfirmationDialog.OnDeleteConfirmListener() {
                @Override
                public void onDeleteConfirmed() {
                    meals.remove(position);
                    notifyItemRemoved(position);
                    notifyItemRangeChanged(position, meals.size());

                    if (context instanceof AppCompatActivity) {
                        Toast.makeText(context,"Meal deleted", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onDeleteCancelled() {
                    notifyItemChanged(position);
                }
            });

            if (context instanceof AppCompatActivity) {
                dialog.show(((AppCompatActivity) context).getSupportFragmentManager(), "delete_dialog");
            }
        }

    }
    @SuppressLint("ResourceAsColor")
    class MealViewHolder extends RecyclerView.ViewHolder {
        private ImageView mealImageView;
        private TextView mealNameText;
        private View mainContent;
        private ImageButton editButton;
        private ImageButton deleteButton;

        private boolean isSwipeOpen = false;


        public MealViewHolder(@NonNull View itemView) {
            super(itemView);

            // Initialize views
            mealImageView = itemView.findViewById(R.id.mealImageView);
            mealNameText = itemView.findViewById(R.id.mealNameText);
            mainContent = itemView.findViewById(R.id.main_content_card);
            editButton = itemView.findViewById(R.id.edit_btn);
            deleteButton = itemView.findViewById(R.id.delete_btn);

            setupClickListeners();
        }

        private void setupClickListeners() {
            // Main content click
            mainContent.setOnClickListener(v -> {
                if (!isSwipeOpen) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION && onItemActionListener != null) {
                        onItemActionListener.onItemClick(meals.get(position),position);
                    }
                } else {
                    // Close swipe if open
                    isSwipeOpen=false;
                }
            });

            editButton.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && onItemActionListener != null) {
                    onItemActionListener.onItemEdit(meals.get(position), position);
                }
            });

            deleteButton.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && onItemActionListener != null) {
                    onItemActionListener.onItemDelete(meals.get(position), position);
                }
            });
        }

        public void bind(Meal meal) {
            mealNameText.setText(meal.getMealName());

            mainContent.setTranslationX(0);
            isSwipeOpen = false;

            Glide.with(itemView.getContext())
                    .load(meal.getUri())
                    .placeholder(R.drawable.image_placeholder)
                    .error(R.drawable.place_holder)
                    .into(mealImageView);
        }
    }
}

