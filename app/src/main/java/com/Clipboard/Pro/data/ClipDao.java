package com.Clipboard.Pro.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface ClipDao {

    @Insert
    long insert(ClipItem item);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<ClipItem> items);

    @Update
    void update(ClipItem item);

    @Query("SELECT * FROM clips")
    LiveData<List<ClipItem>> observeAll();

    @Query("SELECT * FROM clips WHERE text = :text LIMIT 1")
    ClipItem findByText(String text);

    @Query("SELECT * FROM clips WHERE id = :id LIMIT 1")
    ClipItem findById(long id);

    @Query("DELETE FROM clips WHERE id IN (:ids)")
    void deleteByIds(List<Long> ids);

    @Query("DELETE FROM clips WHERE pinned = 0")
    void deleteUnpinned();

    @Query("DELETE FROM clips")
    void deleteAll();

    @Query("DELETE FROM clips WHERE timestamp < :cutoff AND pinned = 0 AND favorite = 0")
    int deleteOlderThan(long cutoff);

    @Query("UPDATE clips SET pinned = :pinned WHERE id IN (:ids)")
    void setPinned(List<Long> ids, boolean pinned);

    @Query("UPDATE clips SET favorite = :favorite WHERE id IN (:ids)")
    void setFavorite(List<Long> ids, boolean favorite);
}
