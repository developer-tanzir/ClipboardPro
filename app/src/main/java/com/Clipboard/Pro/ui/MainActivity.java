package com.Clipboard.Pro.ui;

import android.content.ClipboardManager;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ActionMode;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.Clipboard.Pro.R;
import com.Clipboard.Pro.data.ClipItem;
import com.Clipboard.Pro.data.ClipRepository;
import com.Clipboard.Pro.databinding.ActivityMainBinding;
import com.Clipboard.Pro.databinding.DialogEditClipBinding;
import com.Clipboard.Pro.service.ClipboardMonitorService;
import com.Clipboard.Pro.util.AppLock;
import com.Clipboard.Pro.util.CategoryUtils;
import com.Clipboard.Pro.util.ClipboardHelper;
import com.Clipboard.Pro.util.Prefs;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MainActivity extends LockableActivity implements ClipAdapter.Listener {

    private ActivityMainBinding binding;
    private MainViewModel vm;
    private ClipAdapter adapter;
    private ActionMode actionMode;
    private ClipboardManager clipboardManager;
    private boolean clipListenerRegistered;

    private boolean rowsEmpty;
    private boolean hasItems;

    /** Fires while the app is running and Android lets us see clipboard changes. */
    private final ClipboardManager.OnPrimaryClipChangedListener clipListener = () ->
            ClipRepository.captureFromClipboard(getApplicationContext(), false, true, null);

    // ================================================================ lifecycle

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);

        clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        vm = new ViewModelProvider(this).get(MainViewModel.class);

        adapter = new ClipAdapter(this);
        adapter.setSelectionListener(this::onSelectionChanged);
        binding.recycler.setLayoutManager(new LinearLayoutManager(this));
        binding.recycler.setAdapter(adapter);
        binding.recycler.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                if (dy > 8) {
                    binding.fab.shrink();
                } else if (dy < -8) {
                    binding.fab.extend();
                }
            }
        });

        vm.getRows().observe(this, rows -> {
            rowsEmpty = rows == null || rows.isEmpty();
            adapter.submitList(rows);
            renderEmptyState();
        });
        vm.getHasItems().observe(this, has -> {
            hasItems = Boolean.TRUE.equals(has);
            renderEmptyState();
        });

        binding.searchEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                vm.setQuery(s == null ? "" : s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        setupChips();
        binding.fab.setOnClickListener(v -> showEditDialog(null));

        if (!Prefs.isInfoShown(this)) {
            binding.getRoot().post(() -> {
                if (!isFinishing()) {
                    Dialogs.showClipboardInfo(this);
                }
            });
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        ClipRepository.cleanupAsync(this);
        if (!clipListenerRegistered && clipboardManager != null) {
            clipboardManager.addPrimaryClipChangedListener(clipListener);
            clipListenerRegistered = true;
        }
        if (Prefs.isBackgroundMonitor(this)) {
            try {
                ClipboardMonitorService.start(this);
            } catch (Exception ignored) {
                // Not allowed to start right now; the user can toggle it again in Settings.
            }
        }
    }

    @Override
    protected void onStop() {
        if (clipListenerRegistered && clipboardManager != null) {
            clipboardManager.removePrimaryClipChangedListener(clipListener);
            clipListenerRegistered = false;
        }
        super.onStop();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            captureIfAllowed();
        }
    }

    @Override
    protected void onUnlocked() {
        if (hasWindowFocus()) {
            captureIfAllowed();
        }
    }

    /** Android 10+ only lets the focused app read the clipboard, so this is the main capture path. */
    private void captureIfAllowed() {
        if (Prefs.isCaptureOnOpen(this) && !AppLock.isLockRequired(this)) {
            ClipRepository.captureFromClipboard(this, true, true, null);
        }
    }

    // ================================================================ UI helpers

    private void setupChips() {
        addChip(getString(R.string.filter_all), MainViewModel.FILTER_ALL, true);
        addChip(getString(R.string.filter_favorites), MainViewModel.FILTER_FAVORITES, false);
        for (String category : CategoryUtils.ALL) {
            addChip(category, category, false);
        }
        binding.chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                return;
            }
            Chip chip = group.findViewById(checkedIds.get(0));
            if (chip != null && chip.getTag() instanceof String) {
                vm.setFilter((String) chip.getTag());
            }
        });
    }

    private void addChip(String label, String key, boolean checked) {
        Chip chip = (Chip) getLayoutInflater().inflate(R.layout.item_filter_chip, binding.chipGroup, false);
        chip.setId(View.generateViewId());
        chip.setText(label);
        chip.setTag(key);
        chip.setChecked(checked);
        binding.chipGroup.addView(chip);
    }

    private void renderEmptyState() {
        binding.emptyView.setVisibility(rowsEmpty ? View.VISIBLE : View.GONE);
        binding.recycler.setVisibility(rowsEmpty ? View.INVISIBLE : View.VISIBLE);
        binding.emptyTitle.setText(hasItems ? R.string.empty_no_results_title : R.string.empty_title);
        binding.emptyMessage.setText(hasItems ? R.string.empty_no_results_message : R.string.empty_message);
    }

    private void snack(String message) {
        Snackbar.make(binding.coordinator, message, Snackbar.LENGTH_SHORT)
                .setAnchorView(binding.fab)
                .show();
    }

    private void snackResult(ClipRepository.Result result) {
        int res = ClipRepository.messageFor(result);
        if (res != 0) {
            snack(getString(res));
        }
    }

    // ================================================================ menu

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem newest = menu.findItem(R.id.sort_newest);
        MenuItem oldest = menu.findItem(R.id.sort_oldest);
        if (newest != null && oldest != null) {
            newest.setChecked(vm.isNewestFirst());
            oldest.setChecked(!vm.isNewestFirst());
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.sort_newest) {
            item.setChecked(true);
            vm.setNewestFirst(true);
            return true;
        } else if (id == R.id.sort_oldest) {
            item.setChecked(true);
            vm.setNewestFirst(false);
            return true;
        } else if (id == R.id.action_capture) {
            ClipRepository.captureFromClipboard(this, false, false, this::snackResult);
            return true;
        } else if (id == R.id.action_clear) {
            Dialogs.confirmClear(this, () -> snack(getString(R.string.msg_history_cleared)));
            return true;
        } else if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        } else if (id == R.id.action_info) {
            Dialogs.showClipboardInfo(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // ================================================================ list callbacks

    @Override
    public void onItemClick(ClipItem item) {
        if (adapter.isSelectionMode()) {
            adapter.toggle(item.id);
        } else {
            copyItem(item);
        }
    }

    @Override
    public void onItemLongClick(ClipItem item) {
        if (!adapter.isSelectionMode()) {
            actionMode = startSupportActionMode(actionModeCallback);
        }
        adapter.toggle(item.id);
    }

    @Override
    public void onCopy(ClipItem item) {
        copyItem(item);
    }

    @Override
    public void onFavorite(ClipItem item) {
        ClipRepository.setFavoriteAsync(this, Collections.singletonList(item.id), !item.favorite);
    }

    @Override
    public void onShare(ClipItem item) {
        shareText(item.text);
    }

    @Override
    public void onMore(final ClipItem item, View anchor) {
        androidx.appcompat.widget.PopupMenu popup = new androidx.appcompat.widget.PopupMenu(this, anchor);
        popup.getMenuInflater().inflate(R.menu.item_menu, popup.getMenu());
        popup.getMenu().findItem(R.id.menu_pin).setTitle(item.pinned ? R.string.unpin : R.string.pin);
        popup.setOnMenuItemClickListener(mi -> {
            int id = mi.getItemId();
            if (id == R.id.menu_pin) {
                ClipRepository.setPinnedAsync(this, Collections.singletonList(item.id), !item.pinned);
                return true;
            } else if (id == R.id.menu_edit) {
                showEditDialog(item);
                return true;
            } else if (id == R.id.menu_delete) {
                deleteWithUndo(Collections.singletonList(item));
                return true;
            }
            return false;
        });
        popup.show();
    }

    // ================================================================ actions

    private void copyItem(ClipItem item) {
        if (ClipboardHelper.copy(this, item.text, item.sensitive)) {
            snack(getString(R.string.copied));
        }
    }

    private void shareText(String text) {
        if (text == null || text.isEmpty()) {
            snack(getString(R.string.msg_share_failed));
            return;
        }
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_TEXT, text);
        startActivity(Intent.createChooser(send, getString(R.string.share)));
    }

    private static List<Long> idsOf(List<ClipItem> items) {
        List<Long> ids = new ArrayList<>();
        for (ClipItem i : items) {
            ids.add(i.id);
        }
        return ids;
    }

    private static String joinTexts(List<ClipItem> items) {
        StringBuilder sb = new StringBuilder();
        for (ClipItem i : items) {
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append(i.text);
        }
        return sb.toString();
    }

    private void deleteWithUndo(final List<ClipItem> items) {
        if (items.isEmpty()) {
            return;
        }
        ClipRepository.deleteAsync(this, idsOf(items));
        String message = getResources().getQuantityString(R.plurals.deleted_n, items.size(), items.size());
        Snackbar.make(binding.coordinator, message, Snackbar.LENGTH_LONG)
                .setAnchorView(binding.fab)
                .setAction(R.string.undo, v -> ClipRepository.restoreAsync(this, items))
                .show();
    }

    // ================================================================ add / edit dialog

    private void showEditDialog(@Nullable final ClipItem item) {
        final DialogEditClipBinding d = DialogEditClipBinding.inflate(getLayoutInflater());
        final boolean isNew = item == null;
        final String autoLabel = getString(R.string.category_auto);

        List<String> options = new ArrayList<>();
        if (isNew) {
            options.add(autoLabel);
        }
        options.addAll(Arrays.asList(CategoryUtils.ALL));
        d.ddCategory.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, options));
        d.ddCategory.setText(isNew ? autoLabel : item.category, false);
        if (!isNew) {
            d.etText.setText(item.text);
        }

        final AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(isNew ? R.string.add_clip_title : R.string.edit_clip_title)
                .setView(d.getRoot())
                .setPositiveButton(R.string.save, null)
                .setNegativeButton(android.R.string.cancel, null)
                .create();

        dialog.setOnShowListener(di -> {
            d.etText.requestFocus();
            d.etText.setSelection(d.etText.length());
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String text = d.etText.getText() == null ? "" : d.etText.getText().toString();
                if (text.trim().isEmpty()) {
                    d.tilText.setError(getString(R.string.error_empty));
                    return;
                }
                d.tilText.setError(null);
                String chosen = d.ddCategory.getText() == null ? "" : d.ddCategory.getText().toString();
                if (isNew) {
                    String category = (chosen.isEmpty() || chosen.equals(autoLabel)) ? null : chosen;
                    ClipRepository.addManualAsync(this, text, category, this::snackResult);
                } else {
                    String category = chosen.isEmpty() ? item.category : chosen;
                    ClipRepository.editAsync(this, item.id, text, category, this::snackResult);
                }
                dialog.dismiss();
            });
        });
        if (dialog.getWindow() != null) {
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }
        dialog.show();
    }

    // ================================================================ multi-select

    private void onSelectionChanged(int count) {
        if (actionMode == null) {
            return;
        }
        if (count == 0) {
            actionMode.finish();
        } else {
            actionMode.setTitle(getString(R.string.n_selected, count));
        }
    }

    private final ActionMode.Callback actionModeCallback = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate(R.menu.selection_menu, menu);
            adapter.setSelectionMode(true);
            binding.fab.hide();
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem menuItem) {
            int id = menuItem.getItemId();
            List<ClipItem> selected = adapter.getSelectedItems();
            if (id == R.id.sel_select_all) {
                adapter.selectAll();
                return true;
            }
            if (selected.isEmpty()) {
                return true;
            }
            if (id == R.id.sel_copy) {
                if (ClipboardHelper.copy(MainActivity.this, joinTexts(selected), false)) {
                    snack(getString(R.string.copied));
                }
                mode.finish();
                return true;
            } else if (id == R.id.sel_share) {
                shareText(joinTexts(selected));
                mode.finish();
                return true;
            } else if (id == R.id.sel_pin) {
                boolean allPinned = true;
                for (ClipItem i : selected) {
                    if (!i.pinned) {
                        allPinned = false;
                        break;
                    }
                }
                ClipRepository.setPinnedAsync(MainActivity.this, idsOf(selected), !allPinned);
                mode.finish();
                return true;
            } else if (id == R.id.sel_favorite) {
                boolean allFav = true;
                for (ClipItem i : selected) {
                    if (!i.favorite) {
                        allFav = false;
                        break;
                    }
                }
                ClipRepository.setFavoriteAsync(MainActivity.this, idsOf(selected), !allFav);
                mode.finish();
                return true;
            } else if (id == R.id.sel_delete) {
                List<ClipItem> toDelete = new ArrayList<>(selected);
                mode.finish();
                deleteWithUndo(toDelete);
                return true;
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            actionMode = null;
            adapter.setSelectionMode(false);
            adapter.clearSelection();
            binding.fab.show();
        }
    };
}
