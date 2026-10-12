package com.Clipboard.Pro.data;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import com.Clipboard.Pro.R;
import com.Clipboard.Pro.util.CategoryUtils;
import com.Clipboard.Pro.util.ClipboardHelper;
import com.Clipboard.Pro.util.Prefs;
import com.Clipboard.Pro.util.SensitiveDetector;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * All database work goes through here and runs on a single background thread, so callers (activities,
 * the monitor service) never block the UI thread and writes happen in order.
 */
public final class ClipRepository {

    public enum Result { SAVED, MOVED_TO_TOP, SKIPPED_SENSITIVE, SKIPPED_EMPTY, SKIPPED_SELF, ALREADY_SEEN }

    public interface Callback<T> {
        void onResult(T value);
    }

    private static final ExecutorService IO = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final int MAX_LENGTH = 100_000;
    private static final long DAY_MS = 24L * 60L * 60L * 1000L;

    private ClipRepository() {
    }

    public static LiveData<List<ClipItem>> observeAll(Context ctx) {
        return AppDatabase.get(ctx).clipDao().observeAll();
    }

    /** String resource for a user-facing message about this result, or 0 for "say nothing". */
    public static int messageFor(Result r) {
        switch (r) {
            case SAVED:
                return R.string.msg_saved;
            case MOVED_TO_TOP:
                return R.string.msg_moved_top;
            case SKIPPED_SENSITIVE:
                return R.string.msg_skipped_sensitive;
            case SKIPPED_EMPTY:
                return R.string.msg_clip_empty;
            default:
                return 0;
        }
    }

    // ---------------------------------------------------------------- capture

    /**
     * Reads the system clipboard on the calling thread (must be allowed to by Android) and saves it.
     *
     * @param onlyIfNew true = skip when it is the same clip we already processed (used on window focus)
     * @param auto      true = automatic capture (honours "skip sensitive"); false = user asked for it
     */
    public static void captureFromClipboard(Context ctx, boolean onlyIfNew, boolean auto,
                                            @Nullable Callback<Result> cb) {
        Context app = ctx.getApplicationContext();
        ClipboardHelper.Snapshot s = ClipboardHelper.read(app);
        if (s == null || s.text == null || s.text.trim().isEmpty()) {
            deliver(cb, Result.SKIPPED_EMPTY);
            return;
        }
        if (ClipboardHelper.shouldIgnore(s.text)) {
            deliver(cb, Result.SKIPPED_SELF);
            return;
        }
        boolean isNew = ClipboardHelper.markSeen(app, s.text);
        if (onlyIfNew && !isNew) {
            deliver(cb, Result.ALREADY_SEEN);
            return;
        }
        saveAsync(app, s.text, auto, s.sensitive, null, cb);
    }

    /** Saves text typed/shared by the user (never auto-skipped). category == null means auto-detect. */
    public static void addManualAsync(Context ctx, String text, @Nullable String category,
                                      @Nullable Callback<Result> cb) {
        saveAsync(ctx, text, false, false, category, cb);
    }

    private static void saveAsync(Context ctx, final String text, final boolean auto,
                                  final boolean systemSensitive, @Nullable final String category,
                                  @Nullable final Callback<Result> cb) {
        final Context app = ctx.getApplicationContext();
        IO.execute(() -> {
            Result r = saveSync(app, text, auto, systemSensitive, category);
            deliver(cb, r);
        });
    }

    private static Result saveSync(Context app, String raw, boolean auto, boolean systemSensitive,
                                   @Nullable String category) {
        if (raw == null || raw.trim().isEmpty()) {
            return Result.SKIPPED_EMPTY;
        }
        String text = raw.length() > MAX_LENGTH ? raw.substring(0, MAX_LENGTH) : raw;
        boolean sensitive = systemSensitive || SensitiveDetector.looksSensitive(text);
        if (sensitive && auto && Prefs.isSkipSensitive(app)) {
            return Result.SKIPPED_SENSITIVE;
        }
        ClipDao dao = AppDatabase.get(app).clipDao();
        long now = System.currentTimeMillis();

        ClipItem existing = dao.findByText(text);
        if (existing != null) {
            // Duplicate: keep a single entry, move it to the top and keep pin/favorite state.
            existing.timestamp = now;
            if (sensitive) {
                existing.sensitive = true;
            }
            if (category != null) {
                existing.category = category;
            }
            dao.update(existing);
            return Result.MOVED_TO_TOP;
        }

        ClipItem item = new ClipItem();
        item.text = text;
        item.category = category != null ? category : CategoryUtils.detect(text);
        item.timestamp = now;
        item.sensitive = sensitive;
        dao.insert(item);
        applyCleanup(app, dao);
        return Result.SAVED;
    }

