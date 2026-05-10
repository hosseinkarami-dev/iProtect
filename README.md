# 🛡️ iProtect – Per-App Network Firewall for Android

iProtect is a powerful Android firewall that lists **all installed apps (including system apps)** and allows you to individually control their access to **Wi-Fi** and **Mobile/Cellular data**. It creates a local VPN tunnel to enforce rules in real time — no root access required.

## 🚀 Key Features

- 📱 Lists **every app** on the device (user + system apps)
- ✅ Per-app toggles for **Wi-Fi** and **Cellular data** access
- 🔒 Blocks internet for selected apps completely or per network type
- 🧠 Built on **Android VpnService** – no root needed
- ⚡ Real-time rule enforcement with a single **Start / Stop** button
- 📊 Clean, intuitive UI with app icons, package names, and status indicators

## 🛠 How It Works

1. iProtect reads all installed packages via `PackageManager`
2. You check/uncheck **Wi-Fi** and/or **Cellular** access for each app
3. When you press **Start**, iProtect creates a local VPN
4. All system traffic passes through this VPN
5. The app filters packets based on your rules (allow/deny per network type)
6. Press **Stop** to disable filtering and restore normal connectivity

## 📲 Installation & Setup

1. Install the iProtect APK on your Android device (Android 5.0+ required for VPN)
2. Open the app – it will request **VPN permission** once
3. Grant the VPN permission when prompted (this is required for filtering)
4. Browse the full list of apps (system apps included)
5. Tap on any app to toggle:
   - 📶 Wi-Fi access
   - 📱 Cellular data access
6. Press the **Start** button (floating or bottom action button)
7. The VPN notification will appear – iProtect is now active

## 🖼️ User Interface

- **Main Screen**: Scrollable list of all apps with icons, names, and two toggle switches per row
- **Options Menu**: Select all / none, show system apps toggle
- **Status Bar**: Shows active connection count and VPN status
- **Start/Stop Button**: Changes color when active

## 🔧 Permissions Required

| Permission | Reason |
|------------|--------|
| `android.permission.PACKAGE_USAGE_STATS` | To detect currently running apps (optional, for advanced stats) |
| `android.permission.QUERY_ALL_PACKAGES` | To list system apps (Android 11+) |
| `VpnService` permission (user grants via dialog) | To create the local VPN tunnel |

> ℹ️ iProtect **does not** send any data to external servers. All filtering happens locally on your device.

## 🧪 Technical Details

- **Language**: Java (Android SDK)
- **Core Component**: `VpnService` subclass with `Builder` to establish tunnel
- **Traffic Handling**: Reads/writes to `FileDescriptor` and checks rules from a `HashMap<String, AppRules>`
- **Rule Storage**: SharedPreferences or Room database (persists across reboots)
- **App Enumeration**: `PackageManager.getInstalledPackages()` with `GET_ACTIVITIES` flag

## ⚠️ Important Notes

- The **VPN icon** will stay in the notification bar while iProtect is active – this is normal and indicates filtering is working
- Blocking **Cellular data** only applies when Wi-Fi is off. If both Wi-Fi and Cellular are disabled, the app has no internet
- Some system apps may misbehave if blocked – use caution with critical services
- Performance impact is minimal (packet inspection is lightweight)

## 📸 Example Rule Logic

```java
// Simplified rule check inside VpnService thread
boolean allowWifi = appRules.isWifiAllowed();
boolean allowMobile = appRules.isMobileAllowed();
boolean isWifiConnected = connectivityManager.isWifi();

if ((isWifiConnected && !allowWifi) || (!isWifiConnected && !allowMobile)) {
    // Drop packet (block)
} else {
    // Forward packet (allow)
}