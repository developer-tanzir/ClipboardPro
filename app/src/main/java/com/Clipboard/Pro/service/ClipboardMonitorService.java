package com.Clipboard.Pro.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.Clipboard.Pro.ClipboardProApp;
import com.Clipboard.Pro.R;
import com.Clipboard.Pro.data.ClipRepository;
import com.Clipboard.Pro.ui.CaptureActivity;
import com.Clipboard.Pro.ui.MainActivity;

/**
 * Optional foreground service that listens for clipboard changes.
 *
 * Be aware: on Android 10+ the OS only lets the foreground app / default keyboard read the clipboard,
 * so on those versions this can only record copies while ClipboardPro itself is on screen. The
 * notification therefore offers a "Save clipboard" button that opens a tiny invisible activity which
 * is allowed to read the clipboard.
 */
public class ClipboardMonitorService extends Service {

    private static final int NOTIFICATION_ID = 1001;

    private ClipboardManager clipboardManager;
    private boolean registered;

    private final ClipboardManager.OnPrimaryClipChangedListener listener = () ->
            ClipRepository.captureFromClipboard(getApplicationContext(), false, true, null);

    public static void start(Context context) {
        ContextCompat.startForegroundService(context, new Intent(context, ClipboardMonitorService.class));
    }

    public static void stop(Context context) {
        context.stopService(new Intent(context, ClipboardMonitorService.class));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Notification notification = buildNotification();
        try {
            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
            } else {
                startForeground(NOTIFICATION_ID, notification);
            }
        } catch (Exception e) {
            // Android refused to let us run in the foreground right now (e.g. started from background).
            stopSelf();
            return START_NOT_STICKY;
        }
        if (!registered) {
            clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            if (clipboardManager != null) {
                clipboardManager.addPrimaryClipChangedListener(listener);
                registered = true;
            }
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (registered && clipboardManager != null) {
            clipboardManager.removePrimaryClipChangedListener(listener);
            registered = false;
        }
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private Notification buildNotification() {
        int immutable = PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT;

        Intent open = new Intent(this, MainActivity.class);
        PendingIntent openPi = PendingIntent.getActivity(this, 0, open, immutable);

        Intent capture = new Intent(this, CaptureActivity.class)
                .setAction(CaptureActivity.ACTION_CAPTURE)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);
        PendingIntent capturePi = PendingIntent.getActivity(this, 1, capture, immutable);

        return new NotificationCompat.Builder(this, ClipboardProApp.CHANNEL_MONITOR)
                .setSmallIcon(R.drawable.ic_stat_clip)
                .setContentTitle(getString(R.string.notif_title))
                .setContentText(getString(R.string.notif_text))
                .setStyle(new NotificationCompat.BigTextStyle().bigText(getString(R.string.notif_text)))
                .setContentIntent(openPi)
                .addAction(0, getString(R.string.action_save_clipboard), capturePi)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }
}
