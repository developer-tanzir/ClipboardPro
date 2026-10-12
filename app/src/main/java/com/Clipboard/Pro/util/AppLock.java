package com.Clipboard.Pro.util;

import android.content.Context;
import android.os.SystemClock;

import androidx.biometric.BiometricManager;

/** Tracks whether the optional app lock is currently satisfied. */
public final class AppLock {

    /** Fingerprint/face (weak or better) or the device PIN/pattern/password. Valid on API 23+. */
    public static final int AUTHENTICATORS =
            BiometricManager.Authenticators.BIOMETRIC_WEAK
                    | BiometricManager.Authenticators.DEVICE_CREDENTIAL;

    /** After leaving the app, it stays unlocked for this long so quick app switches are painless. */
    private static final long GRACE_MS = 30_000L;

    private static boolean unlocked;
    private static long lastActive;

    private AppLock() {
    }

    public static boolean canAuthenticate(Context c) {
        return BiometricManager.from(c).canAuthenticate(AUTHENTICATORS)
                == BiometricManager.BIOMETRIC_SUCCESS;
    }

    public static synchronized boolean isLockRequired(Context c) {
        if (!Prefs.isAppLock(c)) {
            return false;
        }
        if (!unlocked) {
            return true;
        }
        return SystemClock.elapsedRealtime() - lastActive > GRACE_MS;
    }

    public static synchronized void markUnlocked() {
        unlocked = true;
        lastActive = SystemClock.elapsedRealtime();
    }

    public static synchronized void touch() {
        lastActive = SystemClock.elapsedRealtime();
    }
}
