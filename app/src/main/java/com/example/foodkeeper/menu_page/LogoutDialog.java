package com.example.foodkeeper.menu_page;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.foodkeeper.R;

public class LogoutDialog extends DialogFragment {

    private OnLogoutListener listener;

    public interface OnLogoutListener {
        void onLogoutConfirmed();
        void onCancel();
    }

    public static LogoutDialog newInstance() {
        return new LogoutDialog();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, android.R.style.Theme_Translucent_NoTitleBar);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.logout_layout, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView titleTextView = view.findViewById(R.id.delete_title);
        Button btnNo = view.findViewById(R.id.btn_no);
        Button btnYes = view.findViewById(R.id.btn_yes_delete);
        View dialogContainer = view.findViewById(R.id.dialog_container);

        titleTextView.setText("Are you sure you want to logout?");

        btnNo.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCancel();
            }
            dismissWithAnimation();
        });

        btnYes.setOnClickListener(v -> {
            if (listener != null) {
                listener.onLogoutConfirmed();
            }
            dismissWithAnimation();
        });

        dialogContainer.setOnClickListener(v -> {
        });

        animateEntrance(dialogContainer);
    }

    private void animateEntrance(View dialogContainer) {
        // Fade in background
        getView().setAlpha(0f);
        getView().animate()
                .alpha(1f)
                .setDuration(200)
                .start();

        dialogContainer.setAlpha(0f);
        dialogContainer.setScaleX(0.8f);
        dialogContainer.setScaleY(0.8f);

        dialogContainer.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(300)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    private void dismissWithAnimation() {
        View dialogContainer = getView().findViewById(R.id.dialog_container);

        getView().animate()
                .alpha(0f)
                .setDuration(200)
                .start();

        dialogContainer.animate()
                .alpha(0f)
                .scaleX(0.9f)
                .scaleY(0.9f)
                .setDuration(200)
                .setInterpolator(new AccelerateInterpolator())
                .withEndAction(() -> dismiss())
                .start();
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();

        if (dialog != null) {
            int width = ViewGroup.LayoutParams.MATCH_PARENT;
            int height = ViewGroup.LayoutParams.MATCH_PARENT;

            Window window = dialog.getWindow();
            if (window != null) {
                window.setLayout(width, height);
                window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                window.setFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND,
                        WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                window.setDimAmount(0.6f);
            }
        }
    }

    public void setOnLogoutListener(OnLogoutListener listener) {
        this.listener = listener;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        listener = null;
    }
}