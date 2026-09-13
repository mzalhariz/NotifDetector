package com.my88.notifdetector;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.HashSet;
import java.util.Set;

public class SettingsActivity extends AppCompatActivity {

    private static final int REQUEST_ALARM_SOUND = 100;
    private SharedPreferences prefs;
    private TextView tvAlarmName;
    private TextView tvDuration;
    private EditText etKeyword;
    private LinearLayout keywordContainer;
    private TextView tvKeywordCount;
    private Button btnClearKeywords;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefs = getSharedPreferences("notif_detector_settings", MODE_PRIVATE);

        Button btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        // Keyword filter
        etKeyword = findViewById(R.id.etKeyword);
        keywordContainer = findViewById(R.id.keywordContainer);
        tvKeywordCount = findViewById(R.id.tvKeywordCount);
        btnClearKeywords = findViewById(R.id.btnClearKeywords);

        Button btnAddKeyword = findViewById(R.id.btnAddKeyword);
        btnAddKeyword.setOnClickListener(v -> addKeyword());

        etKeyword.setOnEditorActionListener((v, actionId, event) -> {
            addKeyword();
            return true;
        });

        btnClearKeywords.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Clear Keywords")
                    .setMessage("Remove all filter keywords? Alarm will trigger on ALL notifications.")
                    .setPositiveButton("Clear", (d, w) -> {
                        prefs.edit().remove("filter_keywords").apply();
                        refreshKeywords();
                        Toast.makeText(this, "All keywords cleared", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        refreshKeywords();

        // Alarm enabled toggle
        Switch switchAlarm = findViewById(R.id.switchAlarm);
        switchAlarm.setChecked(prefs.getBoolean("alarm_enabled", true));
        switchAlarm.setOnCheckedChangeListener((btn, checked) -> {
            prefs.edit().putBoolean("alarm_enabled", checked).apply();
        });

        // Vibration toggle
        Switch switchVibrate = findViewById(R.id.switchVibrate);
        switchVibrate.setChecked(prefs.getBoolean("vibrate_enabled", true));
        switchVibrate.setOnCheckedChangeListener((btn, checked) -> {
            prefs.edit().putBoolean("vibrate_enabled", checked).apply();
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

    private void addKeyword() {
        String keyword = etKeyword.getText().toString().trim();
        if (keyword.isEmpty()) {
            Toast.makeText(this, "Enter a keyword", Toast.LENGTH_SHORT).show();
            return;
        }

        Set<String> keywords = new HashSet<>(prefs.getStringSet("filter_keywords", new HashSet<>()));
        if (keywords.contains(keyword.toLowerCase())) {
            Toast.makeText(this, "Keyword already exists", Toast.LENGTH_SHORT).show();
            return;
        }

        keywords.add(keyword.toLowerCase());
        prefs.edit().putStringSet("filter_keywords", keywords).apply();
        etKeyword.setText("");
        refreshKeywords();
        Toast.makeText(this, "Added: " + keyword, Toast.LENGTH_SHORT).show();
    }

    private void removeKeyword(String keyword) {
        Set<String> keywords = new HashSet<>(prefs.getStringSet("filter_keywords", new HashSet<>()));
        keywords.remove(keyword);
        prefs.edit().putStringSet("filter_keywords", keywords).apply();
        refreshKeywords();
    }

    private void refreshKeywords() {
        keywordContainer.removeAllViews();
        Set<String> keywords = prefs.getStringSet("filter_keywords", new HashSet<>());

        if (keywords.isEmpty()) {
            tvKeywordCount.setText("No keywords — alarm on ALL notifications");
            tvKeywordCount.setTextColor(0xFF4CAF50);
            btnClearKeywords.setVisibility(View.GONE);
        } else {
            tvKeywordCount.setText(keywords.size() + " keyword" + (keywords.size() > 1 ? "s" : "") + " active");
            tvKeywordCount.setTextColor(0xFFFF9A44);
            btnClearKeywords.setVisibility(View.VISIBLE);

            for (String keyword : keywords) {
                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(dp(12), dp(8), dp(12), dp(8));
                row.setBackgroundColor(0xFF0D0D25);
                LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                rowParams.bottomMargin = dp(4);
                row.setLayoutParams(rowParams);

                TextView tvKeyword = new TextView(this);
                tvKeyword.setText(keyword);
                tvKeyword.setTextSize(13);
                tvKeyword.setTextColor(0xFFE4E4F0);
                tvKeyword.setTypeface(null, Typeface.BOLD);
                LinearLayout.LayoutParams tvParams = new LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
                tvKeyword.setLayoutParams(tvParams);
                row.addView(tvKeyword);

                TextView btnRemove = new TextView(this);
                btnRemove.setText("✕");
                btnRemove.setTextSize(16);
                btnRemove.setTextColor(0xFFFF5252);
                btnRemove.setPadding(dp(16), dp(4), dp(8), dp(4));
                btnRemove.setOnClickListener(v -> removeKeyword(keyword));
                row.addView(btnRemove);

                keywordContainer.addView(row);
            }
        }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
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
