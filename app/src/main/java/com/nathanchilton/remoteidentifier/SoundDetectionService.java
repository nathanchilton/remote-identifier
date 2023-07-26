package com.nathanchilton.remoteidentifier;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class SoundDetectionService extends Service {

    private static final int NOTIFICATION_ID = 1;
    private static final String NOTIFICATION_CHANNEL_ID = "SoundDetectionChannel";

    private PowerManager.WakeLock wakeLock;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // Obtain a WakeLock to keep the CPU running
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (powerManager != null) {
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    "SoundDetectionService:WakeLockTag");
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIFICATION_ID, createNotification());
        acquireWakeLock();
        // Start recording audio here
        // Add the audio recording logic here

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopForeground(true);
        releaseWakeLock();
        // Stop recording audio here
        // Add the audio recording stop logic here
    }

    private void acquireWakeLock() {
        if (wakeLock != null && !wakeLock.isHeld()) {
            wakeLock.acquire();
        }
    }

    private void releaseWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
    }

    private Notification createNotification() {
        // Create the notification channel if API level is >= 26
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID,
                    "Sound Detection Service",
                    NotificationManager.IMPORTANCE_LOW);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }

        // Create the notification
        Intent notificationIntent = new Intent(this, MainActivity.class);

        PendingIntent pendingIntent;
        int requestCode = 42;

        // Create the PendingIntent using the appropriate flag based on the API level
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Use FLAG_IMMUTABLE for Android S and above
            pendingIntent = PendingIntent.getForegroundService(this, requestCode, notificationIntent, PendingIntent.FLAG_IMMUTABLE);
        } else {
            // For older versions, use the default flag
            pendingIntent = PendingIntent.getService(this, requestCode, notificationIntent, 0);
        }

        return new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setContentTitle("Sound Detection Service")
                .setContentText("Recording audio...")
                .setSmallIcon(R.drawable.ic_notification_icon)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW) // Change priority based on your needs
                .build();
    }
}