    // ---------------------------------------------------------------- edit / state

    public static void editAsync(Context ctx, final long id, final String newText,
                                 final String category, @Nullable final Callback<Result> cb) {
        final Context app = ctx.getApplicationContext();
        IO.execute(() -> {
            ClipDao dao = AppDatabase.get(app).clipDao();
            ClipItem cur = dao.findById(id);
            if (cur == null) {
                deliver(cb, Result.SKIPPED_EMPTY);
                return;
            }
            String t = newText.length() > MAX_LENGTH ? newText.substring(0, MAX_LENGTH) : newText;
            ClipItem other = dao.findByText(t);
            if (other != null && other.id != id) {
                // Edited text now equals another entry: merge into that one.
                other.timestamp = System.currentTimeMillis();
                other.pinned = other.pinned || cur.pinned;
                other.favorite = other.favorite || cur.favorite;
                dao.update(other);
                dao.deleteByIds(Collections.singletonList(id));
                deliver(cb, Result.MOVED_TO_TOP);
                return;
            }
            if (!cur.text.equals(t)) {
                cur.text = t;
                cur.sensitive = SensitiveDetector.looksSensitive(t);
            }
            cur.category = category;
            dao.update(cur);
            deliver(cb, Result.SAVED);
        });
    }

    public static void setPinnedAsync(Context ctx, final List<Long> ids, final boolean pinned) {
        final Context app = ctx.getApplicationContext();
        IO.execute(() -> AppDatabase.get(app).clipDao().setPinned(ids, pinned));
    }

    public static void setFavoriteAsync(Context ctx, final List<Long> ids, final boolean favorite) {
        final Context app = ctx.getApplicationContext();
        IO.execute(() -> AppDatabase.get(app).clipDao().setFavorite(ids, favorite));
    }

    public static void deleteAsync(Context ctx, final List<Long> ids) {
        final Context app = ctx.getApplicationContext();
        IO.execute(() -> AppDatabase.get(app).clipDao().deleteByIds(ids));
    }

    public static void restoreAsync(Context ctx, final List<ClipItem> items) {
        final Context app = ctx.getApplicationContext();
        IO.execute(() -> AppDatabase.get(app).clipDao().insertAll(items));
    }

    public static void clearAsync(Context ctx, final boolean keepPinned, @Nullable final Runnable onDone) {
        final Context app = ctx.getApplicationContext();
        IO.execute(() -> {
            ClipDao dao = AppDatabase.get(app).clipDao();
            if (keepPinned) {
                dao.deleteUnpinned();
            } else {
                dao.deleteAll();
            }
            if (onDone != null) {
                MAIN.post(onDone);
            }
        });
    }

    // ---------------------------------------------------------------- cleanup

    public static void cleanupAsync(Context ctx) {
        final Context app = ctx.getApplicationContext();
        IO.execute(() -> applyCleanup(app, AppDatabase.get(app).clipDao()));
    }

    private static void applyCleanup(Context app, ClipDao dao) {
        int days = Prefs.getCleanupDays(app);
        if (days > 0) {
            dao.deleteOlderThan(System.currentTimeMillis() - days * DAY_MS);
        }
    }

    // ---------------------------------------------------------------- helpers

    private static void deliver(@Nullable final Callback<Result> cb, final Result r) {
        if (cb == null) {
            return;
        }
        if (Looper.myLooper() == Looper.getMainLooper()) {
            cb.onResult(r);
        } else {
            MAIN.post(() -> cb.onResult(r));
        }
    }
}
