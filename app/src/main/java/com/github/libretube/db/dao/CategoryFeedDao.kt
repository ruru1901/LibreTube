package com.github.libretube.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.github.libretube.db.obj.CachedCategoryFeedItem

@Dao
interface CategoryFeedDao {

    @Query("""
        SELECT * FROM categoryFeedCache 
        WHERE categoryId = :cat AND languageCode = :lang 
        ORDER BY score DESC LIMIT :limit
    """)
    suspend fun getForQuery(cat: String, lang: String, limit: Int = 20): List<CachedCategoryFeedItem>

    @Query("SELECT MAX(fetchedAt) FROM categoryFeedCache WHERE categoryId = :cat AND languageCode = :lang")
    suspend fun getLastFetchTime(cat: String, lang: String): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CachedCategoryFeedItem>)

    @Query("DELETE FROM categoryFeedCache WHERE fetchedAt < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long)
}
