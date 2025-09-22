package com.example.foodkeeper;

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

public class NotificationHelper {
    private static final String LOW_CHANNEL_ID = "expiring_items_channel";
    private static final String CHANNEL_NAME = "Expiration Alerts";
    private static final String CHANNEL_DESCRIPTION = "Notifications for items about to expire";
    private static final int DEFAULT_IMAGE_RESOURCE = R.drawable.image_placeholder;

    public static void showExpiringItemNotification(Context context, FoodItem item) {
        NotificationManager notificationManager = createNotificationChannel(context);

        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, LOW_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Expiring Soon!")
                .setContentText(item.getName() + " is about to expire")
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_REMINDER);

        try {
            @SuppressLint("RemoteViewLayout")
            RemoteViews customView = new RemoteViews(context.getPackageName(), R.layout.notification);
            customView.setTextViewText(R.id.title, "Expiring Soon!");
            customView.setTextViewText(R.id.textMessage, item.getName() + " is about to expire");
            customView.setImageViewResource(R.id.noti_logo_view, R.drawable.logo_background);

            if (item.getImage() != null && item.getImage().length > 0) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(item.getImage(), 0, item.getImage().length);
                customView.setImageViewBitmap(R.id.foodItemImage, bitmap);
            } else {
                customView.setImageViewResource(R.id.foodItemImage, DEFAULT_IMAGE_RESOURCE);
            }

            builder.setCustomContentView(customView)
                    .setStyle(new NotificationCompat.DecoratedCustomViewStyle());
        } catch (Exception e) {
            e.printStackTrace();
        }

        int notificationId = item.getId() != 0 ? item.getId() : item.getName().hashCode();
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