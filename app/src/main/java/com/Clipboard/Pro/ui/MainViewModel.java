package com.Clipboard.Pro.ui;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.Clipboard.Pro.R;
import com.Clipboard.Pro.data.ClipItem;
import com.Clipboard.Pro.data.ClipRepository;
import com.Clipboard.Pro.util.Prefs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Holds search / filter / sort state and turns the raw database list into display rows. */
public class MainViewModel extends AndroidViewModel {

    public static final String FILTER_ALL = "";
    public static final String FILTER_FAVORITES = "__favorites__";

    private final LiveData<List<ClipItem>> all;
    private final MutableLiveData<String> query = new MutableLiveData<>("");
    private final MutableLiveData<String> filter = new MutableLiveData<>(FILTER_ALL);
    private final MutableLiveData<Boolean> newestFirst = new MutableLiveData<>(true);
    private final MutableLiveData<Boolean> hasItems = new MutableLiveData<>(false);
    private final MediatorLiveData<List<Row>> rows = new MediatorLiveData<>();

    public MainViewModel(@NonNull Application application) {
        super(application);
        newestFirst.setValue(Prefs.isSortNewest(application));
        all = ClipRepository.observeAll(application);
        rows.addSource(all, list -> recompute());
        rows.addSource(query, q -> recompute());
        rows.addSource(filter, f -> recompute());
        rows.addSource(newestFirst, n -> recompute());
    }

    public LiveData<List<Row>> getRows() {
        return rows;
    }

    public LiveData<Boolean> getHasItems() {
        return hasItems;
    }

    public boolean isNewestFirst() {
        return Boolean.TRUE.equals(newestFirst.getValue());
    }

    public void setQuery(String q) {
        query.setValue(q == null ? "" : q);
    }

    public void setFilter(String f) {
        filter.setValue(f == null ? FILTER_ALL : f);
    }

    public void setNewestFirst(boolean newest) {
        Prefs.setSortNewest(getApplication(), newest);
        newestFirst.setValue(newest);
    }

    private void recompute() {
        List<ClipItem> source = all.getValue();
        if (source == null) {
            return;
        }
        hasItems.setValue(!source.isEmpty());

        String q = query.getValue() == null ? "" : query.getValue().trim().toLowerCase(Locale.ROOT);
        String f = filter.getValue() == null ? FILTER_ALL : filter.getValue();
        final boolean newest = isNewestFirst();

        List<ClipItem> pinned = new ArrayList<>();
        List<ClipItem> others = new ArrayList<>();
        for (ClipItem item : source) {
            if (FILTER_FAVORITES.equals(f)) {
                if (!item.favorite) {
                    continue;
                }
            } else if (!FILTER_ALL.equals(f) && !f.equals(item.category)) {
                continue;
            }
            if (!q.isEmpty() && !item.text.toLowerCase(Locale.ROOT).contains(q)) {
                continue;
            }
            if (item.pinned) {
                pinned.add(item);
            } else {
                others.add(item);
            }
        }

        Comparator<ClipItem> byTime = (a, b) -> newest
                ? Long.compare(b.timestamp, a.timestamp)
                : Long.compare(a.timestamp, b.timestamp);
        Collections.sort(pinned, byTime);
        Collections.sort(others, byTime);

        List<Row> result = new ArrayList<>();
        if (!pinned.isEmpty()) {
            result.add(Row.header(getApplication().getString(R.string.header_pinned)));
            for (ClipItem item : pinned) {
                result.add(Row.item(item));
            }
        }
        if (!others.isEmpty()) {
            if (!pinned.isEmpty()) {
                result.add(Row.header(getApplication().getString(R.string.header_history)));
            }
            for (ClipItem item : others) {
                result.add(Row.item(item));
            }
        }
        rows.setValue(result);
    }
}
