package com.Clipboard.Pro;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

import androidx.appcompat.app.AppCompatDelegate;

import com.Clipboard.Pro.data.ClipRepository;
import com.Clipboard.Pro.util.Prefs;

public class ClipboardProApp extends Application {

    public static final String CHANNEL_MONITOR = "clipboard_monitor";

    @Override
    public void onCreate() {
        super.onCreate();
        AppCompatDelegate.setDefaultNightMode(Prefs.getThemeMode(this));
        createNotificationChannel();
        ClipRepository.cleanupAsync(this);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_MONITOR,
                    getString(R.string.channel_monitor_name),
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription(getString(R.string.channel_monitor_desc));
            channel.setShowBadge(false);
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) {
                nm.createNotificationChannel(channel);
            }
        }
    }
}
