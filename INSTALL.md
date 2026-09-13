# NotifDetector - Installation Guide

## Method 1: Download APK from GitHub Actions (Recommended)

### Step 1: Push Code to GitHub

1. Create a repository on GitHub: `https://github.com/new`
   - Repository name: `NotifDetector`
   - Set to **Public** or **Private**
   - Click **Create repository**

2. Upload all project files to the repository
   - You can drag and drop the extracted `NotifDetector` folder contents
   - Or use `git push` from command line

### Step 2: GitHub Actions Auto-Build

Once the code is pushed, GitHub Actions will automatically build the APK.

1. Go to your repo → **Actions** tab
2. You should see the **"Build APK"** workflow running
3. Wait for it to complete (green checkmark)
4. Click on the completed run → **Artifacts** section
5. Download **NotifDetector-debug.apk**

> If the workflow doesn't appear, make sure `.github/workflows/build.yml` exists in the repo.

### Step 3: Install APK on Phone

1. Transfer the downloaded APK to your Android phone:
   - Via USB cable
   - Via Google Drive / cloud storage
   - Via Telegram / WhatsApp (send to yourself)
   - Via direct download on phone browser

2. On your phone, tap the APK file to install

3. If prompted **"Install from unknown sources"**:
   - Go to **Settings → Security → Install unknown apps**
   - Allow the browser/file manager you're using
   - Try installing again

---

## Method 2: Build Locally with Android Studio

### Prerequisites

- Android Studio (latest version)
- JDK 17
- Android SDK 34

### Steps

1. Open Android Studio
2. Click **File → Open** and select the `NotifDetector` folder
3. Wait for Gradle sync to complete
4. Click **Build → Build Bundle(s) / APK(s) → Build APK(s)**
5. APK will be at: `app/build/outputs/apk/debug/app-debug.apk`
6. Transfer to phone and install

---

## Post-Installation Setup

### Step 1: Enable Notification Access (REQUIRED)

This is the most important step. Without this, the app cannot read notifications.

1. Open **Notif Detector** app
2. Tap **"Enable Access"** button
3. You'll be taken to **Settings → Notification access**
4. Find **"Notification Detector"** in the list
5. Toggle it **ON**
6. Confirm the permission dialog

> You can also manually go to: **Settings → Apps → Special access → Notification access**

### Step 2: Disable Battery Optimization

To prevent Android from killing the app in background:

1. Go to **Settings → Apps → Notif Detector → Battery**
2. Select **"Unrestricted"** (or "Don't optimize")

#### Brand-Specific Instructions:

**Samsung:**
- Settings → Battery → Background usage limits → Never sleeping apps → Add Notif Detector

**Xiaomi / Redmi / POCO:**
- Settings → Apps → Notif Detector → Battery saver → No restrictions
- Also: Settings → Apps → Notif Detector → Autostart → Enable

**Huawei / Honor:**
- Settings → Battery → App launch → Notif Detector → Manage manually → Enable all toggles

**OPPO / Realme / OnePlus:**
- Settings → Battery → More settings → Optimize battery use → Notif Detector → Don't optimize

### Step 3: Grant Notification Permission (Android 13+)

On Android 13 and above, you need to allow the app to post its own notifications (for the foreground service):

1. When prompted, tap **"Allow"** for notification permission
2. Or go to: **Settings → Apps → Notif Detector → Notifications → Enable**

### Step 4: Configure Keywords (Optional)

1. Open the app → tap **"Settings"**
2. In the **KEYWORD FILTER** section:
   - Type a keyword (e.g., `raptor`, `alarm`, `critical`)
   - Tap **"+ Add"**
   - Repeat for each keyword
3. The alarm will now only trigger when a notification **contains** one of your keywords

### Step 5: Configure Alarm & Vibration

In Settings:
- **Alarm Sound** — Toggle ON/OFF, pick custom sound
- **Vibration** — Toggle ON/OFF independently
- **Alarm Duration** — Slide between 1-10 seconds

---

## How It Works

```
Phone Notification Arrives
        │
        ▼
NotificationListener (Background Service)
        │
        ├── Extract: App Name, Title, Text, Timestamp
        │
        ├── Check Keywords
        │       │
        │       ├── Match Found → Play Alarm + Vibrate
        │       │
        │       └── No Match → Log only (no alarm)
        │
        ├── Save to Storage
        │
        └── Broadcast to Main Screen (if open)
                │
                ▼
        RecyclerView updates in real-time
```

---

## Troubleshooting

| Problem | Solution |
|---------|----------|
| App not detecting notifications | Enable Notification Access (Step 1) |
| App killed in background | Disable Battery Optimization (Step 2) |
| No alarm sound | Check Settings → Alarm Sound is ON |
| No vibration | Check Settings → Vibration is ON |
| Alarm on every notification | Add keywords in Settings to filter |
| App not starting after reboot | Ensure Autostart is enabled (brand-specific) |
| "Install from unknown sources" error | Allow install from the app you're using to open the APK |
| Build fails on GitHub Actions | Check that `build.gradle` uses AGP 8.7.0 and Gradle 8.9 |

---

## File Structure

```
NotifDetector/
├── app/
│   ├── build.gradle
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/my88/notifdetector/
│       │   ├── MainActivity.java
│       │   ├── SettingsActivity.java
│       │   ├── NotificationListener.java
│       │   ├── BootReceiver.java
│       │   ├── NotificationAdapter.java
│       │   ├── NotificationItem.java
│       │   └── NotificationStorage.java
│       └── res/
│           ├── drawable/
│           │   ├── ic_launcher_background.xml
│           │   ├── ic_launcher_foreground.xml
│           │   └── search_bg.xml
│           ├── layout/
│           │   ├── activity_main.xml
│           │   ├── activity_settings.xml
│           │   └── item_notification.xml
│           ├── mipmap-anydpi-v26/
│           │   └── ic_launcher.xml
│           └── values/
│               ├── strings.xml
│               └── styles.xml
├── build.gradle
├── settings.gradle
├── gradle.properties
├── gradle/wrapper/
│   └── gradle-wrapper.properties
├── .github/workflows/
│   └── build.yml
├── PRD.md
└── INSTALL.md
```

---

*Design & Develop by MY88 IT Zalhariz*
