package com.example.foodkeeper.FoodkeeperUtils;

import android.animation.ValueAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import com.example.foodkeeper.R;
import androidx.annotation.Nullable;
import androidx.core.view.GestureDetectorCompat;

public class SwipeRevealLayout extends FrameLayout {
    private static final int ANIMATION_DURATION = 300;
    private static final float REVEAL_THRESHOLD = 0.5f;

    private View mainContentView;
    private View backgroundView;
    private GestureDetectorCompat gestureDetector;
    private float currentOffset = 0f;
    private float maxOffset = 0f;
    private boolean isRevealed = false;
    private ValueAnimator animator;

    public SwipeRevealLayout(@NonNull Context context) {
        super(context);
        init();
    }

    public SwipeRevealLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SwipeRevealLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        gestureDetector = new GestureDetectorCompat(getContext(), new SwipeGestureListener());
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        // Find views
        backgroundView = findViewById(R.id.background_layout);
        mainContentView = findViewById(R.id.main_content_card);

        if (backgroundView != null) {
            backgroundView.setVisibility(GONE);  // Hide by default
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        if (backgroundView != null && maxOffset == 0f) {
            backgroundView.measure(
                    MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
                    MeasureSpec.makeMeasureSpec(getHeight(), MeasureSpec.EXACTLY)
            );
            maxOffset = backgroundView.getMeasuredWidth();
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return gestureDetector.onTouchEvent(ev) || super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean result = gestureDetector.onTouchEvent(event);

        if (event.getAction() == MotionEvent.ACTION_UP ||
                event.getAction() == MotionEvent.ACTION_CANCEL) {
            handleTouchUp();
        }

        return result || super.onTouchEvent(event);
    }

    private void handleTouchUp() {
        // Check if we've swiped past the threshold
        if (Math.abs(currentOffset) > maxOffset * REVEAL_THRESHOLD) {
            animateToRevealed();
        } else {
            animateToClosed();
        }
    }

    private void animateToRevealed() {
        animateOffset(currentOffset, maxOffset);  // Positive for right swipe
        isRevealed = true;
        if (backgroundView != null) {
            backgroundView.setVisibility(VISIBLE);
        }
    }

    private void animateToClosed() {
        animateOffset(currentOffset, 0f);
        isRevealed = false;
    }

    private void animateOffset(float from, float to) {
        if (animator != null) {
            animator.cancel();
        }

        animator = ValueAnimator.ofFloat(from, to);
        animator.setDuration(ANIMATION_DURATION);
        animator.addUpdateListener(animation -> {
            currentOffset = (float) animation.getAnimatedValue();
            updateViewPosition();
        });

        // Hide background when animation ends if closing
        if (to == 0f) {
            animator.addListener(new android.animation.AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(android.animation.Animator animation) {
                    if (backgroundView != null && currentOffset == 0f) {
                        backgroundView.setVisibility(GONE);
                    }
                }
            });
        }

        animator.start();
    }

    private void updateViewPosition() {
        if (mainContentView != null) {
            mainContentView.setTranslationX(currentOffset);
        }
    }

    public void close() {
        if (isRevealed) {
            animateToClosed();
        }
    }

    public boolean isRevealed() {
        return isRevealed;
    }

    private class SwipeGestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDown(@NonNull MotionEvent e) {
            return true;
        }

        @Override
        public boolean onScroll(@NonNull MotionEvent e1, @NonNull MotionEvent e2,
                                float distanceX, float distanceY) {
            // Show background when starting to swipe right
            if (backgroundView != null && backgroundView.getVisibility() != VISIBLE && distanceX < 0) {
                backgroundView.setVisibility(VISIBLE);
            }

            // Swipe right (negative distanceX) = positive offset (moves content right)
            // Only allow swiping right (revealing actions on the left)
            float newOffset = currentOffset - distanceX;

            // Constrain: 0 (closed) to maxOffset (fully open to the right)
            currentOffset = Math.max(0, Math.min(maxOffset, newOffset));

            updateViewPosition();
            return true;
        }

        @Override
        public boolean onFling(@NonNull MotionEvent e1, @NonNull MotionEvent e2,
                               float velocityX, float velocityY) {
            // velocityX is positive when flinging right, negative when flinging left
            if (velocityX > 1000) {  // Fast right fling
                animateToRevealed();
                return true;
            } else if (velocityX < -1000) {  // Fast left fling
                animateToClosed();
                return true;
            }
            return false;
        }
    }
}