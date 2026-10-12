package com.Clipboard.Pro.ui;

import com.Clipboard.Pro.data.ClipItem;

/** One row in the list: either a section header or a clip. */
public final class Row {

    public static final int TYPE_HEADER = 0;
    public static final int TYPE_ITEM = 1;

    public final int type;
    public final String key;
    public final String header;
    public final ClipItem item;

    private Row(int type, String key, String header, ClipItem item) {
        this.type = type;
        this.key = key;
        this.header = header;
        this.item = item;
    }

    public static Row header(String title) {
        return new Row(TYPE_HEADER, "h:" + title, title, null);
    }

    public static Row item(ClipItem item) {
        return new Row(TYPE_ITEM, "i:" + item.id, null, item);
    }

    public boolean sameContent(Row other) {
        if (type != other.type) {
            return false;
        }
        if (type == TYPE_HEADER) {
            return header.equals(other.header);
        }
        return item.contentEquals(other.item);
    }
}
