package com.Clipboard.Pro.ui;

import android.content.Context;

import androidx.annotation.Nullable;

import com.Clipboard.Pro.R;
import com.Clipboard.Pro.data.ClipRepository;
import com.Clipboard.Pro.util.Prefs;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/** Dialogs shared between the main screen and Settings. */
public final class Dialogs {

    private Dialogs() {
    }

    public static void confirmClear(final Context ctx, @Nullable final Runnable onDone) {
        final boolean[] keepPinned = {true};
        new MaterialAlertDialogBuilder(ctx)
                .setTitle(R.string.clear_history_title)
                .setMultiChoiceItems(
                        new CharSequence[]{ctx.getString(R.string.keep_pinned)},
                        keepPinned,
                        (dialog, which, isChecked) -> keepPinned[0] = isChecked)
                .setPositiveButton(R.string.clear,
                        (dialog, which) -> ClipRepository.clearAsync(ctx, keepPinned[0], onDone))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    public static void showClipboardInfo(Context ctx) {
        Prefs.setInfoShown(ctx, true);
        new MaterialAlertDialogBuilder(ctx)
                .setTitle(R.string.info_title)
                .setMessage(R.string.info_message)
                .setPositiveButton(R.string.got_it, null)
                .show();
    }
}
