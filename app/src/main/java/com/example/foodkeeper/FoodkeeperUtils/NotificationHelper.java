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

import com.example.foodkeeper.FoodItem.models.FoodItem;
import com.example.foodkeeper.LandingPage.LandingPageActivity;
import com.example.foodkeeper.R;

public class NotificationHelper {
    // Channel IDs
    private static final String LOW_STOCK_CHANNEL_ID = "low_stock_channel";
    private static final String EXPIRING_CHANNEL_ID = "expiring_items_channel";

    // Channel Names
    private static final String LOW_STOCK_CHANNEL_NAME = "Low Stock Alerts";
    private static final String EXPIRING_CHANNEL_NAME = "Expiration Alerts";

    // Channel Descriptions
    private static final String LOW_STOCK_CHANNEL_DESC = "Notifications for items running low on stock";
    private static final String EXPIRING_CHANNEL_DESC = "Notifications for items about to expire";

    // Default image
    private static final int DEFAULT_IMAGE_RESOURCE = R.drawable.image_placeholder;

    public static void showLowStockNotification(Context context, FoodItem item) {
        NotificationManager notificationManager = createNotificationChannels(context);

        // Create intent with extra to trigger the dialog
        Intent intent = new Intent(context, LandingPageActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("show_shopping_dialog", true);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                generateNotificationId(item, "LOW_STOCK"), // Use unique request code
                intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, LOW_STOCK_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Low Stock!")
                .setContentText(item.getName() + " is running low")
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_REMINDER);

        addCustomView(context, builder, item, "Low Stock!",
                item.getName() + " is running low");

        int notificationId = generateNotificationId(item, "LOW_STOCK");
        notificationManager.notify(notificationId, builder.build());
    }
    public static void showExpiringItemNotification(Context context, FoodItem item) {
        NotificationManager notificationManager = createNotificationChannels(context);

        NotificationCompat.Builder builder = createBaseNotification(
                context,
                EXPIRING_CHANNEL_ID,
                "Expiring Soon!",
                item.getName() + " is about to expire"
        );

        addCustomView(context, builder, item, "Expiring Soon!",
                item.getName() + " is about to expire");

        int notificationId = generateNotificationId(item, "EXPIRING");
        notificationManager.notify(notificationId, builder.build());
    }
    private static NotificationCompat.Builder createBaseNotification(
            Context context, String channelId, String title, String message) {

        Intent intent = new Intent(context, LandingPageActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        return new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(message)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_REMINDER);
    }
    @SuppressLint("RemoteViewLayout")
    private static void addCustomView(Context context, NotificationCompat.Builder builder,
                                      FoodItem item, String title, String message) {
        try {
            RemoteViews customView = new RemoteViews(context.getPackageName(), R.layout.notification);
            customView.setTextViewText(R.id.title, title);
            customView.setTextViewText(R.id.textMessage, message);
            customView.setImageViewResource(R.id.noti_logo_view, R.mipmap.app_logo);

            // Set food item image
            if (item.getImage() != null && item.getImage().length > 0) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(
                        item.getImage(), 0, item.getImage().length);
                customView.setImageViewBitmap(R.id.foodItemImage, bitmap);
            } else {
                customView.setImageViewResource(R.id.foodItemImage, DEFAULT_IMAGE_RESOURCE);
            }

            builder.setCustomContentView(customView)
                    .setStyle(new NotificationCompat.DecoratedCustomViewStyle());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static NotificationManager createNotificationChannels(Context context) {
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Low Stock Channel
            NotificationChannel lowStockChannel = new NotificationChannel(
                    LOW_STOCK_CHANNEL_ID,
                    LOW_STOCK_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            lowStockChannel.setDescription(LOW_STOCK_CHANNEL_DESC);
            lowStockChannel.enableVibration(true);
            lowStockChannel.setShowBadge(true);

            // Expiring Items Channel
            NotificationChannel expiringChannel = new NotificationChannel(
                    EXPIRING_CHANNEL_ID,
                    EXPIRING_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            expiringChannel.setDescription(EXPIRING_CHANNEL_DESC);
            expiringChannel.enableVibration(true);
            expiringChannel.setShowBadge(true);

            notificationManager.createNotificationChannel(lowStockChannel);
            notificationManager.createNotificationChannel(expiringChannel);
        }

        return notificationManager;
    }


    private static int generateNotificationId(FoodItem item, String type) {
        int baseId = item.getId() != 0 ? item.getId() : item.getName().hashCode();
        int offset = type.equals("LOW_STOCK") ? 100000 : 200000;
        return baseId + offset;
    }
}