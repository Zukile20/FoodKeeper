package com.example.foodkeeper.FoodkeeperUtils;

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
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.foodkeeper.R;

import java.util.Objects;

public class DeleteConfirmationDialog extends DialogFragment {
    private String itemName;
    private OnDeleteConfirmListener listener;
    private String message;
    private String delete_button_text;

    public interface OnDeleteConfirmListener {
        void onDeleteConfirmed();

        void onDeleteCancelled();
    }

    public static DeleteConfirmationDialog newInstance(String itemName,String message,String delete_button_text) {
        DeleteConfirmationDialog dialog = new DeleteConfirmationDialog();
        Bundle args = new Bundle();
        args.putString("item_name", itemName);
        args.putString("message", message);
        args.putString("delete_button_text", delete_button_text);
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            itemName = getArguments().getString("item_name");
            message = getArguments().getString("message");
            delete_button_text = getArguments().getString("delete_button_text");
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
        Button noButton = view.findViewById(R.id.btn_no);
        Button yesDeleteButton = view.findViewById(R.id.btn_yes_delete);
        View dialogContainer = view.findViewById(R.id.dialog_container);
        if (!Objects.equals(message, "")) {


            titleText.setText(message);
            yesDeleteButton.setText(delete_button_text);
        } else {
            String title = "Are you sure you want to delete the " + itemName + "?";
            titleText.setText(title);
        }
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
                window.setDimAmount(0.6f); // Dimming background
            }
        }
    }

    public void setOnDeleteConfirmListener(OnDeleteConfirmListener listener) {
        this.listener = listener;
    }
}