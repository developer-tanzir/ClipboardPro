package com.Clipboard.Pro.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.Clipboard.Pro.data.ClipRepository;

/**
 * Invisible activity with three jobs:
 * 1. ACTION_CAPTURE (notification button / app shortcut): read the clipboard once it has focus,
 *    which is what Android 10+ requires.
 * 2. ACTION_SEND: "Share -> Save to ClipboardPro".
 * 3. ACTION_PROCESS_TEXT: "Select text -> Save to ClipboardPro".
 */
public class CaptureActivity extends Activity {

    public static final String ACTION_CAPTURE = "com.Clipboard.Pro.action.CAPTURE";

    private boolean handled;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        String action = intent == null ? null : intent.getAction();
        if (Intent.ACTION_SEND.equals(action)) {
            handled = true;
            saveText(intent.getCharSequenceExtra(Intent.EXTRA_TEXT));
        } else if (Intent.ACTION_PROCESS_TEXT.equals(action)) {
            handled = true;
            saveText(intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT));
        } else if (!ACTION_CAPTURE.equals(action)) {
            handled = true;
            finish();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && !handled) {
            handled = true;
            // The window now has focus, so Android allows us to read the clipboard.
            ClipRepository.captureFromClipboard(this, false, false, result -> {
                int msg = ClipRepository.messageFor(result);
                if (msg != 0) {
                    Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
                }
            });
            finish();
            overridePendingTransition(0, 0);
        }
    }

    private void saveText(CharSequence text) {
        if (text == null || text.toString().trim().isEmpty()) {
            Toast.makeText(getApplicationContext(),
                    com.Clipboard.Pro.R.string.msg_clip_empty, Toast.LENGTH_SHORT).show();
        } else {
            ClipRepository.addManualAsync(this, text.toString(), null, result -> {
                int msg = ClipRepository.messageFor(result);
                if (msg != 0) {
                    Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
                }
            });
        }
        finish();
    }
}
