package com.example.foodkeeper.FoodkeeperUtils;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.widget.RemoteViews;

import androidx.core.app.NotificationCompat;

import com.example.foodkeeper.FoodItem.FoodItem;
import com.example.foodkeeper.LandingPageActivity;
import com.example.foodkeeper.R;

public class NotificationHelper {
    private static final String LOW_CHANNEL_ID = "low_stock_channel";
    private static final String CHANNEL_NAME = "Low Stock Alerts";
    private static final String CHANNEL_DESCRIPTION = "Notifications for items running low on stock";

    public static void showLowStockNotification(Context context, FoodItem item) {
        NotificationManager notificationManager = createNotificationChannel(context);

        Intent intent = new Intent(context, LandingPageActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        // Custom layout
        @SuppressLint("RemoteViewLayout")
        RemoteViews customView = new RemoteViews(context.getPackageName(), R.layout.notification);
        customView.setTextViewText(R.id.title, "Low Stock!");
        customView.setTextViewText(R.id.textMessage,
                item.getName() + " is running low");
        customView.setImageViewResource(R.id.noti_logo_view, R.mipmap.app_logo);


        byte[] image =item.getImage();
        Bitmap bitmap = BitmapFactory.decodeByteArray(image, 0, image.length);
        customView.setImageViewBitmap(R.id.foodItemImage,bitmap);

        // Notification builder
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, LOW_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setCustomContentView(customView)
                .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_REMINDER);

        int notificationId = item.getId()!= 0 ? item.getId() : item.getName().hashCode();
        notificationManager.notify(notificationId, builder.build());
    }

    private static NotificationManager createNotificationChannel(Context context) {
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    LOW_CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
            );

            channel.setDescription(CHANNEL_DESCRIPTION);
            channel.enableVibration(true);
            channel.setShowBadge(true);

            notificationManager.createNotificationChannel(channel);
        }

        return notificationManager;
    }


}