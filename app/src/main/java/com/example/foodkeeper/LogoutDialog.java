package com.example.foodkeeper;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class LogoutDialog extends Dialog {

    private OnLogoutListener onLogoutListener;
    private Button btnNo;
    private Button btnYes;
    private TextView titleTextView;

    public interface OnLogoutListener {
        void onLogoutConfirmed();
        void onCancel();
    }

    public LogoutDialog(Context context, OnLogoutListener listener) {
        super(context);
        this.onLogoutListener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.logout_layout);

        btnNo = findViewById(R.id.btn_no);
        btnYes = findViewById(R.id.btn_yes_delete);
        titleTextView = findViewById(R.id.delete_title);

        btnNo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onLogoutListener != null) {
                    onLogoutListener.onCancel();
                }
                dismiss();
            }
        });

        btnYes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onLogoutListener != null) {
                    onLogoutListener.onLogoutConfirmed();
                }
                dismiss();
            }
        });

        setCancelable(false);
        setCanceledOnTouchOutside(false);
    }
}