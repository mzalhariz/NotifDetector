package com.my88.notifdetector;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.FileWriter;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private NotificationAdapter adapter;
    private NotificationStorage storage;
    private TextView tvStatus;
    private TextView tvCount;
    private EditText etSearch;
    private BroadcastReceiver receiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        storage = NotificationStorage.getInstance(this);
        tvStatus = findViewById(R.id.tvStatus);
        tvCount = findViewById(R.id.tvCount);
        etSearch = findViewById(R.id.etSearch);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NotificationAdapter();
        recyclerView.setAdapter(adapter);

        Button btnSettings = findViewById(R.id.btnSettings);
        btnSettings.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));

        Button btnEnable = findViewById(R.id.btnEnable);
        btnEnable.setOnClickListener(v -> openNotificationAccessSettings());

        Button btnClear = findViewById(R.id.btnClear);
        btnClear.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Clear All")
                    .setMessage("Delete all notification logs?")
                    .setPositiveButton("Clear", (d, w) -> {
                        storage.clearAll();
                        refreshList();
                        Toast.makeText(this, "Cleared", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        Button btnExport = findViewById(R.id.btnExport);
        btnExport.setOnClickListener(v -> exportData());

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterList(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Register broadcast receiver for real-time updates
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String appName = intent.getStringExtra("app_name");
                String packageName = intent.getStringExtra("package_name");
                String title = intent.getStringExtra("title");
                String text = intent.getStringExtra("text");
                String timestamp = intent.getStringExtra("timestamp");

                boolean matched = intent.getBooleanExtra("matched", false);
                NotificationItem item = new NotificationItem(appName, packageName, title, text, timestamp);
                item.setMatched(matched);
                adapter.addItem(item);
                recyclerView.scrollToPosition(0);
                updateCount();
            }
        };

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, new IntentFilter(NotificationListener.ACTION_NOTIFICATION_RECEIVED),
                    Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(receiver, new IntentFilter(NotificationListener.ACTION_NOTIFICATION_RECEIVED));
        }

        refreshList();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatus();
        refreshList();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (receiver != null) {
            unregisterReceiver(receiver);
        }
    }

    private void updateStatus() {
        boolean enabled = isNotificationServiceEnabled();
        tvStatus.setText(enabled ? "● Listening" : "○ Not Listening");
        tvStatus.setTextColor(enabled ? 0xFF4CAF50 : 0xFFFF5252);
    }

    private void updateCount() {
        tvCount.setText(storage.getAll().size() + " notifications");
    }

    private void refreshList() {
        List<NotificationItem> items = storage.getAll();
        adapter.setItems(items);
        updateCount();
        updateStatus();
    }

    private void filterList(String query) {
        List<NotificationItem> items = storage.search(query);
        adapter.setItems(items);
        tvCount.setText(items.size() + " results");
    }

    private boolean isNotificationServiceEnabled() {
        String pkgName = getPackageName();
        String flat = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        return flat != null && flat.contains(pkgName);
    }

    private void openNotificationAccessSettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Please enable notification access manually in Settings", Toast.LENGTH_LONG).show();
        }
    }

    private void exportData() {
        String json = storage.exportToJson();
        try {
            File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File file = new File(dir, "notification_log_" + System.currentTimeMillis() + ".json");
            FileWriter writer = new FileWriter(file);
            writer.write(json);
            writer.close();
            Toast.makeText(this, "Exported to Downloads: " + file.getName(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            // Fallback: share via intent
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("application/json");
            shareIntent.putExtra(Intent.EXTRA_TEXT, json);
            startActivity(Intent.createChooser(shareIntent, "Export Notifications"));
        }
    }
}
