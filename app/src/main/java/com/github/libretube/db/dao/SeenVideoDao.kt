package com.github.libretube.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.github.libretube.db.obj.SeenVideo

@Dao
interface SeenVideoDao {
    @Query("SELECT videoId FROM seenVideo WHERE seenAt > :cutoff")
    suspend fun getSeenSince(cutoff: Long): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(items: List<SeenVideo>)

    @Query("DELETE FROM seenVideo WHERE seenAt < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long)
}
