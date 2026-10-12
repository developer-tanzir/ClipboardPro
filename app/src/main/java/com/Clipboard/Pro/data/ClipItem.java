package com.Clipboard.Pro.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "clips")
public class ClipItem {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    public String text = "";

    @NonNull
    public String category = "General";

    /** Last time this text was copied / saved (milliseconds since epoch). */
    public long timestamp;

    public boolean pinned;
    public boolean favorite;
    public boolean sensitive;

    public boolean contentEquals(ClipItem o) {
        return o != null
                && id == o.id
                && timestamp == o.timestamp
                && pinned == o.pinned
                && favorite == o.favorite
                && sensitive == o.sensitive
                && text.equals(o.text)
                && category.equals(o.category);
    }
}
