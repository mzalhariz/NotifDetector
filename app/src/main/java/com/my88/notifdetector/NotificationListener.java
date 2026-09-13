package com.my88.notifdetector;

import android.app.Notification;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class NotificationListener extends NotificationListenerService {

    private static final String TAG = "NotifDetector";
    public static final String ACTION_NOTIFICATION_RECEIVED = "com.my88.notifdetector.NOTIFICATION_RECEIVED";
    private MediaPlayer mediaPlayer;

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null || sbn.getNotification() == null) return;

        String packageName = sbn.getPackageName();

        // Skip our own notifications
        if (packageName.equals(getPackageName())) return;

        Notification notification = sbn.getNotification();
        Bundle extras = notification.extras;

        String title = "";
        String text = "";
        String appName = packageName;

        if (extras != null) {
            CharSequence titleCs = extras.getCharSequence(Notification.EXTRA_TITLE);
            CharSequence textCs = extras.getCharSequence(Notification.EXTRA_TEXT);
            if (titleCs != null) title = titleCs.toString();
            if (textCs != null) text = textCs.toString();
        }

        // Get app label
        try {
            appName = getPackageManager()
                    .getApplicationLabel(getPackageManager().getApplicationInfo(packageName, 0))
                    .toString();
        } catch (Exception e) {
            appName = packageName;
        }

        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

        Log.d(TAG, "Notification from: " + appName + " | " + title + " | " + text);

        // Check if alarm is enabled
        SharedPreferences prefs = getSharedPreferences("notif_detector_settings", MODE_PRIVATE);
        boolean alarmEnabled = prefs.getBoolean("alarm_enabled", true);

        if (alarmEnabled) {
            playAlarmSound();
            vibrate();
        }

        // Broadcast to MainActivity
        Intent intent = new Intent(ACTION_NOTIFICATION_RECEIVED);
        intent.putExtra("app_name", appName);
        intent.putExtra("package_name", packageName);
        intent.putExtra("title", title);
        intent.putExtra("text", text);
        intent.putExtra("timestamp", timestamp);
        sendBroadcast(intent);

        // Save to storage
        NotificationStorage.getInstance(this).addNotification(
                new NotificationItem(appName, packageName, title, text, timestamp)
        );
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        // Optional: handle notification dismissal
    }

    private void playAlarmSound() {
        try {
            // Stop any currently playing alarm
            stopAlarm();

            SharedPreferences prefs = getSharedPreferences("notif_detector_settings", MODE_PRIVATE);
            String uriStr = prefs.getString("alarm_uri", "");

            Uri alarmUri;
            if (!uriStr.isEmpty()) {
                // User selected custom alarm
                alarmUri = Uri.parse(uriStr);
            } else {
                // Default: police siren (system alarm sound)
                alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
                if (alarmUri == null) {
                    alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                }
            }

            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(this, alarmUri);
            mediaPlayer.setLooping(false);
            mediaPlayer.prepare();
            mediaPlayer.start();

            // Auto-stop after duration
            int duration = prefs.getInt("alarm_duration", 3) * 1000;
            new android.os.Handler(getMainLooper()).postDelayed(this::stopAlarm, duration);

        } catch (Exception e) {
            Log.e(TAG, "Error playing alarm sound", e);
        }
    }

    private void stopAlarm() {
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) mediaPlayer.stop();
                mediaPlayer.release();
            } catch (Exception e) {
                // ignore
            }
            mediaPlayer = null;
        }
    }

    private void vibrate() {
        try {
            Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                long[] pattern = {0, 300, 200, 300, 200, 300};
                vibrator.vibrate(android.os.VibrationEffect.createWaveform(pattern, -1));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error vibrating", e);
        }
    }
}
