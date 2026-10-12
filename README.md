<div align="center">

# 📋 ClipboardPro

**A lightweight, private clipboard history manager for Android.**
No account. No cloud. No internet permission.

![Android](https://img.shields.io/badge/Android-7.0%2B-3DDC84?logo=android&logoColor=white)
![Java](https://img.shields.io/badge/Language-Java-orange)
![Material 3](https://img.shields.io/badge/UI-Material%203-D6007A)
![License](https://img.shields.io/badge/License-MIT-blue)

</div>

---

## ✨ Features

| | |
|---|---|
| 🗂️ **Clipboard history** | Saves copied text locally with timestamps and text previews |
| 🔎 **Search & sort** | Instant search, newest/oldest sorting, category and favorites filters |
| 📌 **Pin & ⭐ favorite** | Pinned items live in their own section at the top |
| ✏️ **Full item control** | Copy, edit, pin, favorite, share, or delete any item (with undo) |
| ✅ **Bulk actions** | Long-press to multi-select, then copy, pin, favorite, share, or delete |
| 🏷️ **Categories** | Auto-detected (Links, Emails, Phones, Code) and editable (General, Work, Personal) |
| ♻️ **Duplicate detection** | The same text is kept once and moved to the top |
| ➕ **Manual add** | Floating action button to save your own text |
| 🌗 **Themes** | Light, dark, or follow the system |
| 🔐 **Privacy tools** | Optional app lock (fingerprint, face, or device PIN), hide in recent apps, block screenshots |
| 🛡️ **Sensitive content** | Optionally skips things that look like passwords, card numbers, one-time codes, or API keys |
| 🧹 **Auto-cleanup** | Delete history after 1, 7, 30, 90 days, or 1 year (pinned and favorites are kept) |

## ⚠️ How clipboard capture works

Android restricts clipboard access, and the rules depend on your version. ClipboardPro is honest about this and **never promises unrestricted automatic tracking**.

| Android version | What can be captured |
|---|---|
| **9 and below** | Optional background monitor can save copies from any app |
| **10 and above** | Only the app in the foreground (or the active keyboard) may read the clipboard |

To work within these limits, ClipboardPro saves clips:

- when you **open the app** or while it is on screen
- from the **"Save clipboard" notification button** and the **launcher shortcut**
- via **Share → Save to ClipboardPro** and **Select text → Save to ClipboardPro**
- via the **Add** button or **Save clipboard now** in the menu

Copies made while the app is closed on Android 10+ cannot always be captured. Android 12+ may also show a small system notice when an app reads the clipboard; that is normal.

## 📲 Install

1. Download the latest APK from the [**Releases**](../../releases) page.
2. Open it on your phone and allow **Install unknown apps** if asked.
3. Launch **ClipboardPro**.

> The APK is signed with a debug key for testing. Updating later requires the same signing key.

## 🛠️ Build it yourself

### With GitHub Actions (no Android Studio needed)

1. Fork or push this repository to GitHub.
2. Open the **Actions** tab → **Build ClipboardPro APK** → **Run workflow**.
3. Download `ClipboardPro-debug-apk` from the run's **Artifacts**.

### Locally

Requirements: JDK 17, Android SDK 34, Gradle 8.9+ (or open the project in Android Studio).

```bash
gradle assembleDebug
# APK: app/build/outputs/apk/debug/
```

## 🧱 Tech stack

- **Language:** Java, XML layouts, ViewBinding
- **UI:** Material 3, RecyclerView with DiffUtil animations
- **Storage:** Room (SQLite), local only
- **Architecture:** ViewModel + LiveData, single-threaded repository
- **Security:** AndroidX Biometric (with device-credential fallback)
- **Build:** Gradle, AGP 8.5, `minSdk 24`, `targetSdk 34`

## 🔒 Privacy

- All data stays on your device in a private app database.
- The app requests **no internet permission**.
- Cloud backup is disabled so history is never uploaded.
- Permissions used: `POST_NOTIFICATIONS` and foreground-service permissions, only for the optional background monitor.

## 📁 Project structure

```
app/src/main/java/com/Clipboard/Pro/
├── data/      Room entity, DAO, database, repository
├── service/   Optional foreground clipboard monitor
├── ui/        MainActivity, SettingsActivity, CaptureActivity, adapter, ViewModel
└── util/      Prefs, AppLock, clipboard helper, sensitive-content and category detection
```

## 🤝 Contributing

Issues and pull requests are welcome. For larger changes, please open an issue first to discuss what you would like to change.

## 📄 License

Released under the [MIT License](LICENSE).
