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
    private RecyclerView recyclerView;

    public SwipeRevealCallback(MealAdapter adapter) {
        super(0, ItemTouchHelper.RIGHT | ItemTouchHelper.LEFT);
        this.adapter = adapter;
        this.context = adapter.getContext();

        editIcon = ContextCompat.getDrawable(context, R.drawable.ic_edit);
        deleteIcon = ContextCompat.getDrawable(context, R.drawable.trash);

        buttonWidth = dpToPx(80);
        iconSize = dpToPx(24);

        editColor = ContextCompat.getColor(context, R.color.blue);
        deleteColor = ContextCompat.getColor(context, R.color.red);

        buttonPaint = new Paint();
        buttonPaint.setAntiAlias(true);
    }

    @Override
    public int getMovementFlags(@NonNull RecyclerView recyclerView,
                                @NonNull RecyclerView.ViewHolder viewHolder) {
        if (this.recyclerView == null) {
            this.recyclerView = recyclerView;
        }

        if (viewHolder.getAdapterPosition() == RecyclerView.NO_POSITION) {
            return 0;
        }

        int position = viewHolder.getAdapterPosition();
        int swipeFlags;

        if (buttonsRevealed && position == currentSwipedPosition) {
            // Item with revealed buttons: allow both right (stay open) and left (close)
            swipeFlags = ItemTouchHelper.RIGHT | ItemTouchHelper.LEFT;
        } else if (buttonsRevealed && position != currentSwipedPosition) {
            // Different item while another is open: don't allow any swipe
            return makeMovementFlags(0, 0);
        } else {
            // No buttons revealed: only allow right swipe
            swipeFlags = ItemTouchHelper.RIGHT;
        }

        return makeMovementFlags(0, swipeFlags);
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView,
                          @NonNull RecyclerView.ViewHolder viewHolder,
                          @NonNull RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        int position = viewHolder.getAdapterPosition();

        // If swiping left and buttons are revealed on this item, close them
        if (direction == ItemTouchHelper.LEFT && buttonsRevealed && position == currentSwipedPosition) {
            viewHolder.itemView.animate()
                    .translationX(0)
                    .setDuration(200)
                    .withEndAction(() -> {
                        buttonsRevealed = false;
                        currentSwipedPosition = -1;
                        adapter.notifyItemChanged(position);
                    })
                    .start();
            return;
        }

        // If swiping right, reveal buttons
        if (direction == ItemTouchHelper.RIGHT) {
            // Close any previously opened item
            if (buttonsRevealed && currentSwipedPosition != position) {
                RecyclerView.ViewHolder previousViewHolder =
                        recyclerView.findViewHolderForAdapterPosition(currentSwipedPosition);
                if (previousViewHolder != null) {
                    previousViewHolder.itemView.animate()
                            .translationX(0)
                            .setDuration(200)
                            .start();
                }
            }

            currentSwipedPosition = position;
            buttonsRevealed = true;

            View itemView = viewHolder.itemView;
            float maxSwipeDistance = buttonWidth * 2;
            itemView.setTranslationX(maxSwipeDistance);
        }
    }

    @Override
    public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                            @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY,
                            int actionState, boolean isCurrentlyActive) {

        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
            View itemView = viewHolder.itemView;
            int itemHeight = itemView.getHeight();
            int position = viewHolder.getAdapterPosition();

            float maxSwipeDistance = buttonWidth * 2;
            float currentTranslation = itemView.getTranslationX();

            // Handle swipe when buttons are already revealed
            if (buttonsRevealed && position == currentSwipedPosition && currentTranslation > 0) {
                if (dX < 0) {
                    // Swiping left to close - allow smooth closing
                    float newTranslation = Math.max(0, currentTranslation + dX);
                    dX = newTranslation - currentTranslation;
                    itemView.setTranslationX(newTranslation);
                } else {
                    // Trying to swipe more right - keep at max
                    dX = 0;
                    itemView.setTranslationX(maxSwipeDistance);
                }
            } else {
                // Normal right swipe to reveal
                if (dX > maxSwipeDistance) {
                    dX = maxSwipeDistance;
                } else if (dX < 0) {
                    dX = 0;
                }
            }

            // Draw buttons if swiped to the right
            if (itemView.getTranslationX() > 0 || dX > 0) {
                float drawDistance = Math.max(itemView.getTranslationX(), dX);
                if (drawDistance > 0) {
                    drawButtons(c, itemView, drawDistance, itemHeight);
                }
            }
        }

        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
    }

    private void drawButtons(Canvas c, View itemView, float dX, int itemHeight) {
        // Draw delete button (left button)
        buttonPaint.setColor(deleteColor);
        float deleteLeft = itemView.getLeft();
        float deleteRight = itemView.getLeft() + buttonWidth;
        c.drawRect(deleteLeft, itemView.getTop(), deleteRight, itemView.getBottom(), buttonPaint);

        drawIcon(c, deleteIcon, deleteLeft, itemView.getTop(), buttonWidth, itemHeight);

        // Draw edit button (right button)
        buttonPaint.setColor(editColor);
        float editLeft = itemView.getLeft() + buttonWidth;
        float editRight = itemView.getLeft() + buttonWidth * 2;
        c.drawRect(editLeft, itemView.getTop(), editRight, itemView.getBottom(), buttonPaint);

        drawIcon(c, editIcon, editLeft, itemView.getTop(), buttonWidth, itemHeight);
    }

    private void drawIcon(Canvas c, Drawable icon, float left, float top,
                          float buttonWidth, float itemHeight) {
        if (icon == null) return;

        int iconLeft = (int) (left + (buttonWidth - iconSize) / 2);
        int iconTop = (int) (top + (itemHeight - iconSize) / 2);
        int iconRight = iconLeft + iconSize;
        int iconBottom = iconTop + iconSize;

        icon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
        icon.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
        icon.draw(c);
    }

    @Override
    public void clearView(@NonNull RecyclerView recyclerView,
                          @NonNull RecyclerView.ViewHolder viewHolder) {
        super.clearView(recyclerView, viewHolder);

        // Only reset if buttons are not revealed or if this is a different item
        if (!buttonsRevealed || viewHolder.getAdapterPosition() != currentSwipedPosition) {
            viewHolder.itemView.setTranslationX(0);
        }
    }

    @Override
    public float getSwipeThreshold(@NonNull RecyclerView.ViewHolder viewHolder) {
        return 0.3f;
    }

    @Override
    public float getSwipeEscapeVelocity(float defaultValue) {
        return defaultValue * 0.1f;
    }

    @Override
    public float getSwipeVelocityThreshold(float defaultValue) {
        return defaultValue * 5f;
    }

    public boolean handleButtonClick(RecyclerView recyclerView, float x, float y) {
        if (!buttonsRevealed || currentSwipedPosition == -1) return false;

        RecyclerView.ViewHolder viewHolder = recyclerView.findViewHolderForAdapterPosition(currentSwipedPosition);
        if (viewHolder != null) {
            View itemView = viewHolder.itemView;

            if (x >= itemView.getLeft() && x <= itemView.getRight() &&
                    y >= itemView.getTop() && y <= itemView.getBottom()) {

                float buttonAreaRight = itemView.getLeft() + (buttonWidth * 2);

                if (x <= buttonAreaRight) {
                    // Delete button clicked
                    if (x >= itemView.getLeft() && x <= itemView.getLeft() + buttonWidth) {
                        adapter.deleteItem(currentSwipedPosition);
                        resetSwipeState(viewHolder);
                        return true;
                    }

                    // Edit button clicked
                    if (x >= itemView.getLeft() + buttonWidth && x <= buttonAreaRight) {
                        adapter.editItem(currentSwipedPosition);
                        resetSwipeState(viewHolder);
                        return true;
                    }
                } else {
                    // Clicked outside button area - close the swipe
                    resetSwipeState(viewHolder);
                    return true;
                }
            }
        }
        return false;
    }

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

    public void closeOpenSwipe(RecyclerView recyclerView) {
        if (buttonsRevealed && currentSwipedPosition != -1) {
            RecyclerView.ViewHolder viewHolder = recyclerView.findViewHolderForAdapterPosition(currentSwipedPosition);
            if (viewHolder != null) {
                resetSwipeState(viewHolder);
            }
        }
    }

    public void closeOpenSwipe() {
        if (recyclerView != null) {
            closeOpenSwipe(recyclerView);
        }
    }

    private int dpToPx(int dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density);
    }
}