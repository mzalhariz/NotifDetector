package com.my88.notifdetector;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class NotificationStorage {
    private static final String PREFS_NAME = "notif_detector_prefs";
    private static final String KEY_NOTIFICATIONS = "notifications";
    private static final int MAX_ITEMS = 500;

    private static NotificationStorage instance;
    private SharedPreferences prefs;
    private List<NotificationItem> items;

    private NotificationStorage(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        items = loadFromPrefs();
    }

    public static synchronized NotificationStorage getInstance(Context context) {
        if (instance == null) {
            instance = new NotificationStorage(context);
        }
        return instance;
    }

    public void addNotification(NotificationItem item) {
        if (item == null) return;
        items.add(0, item);
        if (items.size() > MAX_ITEMS) {
            items = new ArrayList<>(items.subList(0, MAX_ITEMS));
        }
        saveToPrefs();
    }

    public List<NotificationItem> getAll() {
        return new ArrayList<>(items);
    }

    public List<NotificationItem> search(String query) {
        if (query == null || query.isEmpty()) return getAll();
        String lowerQuery = query.toLowerCase();
        List<NotificationItem> result = new ArrayList<>();
        for (NotificationItem item : items) {
            if (item.getAppName().toLowerCase().contains(lowerQuery) ||
                    item.getTitle().toLowerCase().contains(lowerQuery) ||
                    item.getText().toLowerCase().contains(lowerQuery)) {
                result.add(item);
            }
        }
        return result;
    }

    public void clearAll() {
        items.clear();
        saveToPrefs();
    }

    public String exportToJson() {
        JSONArray arr = new JSONArray();
        for (NotificationItem item : items) {
            arr.put(item.toJson());
        }
        return arr.toString();
    }

    private List<NotificationItem> loadFromPrefs() {
        List<NotificationItem> list = new ArrayList<>();
        String json = prefs.getString(KEY_NOTIFICATIONS, "[]");
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                NotificationItem item = NotificationItem.fromJson(obj);
                if (item != null) list.add(item);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    private void saveToPrefs() {
        JSONArray arr = new JSONArray();
        for (NotificationItem item : items) {
            arr.put(item.toJson());
        }
        prefs.edit().putString(KEY_NOTIFICATIONS, arr.toString()).apply();
    }
}
