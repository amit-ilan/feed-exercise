package com.lightricks.feedexercise.database

import androidx.lifecycle.LiveData
import androidx.room.*
import io.reactivex.Completable

@Dao
interface FeedItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(items: List<FeedItemEntity>): Completable

    @Query("DELETE FROM FeedItemEntity")
    fun deleteAll(): Completable

    @Query("SELECT * FROM FeedItemEntity")
    fun getAll(): LiveData<List<FeedItemEntity>>

    @Query("SELECT COUNT(*) FROM FeedItemEntity")
    fun getCount(): Int
}

