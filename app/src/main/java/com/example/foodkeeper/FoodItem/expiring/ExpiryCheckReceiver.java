package com.example.foodkeeper.FoodItem.expiring;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.example.foodkeeper.FoodItem.models.FoodItem;
import com.example.foodkeeper.FoodkeeperUtils.Database;
import com.example.foodkeeper.FoodkeeperUtils.NotificationHelper;
import com.example.foodkeeper.Register.SessionManager;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class ExpiryCheckReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Database db = new Database(context);
        SessionManager session = new SessionManager(context);
        List<FoodItem> items = db.getFoodItemsInConnectedFridge(session.getUserEmail());

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date today = cal.getTime();

        cal.add(Calendar.DAY_OF_YEAR, 3);
        Date threshold = cal.getTime();

        SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

        for (FoodItem item : items) {
            try {
                Date expiryDate = format.parse(item.getExpiryDate().trim());
                if (expiryDate != null && !expiryDate.before(today) && !expiryDate.after(threshold)) {
                    NotificationHelper.showExpiringItemNotification(context, item);
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        db.close();
    }
}
