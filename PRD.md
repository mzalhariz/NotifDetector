# NotifDetector - Product Requirements Document

## App Overview

| Field | Detail |
|-------|--------|
| **App Name** | Notif Detector |
| **Package** | com.my88.notifdetector |
| **Platform** | Android (API 26+ / Android 8.0+) |
| **Version** | 1.0 |
| **Author** | MY88 IT Zalhariz |
| **Purpose** | Capture all phone notifications in real-time with keyword-based alarm filtering for datacenter technicians |

---

## Problem Statement

Datacenter technicians at MY88 receive critical alerts (e.g., Raptor alarms, system faults) through various apps on their phones. These notifications can easily be missed during noisy datacenter operations or when the phone is in a pocket. There is no centralized way to:

- Log all incoming notifications for later review
- Trigger a loud alarm only for critical/relevant notifications
- Search and export notification history

---

## Target Users

- Datacenter Technicians (DCT) at MY88 Alibaba Data Center
- On-call engineers who need to catch critical alerts immediately
- Anyone needing a notification logging and alarm tool

---

## Features

### 1. Notification Capture (Background Service)
- Listens to **all notifications** from every app on the phone
- Runs as a **foreground service** with persistent status bar notification
- **Wake lock** keeps the service alive even when screen is off
- **Auto-restart** after phone reboot via Boot Receiver
- Only notifications that pass the keyword filter (see below) are logged: App Name, Package Name, Title, Text, Timestamp — non-matching notifications are discarded and never stored

### 2. Keyword Filter
- User-defined keywords to filter which notifications are captured, logged, and trigger the alarm
- **Case-insensitive** matching against notification title, text, and app name
- If **no keywords** are set, ALL notifications are captured and trigger the alarm
- Non-matching notifications are dropped entirely — not stored, not shown in the list, not exported
- Add, remove, and clear keywords from Settings
- Examples: `raptor`, `alarm`, `critical`, `down`, `fault`, `emergency`

### 3. Alarm Sound
- Plays alarm sound when a **matched** notification is detected
- **Customizable**: user can pick any ringtone/alarm/notification sound from their phone
- **Default**: System alarm sound
- **Adjustable duration**: 1 to 10 seconds (default: 3 seconds)
- Independent ON/OFF toggle

### 4. Vibration
- Vibrates phone when a **matched** notification is detected
- Pattern: 300ms on, 200ms off, 300ms on, 200ms off, 300ms on
- Independent ON/OFF toggle (separate from alarm sound)

### 5. Notification List (Main Screen)
- Displays only keyword-matched notifications in a scrollable list (newest first) — non-matches never reach the list
- **MATCH badge** with orange indicator bar on displayed notifications
- Shows: App Name, Title, Text, Timestamp
- **Real-time updates** via broadcast receiver

### 6. Search
- Full-text search across all captured notifications
- Filters list as you type
- Searches app name, title, and text content

### 7. Export
- Export all notification logs as **JSON file** to Downloads folder
- Fallback: Share via Android share sheet if file write fails

### 8. Clear
- Delete all notification logs with confirmation dialog

### 9. Status Indicator
- Shows **"● Listening"** (green) when notification access is enabled
- Shows **"○ Not Listening"** (red) when access is not granted
- Quick **"Enable Access"** button to open Android notification access settings

---

## UI/UX Design

| Element | Specification |
|---------|--------------|
| **Theme** | Dark futuristic glassmorphism |
| **Background** | #0A0A1A (near black) |
| **Header** | #11112A (dark navy) |
| **Cards** | #111128 (dark purple-gray) |
| **Accent Color** | #FF6A00 (orange) |
| **Secondary** | #FF9A44 (light orange) |
| **Text Primary** | #E4E4F0 (light gray) |
| **Text Secondary** | #888888 (gray) |
| **Error/Delete** | #FF5252 (red) |
| **Success** | #4CAF50 (green) |

### Screens

1. **Main Screen** — Notification list with search, export, clear, and settings buttons
2. **Settings Screen** — Keyword filter, alarm toggle, vibration toggle, alarm tone picker, duration slider

---

## Technical Architecture

### Components

| Component | Type | Purpose |
|-----------|------|---------|
| `MainActivity` | Activity | Main notification list UI |
| `SettingsActivity` | Activity | Settings and keyword management |
| `NotificationListener` | NotificationListenerService | Background notification capture |
| `BootReceiver` | BroadcastReceiver | Auto-start after phone reboot |
| `NotificationAdapter` | RecyclerView.Adapter | Notification card rendering |
| `NotificationItem` | Data Model | Notification data with JSON serialization |
| `NotificationStorage` | Storage | SharedPreferences-based storage (max 500 items) |

### Permissions

| Permission | Purpose |
|------------|---------|
| `POST_NOTIFICATIONS` | Show foreground service notification |
| `VIBRATE` | Vibrate on matched notifications |
| `FOREGROUND_SERVICE` | Keep service alive in background |
| `FOREGROUND_SERVICE_SPECIAL_USE` | Required for Android 14+ |
| `WAKE_LOCK` | Prevent CPU sleep |
| `RECEIVE_BOOT_COMPLETED` | Auto-start after reboot |
| Notification Listener Access | System permission to read all notifications |

### Data Storage

- **SharedPreferences** (`notif_detector_settings`) for settings:
  - `alarm_enabled` (boolean, default: true)
  - `vibrate_enabled` (boolean, default: true)
  - `alarm_uri` (String, default: system alarm)
  - `alarm_duration` (int, default: 3 seconds)
  - `filter_keywords` (StringSet, default: empty)
- **SharedPreferences** (`notification_storage`) for notification logs:
  - JSON array of NotificationItem objects
  - Maximum 500 items (oldest removed when full)

### Build Configuration

| Setting | Value |
|---------|-------|
| compileSdk | 34 |
| minSdk | 26 |
| targetSdk | 34 |
| Android Gradle Plugin | 8.7.0 |
| Gradle | 8.9 |
| Java | 17 |
| AndroidX | Enabled |

---

## Limitations

- Requires **manual grant** of Notification Listener Access (Android security requirement)
- Some phone manufacturers (Xiaomi, Samsung, Huawei) may require additional battery optimization exemptions
- Maximum 500 notification logs stored (auto-purges oldest)
- Notification content depends on what each app exposes — some apps hide content in secure notifications

---

## Future Enhancements

- App-specific filters (only monitor certain apps)
- Custom vibration patterns
- Cloud sync / backup of notification logs
- CSV/Excel export format
- Notification grouping by app
- Dark/light theme toggle
- Widget for home screen status

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2026-09-13 | Initial release — notification capture, keyword filter, alarm, vibration, export, foreground service |
| 1.1 | 2026-09-15 | Non-matching notifications are now dropped entirely instead of being logged with no badge; alarm sound switched to the ALARM audio stream with audio focus request so it plays reliably even when the app isn't in the foreground |
