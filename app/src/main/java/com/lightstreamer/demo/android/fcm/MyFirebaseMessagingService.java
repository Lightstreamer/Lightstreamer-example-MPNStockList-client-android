/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.lightstreamer.demo.android.fcm;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;


import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import androidx.core.app.NotificationCompat;
import android.util.Log;

import java.util.Map;

import static com.lightstreamer.demo.android.fcm.Utils.TAG;

/**
 * This service does the actual handling of the Firebase message.
 */
public class MyFirebaseMessagingService extends FirebaseMessagingService {

    public static final String CH_ID = "lightstreamer_ch1";
    public static final int NOTIFICATION_ID = 1;

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Map<String, String> extras = remoteMessage.getData();
        if (extras != null) {
            // Extract message data
            String stockName = extras.get("stock_name");
            String lastPrice = extras.get("last_price");
            String time = extras.get("time");

            String message = "Stock " + stockName + " is " + lastPrice + " at " + time;

            int itemNum = 0;
            String item = extras.get("item");
            if (item != null) {
                item = item.substring(4);
                try {
                    itemNum = Integer.valueOf(item);
                } catch(NumberFormatException nfe) {
                    // not what I expected
                }
            }

            Log.i(TAG, "Received message: " + message);

            // Post notification of received message.
            Bundle bundle = new Bundle();
            bundle.putString("stock_name", stockName);
            bundle.putString("last_price", lastPrice);
            bundle.putString("time", time);
            bundle.putInt("itemNum", itemNum);

            sendNotification(message, bundle);
        }
    }

    // Put the message into a notification and post it.
    // This is just one simple example of what you might choose to do with a message
    private void sendNotification(String msg, Bundle extras) {
        NotificationManager mNotificationManager = (NotificationManager)
                this.getSystemService(Context.NOTIFICATION_SERVICE);

        Intent intent = new Intent(this, StockListDemo.class);
        intent.putExtras(extras);
        
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification notification;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CH_ID, "lightstreamer_ch", importance);
            channel.setDescription("Demo notifications");
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);

            notification = new Notification.Builder(this, CH_ID)
                            .setSmallIcon(R.drawable.ic_stat_gcm)
                            .setContentTitle("Stock Notification")
                            .setStyle(new Notification.BigTextStyle().bigText(msg))
                            .setContentText(msg)
                            .setAutoCancel(true)
                            .setContentIntent(contentIntent)
                            .build();
        } else {
            notification = new NotificationCompat.Builder(this, CH_ID)
                            .setSmallIcon(R.drawable.ic_stat_gcm)
                            .setContentTitle("Stock Notification")
                            .setStyle(new NotificationCompat.BigTextStyle().bigText(msg))
                            .setContentText(msg)
                            .setAutoCancel(true)
                            .setContentIntent(contentIntent)
                            .build();
        }
        mNotificationManager.notify(NOTIFICATION_ID, notification);
    }
}
