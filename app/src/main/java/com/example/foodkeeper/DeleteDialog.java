package com.example.foodkeeper;

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

public class DeleteDialog extends DialogFragment {
    private String itemName;
    private OnDeleteConfirmListener listener;
    public interface OnDeleteConfirmListener {
        void onDeleteConfirmed();
        void onDeleteCancelled();
    }
    public static DeleteDialog newInstance(String itemName) {
        DeleteDialog dialog = new DeleteDialog();
        Bundle args = new Bundle();
        args.putString("item_name", itemName);
        dialog.setArguments(args);
        return dialog;
    }
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            itemName = getArguments().getString("item_name");
        }
        setStyle(DialogFragment.STYLE_NO_TITLE, android.R.style.Theme_Translucent_NoTitleBar);
    }
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.delete_layout, container, false);
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        TextView titleText = view.findViewById(R.id.delete_title);
        TextView messageText = view.findViewById(R.id.delete_message);
        Button noButton = view.findViewById(R.id.btn_no);
        Button yesDeleteButton = view.findViewById(R.id.btn_yes_delete);
        View dialogContainer = view.findViewById(R.id.dialog_container);

        String title = "Are you sure you want to delete the " + itemName + "?";
        titleText.setText(title);

        noButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteCancelled();
            }
            dismissWithAnimation();
        });

        yesDeleteButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteConfirmed();
            }
            dismissWithAnimation();
        });
        dialogContainer.setOnClickListener(v -> {
        });
    }
    private void animateEntrance(View dialogContainer) {
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
    public void setOnDeleteConfirmListener(OnDeleteConfirmListener listener) {
        this.listener = listener;
    }
}