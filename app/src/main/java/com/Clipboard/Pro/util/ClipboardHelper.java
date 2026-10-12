package com.Clipboard.Pro.util;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.PersistableBundle;

/** Reading from / writing to the system clipboard, plus bookkeeping to avoid re-capturing. */
public final class ClipboardHelper {

    /** Value of ClipDescription.EXTRA_IS_SENSITIVE (API 33), as a string so it also compiles/runs below 33. */
    private static final String EXTRA_IS_SENSITIVE = "android.content.extra.IS_SENSITIVE";

    private static volatile String lastSelfText;
    private static volatile long lastSelfTime;

    public static final class Snapshot {
        public final String text;
        public final boolean sensitive;

        Snapshot(String text, boolean sensitive) {
            this.text = text;
            this.sensitive = sensitive;
        }
    }

    private ClipboardHelper() {
    }

    /** Returns the current primary clip as text, or null if empty, not text, or not readable right now. */
    public static Snapshot read(Context ctx) {
        try {
            ClipboardManager cm = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm == null) {
                return null;
            }
            ClipData clip = cm.getPrimaryClip();
            if (clip == null || clip.getItemCount() == 0) {
                return null;
            }
            ClipDescription desc = clip.getDescription();
            if (desc != null
                    && !(desc.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)
                    || desc.hasMimeType(ClipDescription.MIMETYPE_TEXT_HTML))) {
                return null;
            }
            CharSequence cs = clip.getItemAt(0).coerceToText(ctx);
            if (cs == null) {
                return null;
            }
            boolean sensitive = false;
            if (desc != null) {
                PersistableBundle extras = desc.getExtras();
                if (extras != null) {
                    sensitive = extras.getBoolean(EXTRA_IS_SENSITIVE, false);
                }
            }
            return new Snapshot(cs.toString(), sensitive);
        } catch (Exception e) {
            // SecurityException / DeadObjectException etc: the OS refused access right now.
            return null;
        }
    }

    /** Copies text to the system clipboard and remembers it so we don't re-capture our own copy. */
    public static boolean copy(Context ctx, String text, boolean sensitive) {
        try {
            ClipboardManager cm = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm == null) {
                return false;
            }
            ClipData clip = ClipData.newPlainText("ClipboardPro", text);
            if (sensitive) {
                PersistableBundle extras = new PersistableBundle();
                extras.putBoolean(EXTRA_IS_SENSITIVE, true);
                clip.getDescription().setExtras(extras);
            }
            lastSelfText = text;
            lastSelfTime = System.currentTimeMillis();
            Prefs.setLastClipSig(ctx, signature(text));
            cm.setPrimaryClip(clip);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** True if this text was put on the clipboard by ClipboardPro itself a moment ago. */
    public static boolean shouldIgnore(String text) {
        String last = lastSelfText;
        return last != null
                && last.equals(text)
                && System.currentTimeMillis() - lastSelfTime < 3000;
    }

    /** Remembers this clip; returns true if it differs from the previously seen one. */
    public static boolean markSeen(Context ctx, String text) {
        String sig = signature(text);
        if (sig.equals(Prefs.getLastClipSig(ctx))) {
            return false;
        }
        Prefs.setLastClipSig(ctx, sig);
        return true;
    }

    private static String signature(String text) {
        return text.length() + ":" + text.hashCode();
    }
}
