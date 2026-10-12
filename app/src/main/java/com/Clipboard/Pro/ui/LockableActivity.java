package com.Clipboard.Pro.ui;

import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import com.Clipboard.Pro.R;
import com.Clipboard.Pro.util.AppLock;
import com.Clipboard.Pro.util.Prefs;

/**
 * Base class for screens that show clipboard data. Adds:
 * - the optional app lock (cover view + BiometricPrompt with device-credential fallback)
 * - the optional "hide in recent apps / block screenshots" flag
 */
public abstract class LockableActivity extends AppCompatActivity {

    private View lockOverlay;
    private boolean prompting;

    @Override
    protected void onStart() {
        super.onStart();
        if (Prefs.isSecureWindow(this)) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }
        if (AppLock.isLockRequired(this)) {
            showLock();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        AppLock.touch();
    }

    /** Called after the app lock was passed (or became unnecessary). */
    protected void onUnlocked() {
    }

    private void showLock() {
        if (lockOverlay == null) {
            ViewGroup root = findViewById(android.R.id.content);
            lockOverlay = getLayoutInflater().inflate(R.layout.view_lock, root, false);
            lockOverlay.findViewById(R.id.btn_unlock).setOnClickListener(v -> promptAuth());
            root.addView(lockOverlay);
        }
        promptAuth();
    }

    private void hideLock() {
        if (lockOverlay != null && lockOverlay.getParent() instanceof ViewGroup) {
            ((ViewGroup) lockOverlay.getParent()).removeView(lockOverlay);
        }
        lockOverlay = null;
    }

    private void promptAuth() {
        if (prompting) {
            return;
        }
        int can = BiometricManager.from(this).canAuthenticate(AppLock.AUTHENTICATORS);
        if (can == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED
                || can == BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE) {
            // The user removed their screen lock: don't lock them out of their own data forever.
            Prefs.setAppLock(this, false);
            Toast.makeText(this, R.string.lock_unavailable, Toast.LENGTH_LONG).show();
            hideLock();
            onUnlocked();
            return;
        }
        if (can != BiometricManager.BIOMETRIC_SUCCESS) {
            Toast.makeText(this, R.string.lock_error, Toast.LENGTH_SHORT).show();
            return;
        }

        prompting = true;
        BiometricPrompt prompt = new BiometricPrompt(this, ContextCompat.getMainExecutor(this),
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                        prompting = false;
                        AppLock.markUnlocked();
                        hideLock();
                        onUnlocked();
                    }

                    @Override
                    public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                        // Cancelled or locked out: stay on the lock screen; the Unlock button retries.
                        prompting = false;
                    }
                });
        BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.unlock_prompt_title))
                .setSubtitle(getString(R.string.unlock_prompt_subtitle))
                .setAllowedAuthenticators(AppLock.AUTHENTICATORS)
                .build();
        prompt.authenticate(info);
    }
}
