package com.Clipboard.Pro.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.Clipboard.Pro.R;
import com.Clipboard.Pro.data.ClipItem;
import com.Clipboard.Pro.databinding.ItemClipBinding;
import com.Clipboard.Pro.databinding.ItemHeaderBinding;
import com.google.android.material.color.MaterialColors;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ClipAdapter extends ListAdapter<Row, RecyclerView.ViewHolder> {

    public interface Listener {
        void onItemClick(ClipItem item);

        void onItemLongClick(ClipItem item);

        void onCopy(ClipItem item);

        void onFavorite(ClipItem item);

        void onShare(ClipItem item);

        void onMore(ClipItem item, View anchor);
    }

    public interface SelectionListener {
        void onSelectionChanged(int count);
    }

    private static final DiffUtil.ItemCallback<Row> DIFF = new DiffUtil.ItemCallback<Row>() {
        @Override
        public boolean areItemsTheSame(@NonNull Row a, @NonNull Row b) {
            return a.key.equals(b.key);
        }

        @Override
        public boolean areContentsTheSame(@NonNull Row a, @NonNull Row b) {
            return a.sameContent(b);
        }
    };

    private static final int PREVIEW_CHARS = 600;

    private final Listener listener;
    private SelectionListener selectionListener;
    private final Set<Long> selected = new HashSet<>();
    private boolean selectionMode;

    public ClipAdapter(Listener listener) {
        super(DIFF);
        this.listener = listener;
    }

    public void setSelectionListener(SelectionListener l) {
        this.selectionListener = l;
    }

    // ------------------------------------------------------------ selection

    public boolean isSelectionMode() {
        return selectionMode;
    }

    public void setSelectionMode(boolean enabled) {
        if (selectionMode != enabled) {
            selectionMode = enabled;
            notifyDataSetChanged();
        }
    }

    public void toggle(long id) {
        if (!selected.remove(id)) {
            selected.add(id);
        }
        List<Row> list = getCurrentList();
        for (int i = 0; i < list.size(); i++) {
            Row r = list.get(i);
            if (r.item != null && r.item.id == id) {
                notifyItemChanged(i);
                break;
            }
        }
        notifySelection();
    }

    public void selectAll() {
        for (Row r : getCurrentList()) {
            if (r.item != null) {
                selected.add(r.item.id);
            }
        }
        notifyDataSetChanged();
        notifySelection();
    }

    public void clearSelection() {
        if (!selected.isEmpty()) {
            selected.clear();
            notifyDataSetChanged();
            notifySelection();
        }
    }

    public List<ClipItem> getSelectedItems() {
        List<ClipItem> out = new ArrayList<>();
        for (Row r : getCurrentList()) {
            if (r.item != null && selected.contains(r.item.id)) {
                out.add(r.item);
            }
        }
        return out;
    }

    private void notifySelection() {
        if (selectionListener != null) {
            selectionListener.onSelectionChanged(selected.size());
        }
    }

    @Override
    public void onCurrentListChanged(@NonNull List<Row> previousList, @NonNull List<Row> currentList) {
        if (selected.isEmpty()) {
            return;
        }
        Set<Long> present = new HashSet<>();
        for (Row r : currentList) {
            if (r.item != null) {
                present.add(r.item.id);
            }
        }
        if (selected.retainAll(present)) {
            notifySelection();
        }
    }

    // ------------------------------------------------------------ adapter

    @Override
    public int getItemViewType(int position) {
        return getItem(position).type;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == Row.TYPE_HEADER) {
            return new HeaderHolder(ItemHeaderBinding.inflate(inflater, parent, false));
        }
        return new ClipHolder(ItemClipBinding.inflate(inflater, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Row row = getItem(position);
        if (holder instanceof HeaderHolder) {
            ((HeaderHolder) holder).binding.tvHeader.setText(row.header);
        } else if (holder instanceof ClipHolder) {
            ((ClipHolder) holder).bind(row.item);
        }
    }

    private static String preview(String s) {
        return s.length() > PREVIEW_CHARS ? s.substring(0, PREVIEW_CHARS) + "…" : s;
    }

    // ------------------------------------------------------------ holders

    private class HeaderHolder extends RecyclerView.ViewHolder {
        final ItemHeaderBinding binding;

        HeaderHolder(ItemHeaderBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    private class ClipHolder extends RecyclerView.ViewHolder {
        final ItemClipBinding b;

        ClipHolder(ItemClipBinding binding) {
            super(binding.getRoot());
            this.b = binding;
            b.card.setOnClickListener(v -> {
                ClipItem it = current();
                if (it != null) {
                    listener.onItemClick(it);
                }
            });
            b.card.setOnLongClickListener(v -> {
                ClipItem it = current();
                if (it != null) {
                    listener.onItemLongClick(it);
                }
                return true;
            });
            b.btnCopy.setOnClickListener(v -> {
                ClipItem it = current();
                if (it != null) {
                    listener.onCopy(it);
                }
            });
            b.btnFavorite.setOnClickListener(v -> {
                ClipItem it = current();
                if (it != null) {
                    listener.onFavorite(it);
                }
            });
            b.btnShare.setOnClickListener(v -> {
                ClipItem it = current();
                if (it != null) {
                    listener.onShare(it);
                }
            });
            b.btnMore.setOnClickListener(v -> {
                ClipItem it = current();
                if (it != null) {
                    listener.onMore(it, v);
                }
            });
        }

        private ClipItem current() {
            int pos = getBindingAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) {
                return null;
            }
            return getItem(pos).item;
        }

        void bind(ClipItem item) {
            Context c = b.getRoot().getContext();
            b.tvCategory.setText(item.category);
            b.tvTime.setText(DateUtils.getRelativeTimeSpanString(
                    item.timestamp, System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE));
            b.ivPinned.setVisibility(item.pinned ? View.VISIBLE : View.GONE);
            b.tvText.setText(item.sensitive
                    ? c.getString(R.string.sensitive_hidden)
                    : preview(item.text));
            b.card.setChecked(selected.contains(item.id));
            b.actions.setVisibility(selectionMode ? View.GONE : View.VISIBLE);

            int inactive = MaterialColors.getColor(b.getRoot(),
                    com.google.android.material.R.attr.colorOnSurfaceVariant);
            // The vector has its own tint, so override it via the image tint list (not a color filter).
            b.btnFavorite.setImageTintList(ColorStateList.valueOf(item.favorite
                    ? ContextCompat.getColor(c, R.color.star_gold)
                    : inactive));
        }
    }
}
