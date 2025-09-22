package com.example.foodkeeper.FoodkeeperUtils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.example.foodkeeper.R;
import com.example.foodkeeper.ViewMeals.MealAdapter;

public class SwipeRevealCallback extends ItemTouchHelper.SimpleCallback {

    private MealAdapter adapter;
    private Drawable editIcon;
    private Drawable deleteIcon;
    private int buttonWidth;
    private int iconSize;
    private Paint buttonPaint;
    private int editColor;
    private int deleteColor;
    private Context context;
    private int currentSwipedPosition = -1;
    private boolean buttonsRevealed = false;

    public SwipeRevealCallback(MealAdapter adapter) {
        super(0, ItemTouchHelper.RIGHT);
        this.adapter = adapter;
        this.context = adapter.getContext();

        // Initialize icons
        editIcon = ContextCompat.getDrawable(context, R.drawable.ic_edit);
        deleteIcon = ContextCompat.getDrawable(context, R.drawable.trash);

        // Dimensions
        buttonWidth = dpToPx(80);
        iconSize = dpToPx(24);

        // Colors
        editColor = ContextCompat.getColor(context, R.color.blue);
        deleteColor = ContextCompat.getColor(context, R.color.grey);

        // Paint for buttons
        buttonPaint = new Paint();
        buttonPaint.setAntiAlias(true);
    }

    @Override
    public int getMovementFlags(@NonNull RecyclerView recyclerView,
                                @NonNull RecyclerView.ViewHolder viewHolder) {
        if (viewHolder.getAdapterPosition() == RecyclerView.NO_POSITION) {
            return 0;
        }
        return makeMovementFlags(0, ItemTouchHelper.RIGHT);
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView,
                          @NonNull RecyclerView.ViewHolder viewHolder,
                          @NonNull RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        // Mark this position as swiped and keep buttons revealed
        currentSwipedPosition = viewHolder.getAdapterPosition();
        buttonsRevealed = true;

        // Keep the item in swiped position by setting translation (positive for right swipe)
        View itemView = viewHolder.itemView;
        float maxSwipeDistance = buttonWidth * 2;
        itemView.setTranslationX(maxSwipeDistance);
    }

    @Override
    public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                            @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY,
                            int actionState, boolean isCurrentlyActive) {

        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
            View itemView = viewHolder.itemView;
            int itemHeight = itemView.getHeight();

            // Limit swipe distance to show both buttons
            float maxSwipeDistance = buttonWidth * 2;
            if (Math.abs(dX) > maxSwipeDistance) {
                dX = dX > 0 ? maxSwipeDistance : -maxSwipeDistance;
            }

            // Draw buttons when swiping right
            if (dX > 0) {
                drawButtons(c, itemView, dX, itemHeight);
            }
        }

        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
    }

    private void drawButtons(Canvas c, View itemView, float dX, int itemHeight) {
        // Draw Delete button (left-most when swiped right)
        buttonPaint.setColor(deleteColor);
        float deleteLeft = itemView.getLeft();
        float deleteRight = itemView.getLeft() + buttonWidth;
        c.drawRect(deleteLeft, itemView.getTop(), deleteRight, itemView.getBottom(), buttonPaint);

        // Draw delete icon
        drawIcon(c, deleteIcon, deleteLeft, itemView.getTop(), buttonWidth, itemHeight);

        // Draw Edit button (right of delete button when swiped right)
        buttonPaint.setColor(editColor);
        float editLeft = itemView.getLeft() + buttonWidth;
        float editRight = itemView.getLeft() + buttonWidth * 2;
        c.drawRect(editLeft, itemView.getTop(), editRight, itemView.getBottom(), buttonPaint);

        // Draw edit icon
        drawIcon(c, editIcon, editLeft, itemView.getTop(), buttonWidth, itemHeight);
    }

    private void drawIcon(Canvas c, Drawable icon, float left, float top,
                          float buttonWidth, float itemHeight) {
        if (icon == null) return;

        // Calculate icon position to center it in the button
        int iconLeft = (int) (left + (buttonWidth - iconSize) / 2);
        int iconTop = (int) (top + (itemHeight - iconSize) / 2);
        int iconRight = iconLeft + iconSize;
        int iconBottom = iconTop + iconSize;

        // Set icon bounds and draw
        icon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
        icon.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
        icon.draw(c);
    }

    @Override
    public void clearView(@NonNull RecyclerView recyclerView,
                          @NonNull RecyclerView.ViewHolder viewHolder) {
        super.clearView(recyclerView, viewHolder);

        // DON'T reset translation if buttons should stay revealed
        if (!buttonsRevealed || viewHolder.getAdapterPosition() != currentSwipedPosition) {
            viewHolder.itemView.setTranslationX(0);
        }
    }

    @Override
    public float getSwipeThreshold(@NonNull RecyclerView.ViewHolder viewHolder) {
        return 0.3f; // Lower threshold for easier reveal
    }

    @Override
    public float getSwipeEscapeVelocity(float defaultValue) {
        return defaultValue * 0.1f; // Much slower escape velocity
    }

    @Override
    public float getSwipeVelocityThreshold(float defaultValue) {
        return defaultValue * 5f; // Higher velocity threshold
    }

    // Handle button clicks
    public boolean handleButtonClick(RecyclerView recyclerView, float x, float y) {
        if (!buttonsRevealed || currentSwipedPosition == -1) return false;

        RecyclerView.ViewHolder viewHolder = recyclerView.findViewHolderForAdapterPosition(currentSwipedPosition);
        if (viewHolder != null) {
            View itemView = viewHolder.itemView;

            // Check if click is within the item bounds
            if (x >= itemView.getLeft() && x <= itemView.getRight() &&
                    y >= itemView.getTop() && y <= itemView.getBottom()) {

                // Calculate button positions (buttons are revealed on the left side)
                float buttonAreaRight = itemView.getLeft() + (buttonWidth * 2);

                if (x <= buttonAreaRight) {
                    // Delete button (left button)
                    if (x >= itemView.getLeft() && x <= itemView.getLeft() + buttonWidth) {
                        adapter.deleteItem(currentSwipedPosition);
                        resetSwipeState(viewHolder);
                        return true;
                    }

                    // Edit button (right of delete button)
                    if (x >= itemView.getLeft() + buttonWidth && x <= buttonAreaRight) {
                        adapter.editItem(currentSwipedPosition);
                        resetSwipeState(viewHolder);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // Reset swipe state and close the revealed buttons
    public void resetSwipeState(RecyclerView.ViewHolder viewHolder) {
        if (viewHolder != null) {
            viewHolder.itemView.animate()
                    .translationX(0)
                    .setDuration(200)
                    .withEndAction(() -> {
                        buttonsRevealed = false;
                        currentSwipedPosition = -1;
                    })
                    .start();
        }
    }

    // Close any open swipe
    public void closeOpenSwipe(RecyclerView recyclerView) {
        if (buttonsRevealed && currentSwipedPosition != -1) {
            RecyclerView.ViewHolder viewHolder = recyclerView.findViewHolderForAdapterPosition(currentSwipedPosition);
            resetSwipeState(viewHolder);
        }
    }

    // Check if any item is currently swiped open

    private int dpToPx(int dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density);
    }
}