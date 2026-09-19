package com.my88.notifdetector;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.Vibrator;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class NotificationListener extends NotificationListenerService {

    private static final String TAG = "NotifDetector";
    private static final String CHANNEL_ID = "notif_detector_service";
    private static final int FOREGROUND_ID = 1001;
    public static final String ACTION_NOTIFICATION_RECEIVED = "com.my88.notifdetector.NOTIFICATION_RECEIVED";
    private MediaPlayer mediaPlayer;
    private PowerManager.WakeLock wakeLock;
    private AudioManager audioManager;
    private AudioFocusRequest audioFocusRequest;

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        startForegroundService();
        acquireWakeLock();
        Log.d(TAG, "NotificationListener connected and running in foreground");
    }

    @Override
    public void onListenerDisconnected() {
        super.onListenerDisconnected();
        releaseWakeLock();
        Log.d(TAG, "NotificationListener disconnected");
    }

    private void startForegroundService() {
        createNotificationChannel();

        Intent notifIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notifIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        SharedPreferences prefs = getSharedPreferences("notif_detector_settings", MODE_PRIVATE);
        Set<String> keywords = prefs.getStringSet("filter_keywords", new HashSet<>());
        String contentText = keywords.isEmpty()
                ? "Monitoring all notifications"
                : "Filtering: " + String.join(", ", keywords);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Notification Detector Active")
                .setContentText(contentText)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setSilent(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();

        try {
            startForeground(FOREGROUND_ID, notification);
        } catch (Exception e) {
            Log.e(TAG, "Failed to start foreground", e);
        }
    }

    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Notification Detector Service",
                NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("Keeps the notification detector running in background");
        channel.setShowBadge(false);

        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }

    private void acquireWakeLock() {
        if (wakeLock == null) {
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            if (pm != null) {
                wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "NotifDetector::ListenerLock");
                wakeLock.acquire();
            }
        }
    }

    private void releaseWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
            wakeLock = null;
        }
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null || sbn.getNotification() == null) return;

        String packageName = sbn.getPackageName();

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

        try {
            appName = getPackageManager()
                    .getApplicationLabel(getPackageManager().getApplicationInfo(packageName, 0))
                    .toString();
        } catch (Exception e) {
            appName = packageName;
        }

        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

        Log.d(TAG, "Notification from: " + appName + " | " + title + " | " + text);

        boolean matched = matchesKeywords(title, text, appName);
        if (!matched) return;

        SharedPreferences prefs = getSharedPreferences("notif_detector_settings", MODE_PRIVATE);
        boolean alarmEnabled = prefs.getBoolean("alarm_enabled", true);
        boolean vibrateEnabled = prefs.getBoolean("vibrate_enabled", true);

        if (alarmEnabled) playAlarmSound();
        if (vibrateEnabled) vibrate();

        Intent intent = new Intent(ACTION_NOTIFICATION_RECEIVED);
        intent.putExtra("app_name", appName);
        intent.putExtra("package_name", packageName);
        intent.putExtra("title", title);
        intent.putExtra("text", text);
        intent.putExtra("timestamp", timestamp);
        intent.putExtra("matched", matched);
        sendBroadcast(intent);

        NotificationItem item = new NotificationItem(appName, packageName, title, text, timestamp);
        item.setMatched(matched);
        NotificationStorage.getInstance(this).addNotification(item);
    }

    private boolean matchesKeywords(String title, String text, String appName) {
        SharedPreferences prefs = getSharedPreferences("notif_detector_settings", MODE_PRIVATE);
        Set<String> keywords = prefs.getStringSet("filter_keywords", new HashSet<>());

        if (keywords.isEmpty()) return true;

        String combined = (title + " " + text + " " + appName).toLowerCase(Locale.getDefault());

        for (String keyword : keywords) {
            if (!keyword.trim().isEmpty() && combined.contains(keyword.trim().toLowerCase(Locale.getDefault()))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
    }

    private void playAlarmSound() {
        try {
            stopAlarm();

            SharedPreferences prefs = getSharedPreferences("notif_detector_settings", MODE_PRIVATE);
            String uriStr = prefs.getString("alarm_uri", "");

            Uri alarmUri;
            if (!uriStr.isEmpty()) {
                alarmUri = Uri.parse(uriStr);
            } else {
                alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
                if (alarmUri == null) {
                    alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                }
            }

            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            requestAudioFocus(audioAttributes);

            int durationMs = prefs.getInt("alarm_duration", 3) * 1000;

            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioAttributes(audioAttributes);
            mediaPlayer.setDataSource(this, alarmUri);
            mediaPlayer.setLooping(true);
            mediaPlayer.setOnPreparedListener(mp -> {
                mp.start();
                new android.os.Handler(getMainLooper()).postDelayed(this::stopAlarm, durationMs);
            });
            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Log.e(TAG, "MediaPlayer error: what=" + what + " extra=" + extra);
                stopAlarm();
                return true;
            });
            mediaPlayer.prepareAsync();

        } catch (Exception e) {
            Log.e(TAG, "Error playing alarm sound", e);
        }
    }

    private void requestAudioFocus(AudioAttributes audioAttributes) {
        if (audioManager == null) {
            audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        }
        if (audioManager == null) return;

        audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(audioAttributes)
                .build();
        audioManager.requestAudioFocus(audioFocusRequest);
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
        if (audioManager != null && audioFocusRequest != null) {
            audioManager.abandonAudioFocusRequest(audioFocusRequest);
            audioFocusRequest = null;
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
