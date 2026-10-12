package com.Clipboard.Pro.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;

import com.Clipboard.Pro.R;
import com.Clipboard.Pro.data.ClipRepository;
import com.Clipboard.Pro.databinding.ActivitySettingsBinding;
import com.Clipboard.Pro.service.ClipboardMonitorService;
import com.Clipboard.Pro.util.AppLock;
import com.Clipboard.Pro.util.Prefs;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;

public class SettingsActivity extends LockableActivity {

    private static final int[] THEME_MODES = {
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
            AppCompatDelegate.MODE_NIGHT_NO,
            AppCompatDelegate.MODE_NIGHT_YES
    };
    private static final int[] CLEANUP_DAYS = {0, 1, 7, 30, 90, 365};

    private ActivitySettingsBinding binding;
    private boolean updating;

    private final ActivityResultLauncher<String> notificationPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                // The monitor works either way; without permission its notification is just hidden.
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        bindState();
        bindVersion();

        // ---- Appearance
        binding.rowTheme.setOnClickListener(v -> showThemeDialog());

        // ---- Capture
        binding.rowCaptureOpen.setOnClickListener(v -> binding.switchCaptureOpen.toggle());
        binding.switchCaptureOpen.setOnCheckedChangeListener((b, checked) -> {
            if (!updating) {
                Prefs.setCaptureOnOpen(this, checked);
            }
        });

        binding.rowSkipSensitive.setOnClickListener(v -> binding.switchSkipSensitive.toggle());
        binding.switchSkipSensitive.setOnCheckedChangeListener((b, checked) -> {
            if (!updating) {
                Prefs.setSkipSensitive(this, checked);
            }
        });

        binding.rowBgMonitor.setOnClickListener(v -> binding.switchBgMonitor.toggle());
        binding.switchBgMonitor.setOnCheckedChangeListener((b, checked) -> {
            if (updating) {
                return;
            }
            if (checked) {
                confirmEnableMonitor();
            } else {
                Prefs.setBackgroundMonitor(this, false);
                ClipboardMonitorService.stop(this);
            }
        });

        // ---- History
        binding.rowCleanup.setOnClickListener(v -> showCleanupDialog());
        binding.rowClear.setOnClickListener(v -> Dialogs.confirmClear(this,
                () -> Toast.makeText(this, R.string.msg_history_cleared, Toast.LENGTH_SHORT).show()));

        // ---- Privacy
        binding.rowAppLock.setOnClickListener(v -> binding.switchAppLock.toggle());
        binding.switchAppLock.setOnCheckedChangeListener((b, checked) -> {
            if (updating) {
                return;
            }
            if (checked) {
                if (AppLock.canAuthenticate(this)) {
                    Prefs.setAppLock(this, true);
                    AppLock.markUnlocked();
                } else {
                    setSwitch(binding.switchAppLock, false);
                    new MaterialAlertDialogBuilder(this)
                            .setTitle(R.string.lock_setup_title)
                            .setMessage(R.string.lock_setup_message)
                            .setPositiveButton(android.R.string.ok, null)
                            .show();
                }
            } else {
                Prefs.setAppLock(this, false);
            }
        });

        binding.rowSecure.setOnClickListener(v -> binding.switchSecure.toggle());
        binding.switchSecure.setOnCheckedChangeListener((b, checked) -> {
            if (updating) {
                return;
            }
            Prefs.setSecureWindow(this, checked);
            if (checked) {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
            } else {
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
            }
        });

        // ---- About
        binding.rowInfo.setOnClickListener(v -> Dialogs.showClipboardInfo(this));
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // ------------------------------------------------------------ state

    private void bindState() {
        updating = true;
        binding.switchCaptureOpen.setChecked(Prefs.isCaptureOnOpen(this));
        binding.switchSkipSensitive.setChecked(Prefs.isSkipSensitive(this));
        binding.switchBgMonitor.setChecked(Prefs.isBackgroundMonitor(this));
        binding.switchAppLock.setChecked(Prefs.isAppLock(this));
        binding.switchSecure.setChecked(Prefs.isSecureWindow(this));
        updating = false;
        binding.tvThemeValue.setText(themeLabel());
        binding.tvCleanupValue.setText(cleanupLabel(Prefs.getCleanupDays(this)));
    }

    private void setSwitch(MaterialSwitch sw, boolean checked) {
        updating = true;
        sw.setChecked(checked);
        updating = false;
    }

    private void bindVersion() {
        String version = "1.0";
        try {
            version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception ignored) {
            // keep default
        }
        binding.tvVersion.setText(getString(R.string.version_format, version));
    }

    // ------------------------------------------------------------ dialogs

    private String[] themeLabels() {
        return new String[]{
                getString(R.string.theme_system),
                getString(R.string.theme_light),
                getString(R.string.theme_dark)};
    }

    private String themeLabel() {
        int mode = Prefs.getThemeMode(this);
        for (int i = 0; i < THEME_MODES.length; i++) {
            if (THEME_MODES[i] == mode) {
                return themeLabels()[i];
            }
        }
        return themeLabels()[0];
    }

    private void showThemeDialog() {
        int current = 0;
        int mode = Prefs.getThemeMode(this);
        for (int i = 0; i < THEME_MODES.length; i++) {
            if (THEME_MODES[i] == mode) {
                current = i;
            }
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.setting_theme)
                .setSingleChoiceItems(themeLabels(), current, (dialog, which) -> {
                    Prefs.setThemeMode(this, THEME_MODES[which]);
                    dialog.dismiss();
                    AppCompatDelegate.setDefaultNightMode(THEME_MODES[which]);
                    binding.tvThemeValue.setText(themeLabel());
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private String cleanupLabel(int days) {
        switch (days) {
            case 1:
                return getString(R.string.cleanup_1);
            case 7:
                return getString(R.string.cleanup_7);
            case 30:
                return getString(R.string.cleanup_30);
            case 90:
                return getString(R.string.cleanup_90);
            case 365:
                return getString(R.string.cleanup_365);
            default:
                return getString(R.string.cleanup_never);
        }
    }

    private void showCleanupDialog() {
        String[] labels = new String[CLEANUP_DAYS.length];
        int current = 0;
        int saved = Prefs.getCleanupDays(this);
        for (int i = 0; i < CLEANUP_DAYS.length; i++) {
            labels[i] = cleanupLabel(CLEANUP_DAYS[i]);
            if (CLEANUP_DAYS[i] == saved) {
                current = i;
            }
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.setting_cleanup)
                .setSingleChoiceItems(labels, current, (dialog, which) -> {
                    Prefs.setCleanupDays(this, CLEANUP_DAYS[which]);
                    ClipRepository.cleanupAsync(this);
                    binding.tvCleanupValue.setText(cleanupLabel(CLEANUP_DAYS[which]));
                    dialog.dismiss();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void confirmEnableMonitor() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.monitor_dialog_title)
                .setMessage(R.string.monitor_dialog_message)
                .setPositiveButton(R.string.enable, (dialog, which) -> {
                    requestNotificationPermissionIfNeeded();
                    Prefs.setBackgroundMonitor(this, true);
                    try {
                        ClipboardMonitorService.start(this);
                    } catch (Exception e) {
                        Prefs.setBackgroundMonitor(this, false);
                        setSwitch(binding.switchBgMonitor, false);
                    }
                })
                .setNegativeButton(android.R.string.cancel,
                        (dialog, which) -> setSwitch(binding.switchBgMonitor, false))
                .setOnCancelListener(dialog -> setSwitch(binding.switchBgMonitor, false))
                .show();
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }
}
