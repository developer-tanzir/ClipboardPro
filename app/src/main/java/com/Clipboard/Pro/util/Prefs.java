package com.Clipboard.Pro.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

/** Small typed wrapper around SharedPreferences. */
public final class Prefs {

    private static final String FILE = "clipboardpro_prefs";

    private static final String K_THEME = "theme_mode";
    private static final String K_SKIP_SENSITIVE = "skip_sensitive";
    private static final String K_CAPTURE_ON_OPEN = "capture_on_open";
    private static final String K_BG_MONITOR = "background_monitor";
    private static final String K_CLEANUP_DAYS = "cleanup_days";
    private static final String K_APP_LOCK = "app_lock";
    private static final String K_SECURE_WINDOW = "secure_window";
    private static final String K_SORT_NEWEST = "sort_newest";
    private static final String K_INFO_SHOWN = "info_shown";
    private static final String K_LAST_SIG = "last_clip_sig";

    private Prefs() {
    }

    private static SharedPreferences sp(Context c) {
        return c.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public static int getThemeMode(Context c) {
        return sp(c).getInt(K_THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }

    public static void setThemeMode(Context c, int mode) {
        sp(c).edit().putInt(K_THEME, mode).apply();
    }

    public static boolean isSkipSensitive(Context c) {
        return sp(c).getBoolean(K_SKIP_SENSITIVE, true);
    }

    public static void setSkipSensitive(Context c, boolean v) {
        sp(c).edit().putBoolean(K_SKIP_SENSITIVE, v).apply();
    }

    public static boolean isCaptureOnOpen(Context c) {
        return sp(c).getBoolean(K_CAPTURE_ON_OPEN, true);
    }

    public static void setCaptureOnOpen(Context c, boolean v) {
        sp(c).edit().putBoolean(K_CAPTURE_ON_OPEN, v).apply();
    }

    public static boolean isBackgroundMonitor(Context c) {
        return sp(c).getBoolean(K_BG_MONITOR, false);
    }

    public static void setBackgroundMonitor(Context c, boolean v) {
        sp(c).edit().putBoolean(K_BG_MONITOR, v).apply();
    }

    /** 0 = never. */
    public static int getCleanupDays(Context c) {
        return sp(c).getInt(K_CLEANUP_DAYS, 0);
    }

    public static void setCleanupDays(Context c, int days) {
        sp(c).edit().putInt(K_CLEANUP_DAYS, days).apply();
    }

    public static boolean isAppLock(Context c) {
        return sp(c).getBoolean(K_APP_LOCK, false);
    }

    public static void setAppLock(Context c, boolean v) {
        sp(c).edit().putBoolean(K_APP_LOCK, v).apply();
    }

    public static boolean isSecureWindow(Context c) {
        return sp(c).getBoolean(K_SECURE_WINDOW, false);
    }

    public static void setSecureWindow(Context c, boolean v) {
        sp(c).edit().putBoolean(K_SECURE_WINDOW, v).apply();
    }

    public static boolean isSortNewest(Context c) {
        return sp(c).getBoolean(K_SORT_NEWEST, true);
    }

    public static void setSortNewest(Context c, boolean v) {
        sp(c).edit().putBoolean(K_SORT_NEWEST, v).apply();
    }

    public static boolean isInfoShown(Context c) {
        return sp(c).getBoolean(K_INFO_SHOWN, false);
    }

    public static void setInfoShown(Context c, boolean v) {
        sp(c).edit().putBoolean(K_INFO_SHOWN, v).apply();
    }

    public static String getLastClipSig(Context c) {
        return sp(c).getString(K_LAST_SIG, "");
    }

    public static void setLastClipSig(Context c, String sig) {
        sp(c).edit().putString(K_LAST_SIG, sig).apply();
    }
}
