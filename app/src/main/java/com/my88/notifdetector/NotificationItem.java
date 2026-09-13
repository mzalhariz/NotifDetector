package com.my88.notifdetector;

import org.json.JSONException;
import org.json.JSONObject;

public class NotificationItem {
    private String appName;
    private String packageName;
    private String title;
    private String text;
    private String timestamp;
    private boolean matched;

    public NotificationItem(String appName, String packageName, String title, String text, String timestamp) {
        this.appName = appName;
        this.packageName = packageName;
        this.title = title;
        this.text = text;
        this.timestamp = timestamp;
        this.matched = false;
    }

    public String getAppName() { return appName; }
    public String getPackageName() { return packageName; }
    public String getTitle() { return title; }
    public String getText() { return text; }
    public String getTimestamp() { return timestamp; }
    public boolean isMatched() { return matched; }
    public void setMatched(boolean matched) { this.matched = matched; }

    public JSONObject toJson() {
        JSONObject obj = new JSONObject();
        try {
            obj.put("app_name", appName);
            obj.put("package_name", packageName);
            obj.put("title", title);
            obj.put("text", text);
            obj.put("timestamp", timestamp);
            obj.put("matched", matched);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return obj;
    }

    public static NotificationItem fromJson(JSONObject obj) {
        try {
            NotificationItem item = new NotificationItem(
                    obj.optString("app_name", ""),
                    obj.optString("package_name", ""),
                    obj.optString("title", ""),
                    obj.optString("text", ""),
                    obj.optString("timestamp", "")
            );
            item.setMatched(obj.optBoolean("matched", false));
            return item;
        } catch (Exception e) {
            return null;
        }
    }
}
