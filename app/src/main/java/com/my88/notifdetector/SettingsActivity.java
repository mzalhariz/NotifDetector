package com.my88.notifdetector;

import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    private static final int REQUEST_ALARM_SOUND = 100;
    private SharedPreferences prefs;
    private TextView tvAlarmName;
    private TextView tvDuration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefs = getSharedPreferences("notif_detector_settings", MODE_PRIVATE);

        // Back button
        Button btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        // Alarm enabled toggle
        Switch switchAlarm = findViewById(R.id.switchAlarm);
        switchAlarm.setChecked(prefs.getBoolean("alarm_enabled", true));
        switchAlarm.setOnCheckedChangeListener((btn, checked) -> {
            prefs.edit().putBoolean("alarm_enabled", checked).apply();
        });

        // Alarm sound picker
        tvAlarmName = findViewById(R.id.tvAlarmName);
        updateAlarmName();

        Button btnPickAlarm = findViewById(R.id.btnPickAlarm);
        btnPickAlarm.setOnClickListener(v -> {
            Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM | RingtoneManager.TYPE_NOTIFICATION | RingtoneManager.TYPE_RINGTONE);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Alarm Sound");
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);

            String currentUri = prefs.getString("alarm_uri", "");
            if (!currentUri.isEmpty()) {
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(currentUri));
            }

            startActivityForResult(intent, REQUEST_ALARM_SOUND);
        });

        // Reset to default (police siren)
        Button btnDefault = findViewById(R.id.btnDefault);
        btnDefault.setOnClickListener(v -> {
            prefs.edit().remove("alarm_uri").apply();
            updateAlarmName();
            Toast.makeText(this, "Reset to default alarm", Toast.LENGTH_SHORT).show();
        });

        // Duration slider
        tvDuration = findViewById(R.id.tvDuration);
        SeekBar seekDuration = findViewById(R.id.seekDuration);
        int currentDuration = prefs.getInt("alarm_duration", 3);
        seekDuration.setProgress(currentDuration);
        tvDuration.setText(currentDuration + " seconds");

        seekDuration.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                int val = Math.max(1, progress);
                tvDuration.setText(val + " seconds");
                prefs.edit().putInt("alarm_duration", val).apply();
            }
            @Override public void onStartTrackingTouch(SeekBar bar) {}
            @Override public void onStopTrackingTouch(SeekBar bar) {}
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ALARM_SOUND && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
            if (uri != null) {
                prefs.edit().putString("alarm_uri", uri.toString()).apply();
                updateAlarmName();
                Toast.makeText(this, "Alarm sound updated!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void updateAlarmName() {
        String uriStr = prefs.getString("alarm_uri", "");
        if (uriStr.isEmpty()) {
            tvAlarmName.setText("Default (System Alarm / Police Siren)");
        } else {
            try {
                Uri uri = Uri.parse(uriStr);
                String name = RingtoneManager.getRingtone(this, uri).getTitle(this);
                tvAlarmName.setText(name);
            } catch (Exception e) {
                tvAlarmName.setText("Custom sound");
            }
        }
    }
}
