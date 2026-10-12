# ClipboardPro

Local-only clipboard history manager for Android. Java + XML, Room, Material 3.
Package: `com.Clipboard.Pro` · minSdk 24 · targetSdk 34

## Build with GitHub Actions
1. Create a new GitHub repository.
2. Upload **everything in this folder**, including the hidden `.github` folder
   (easiest: `git init && git add . && git commit -m "init" && git push`).
3. Open the **Actions** tab -> *Build ClipboardPro APK* (runs on every push to `main`/`master`,
   or press **Run workflow**).
4. When it finishes, download `ClipboardPro-debug-apk` from the run's **Artifacts** and install the APK.

The workflow installs Gradle 8.9 and JDK 17 itself, so no `gradlew` file is needed.
The release APK is signed with the debug key for testing only; use your own keystore before Google Play.

## How clipboard capture works (be honest with users)
Android 10+ blocks background clipboard reads. ClipboardPro captures:
- when the app is opened / on screen (and the clipboard listener while it is running),
- from the notification button **Save clipboard** and the launcher shortcut (tiny invisible activity),
- via **Share -> Save to ClipboardPro** and **Select text -> Save to ClipboardPro**,
- via the Add button, and "Save clipboard now" in the menu.
Android 9 and below can also use the optional background monitor (foreground service).
Android 12+ shows a system notice when an app reads the clipboard; that is normal.

## Notes
- Sensitive detection (cards, OTP, passwords, keys...) is heuristic. Toggle in Settings.
- Auto-delete never removes pinned or favorite items. Cleanup runs when the app starts or a clip is saved.
- App lock uses fingerprint/face or the device PIN/pattern/password; the app turns it off if the screen lock is removed.
- The background monitor does not restart after a reboot; open the app once to restart it.
- No INTERNET permission is requested.
