package com.github.libretube.db.obj

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.github.libretube.api.obj.StreamItem
import com.github.libretube.api.obj.StreamItem.Companion.TYPE_STREAM

@Entity(tableName = "categoryFeedCache")
data class CachedCategoryFeedItem(
    @PrimaryKey
    val cacheKey: String,
    val categoryId: String,
    val languageCode: String,
    val videoId: String,
    val title: String?,
    val thumbnail: String?,
    val uploaderName: String?,
    val uploaderUrl: String?,
    val uploaderAvatar: String?,
    val duration: Long?,
    val views: Long?,
    val uploaded: Long,
    val uploaderVerified: Boolean,
    val shortDescription: String?,
    val isShort: Boolean,
    val score: Double,
    val fetchedAt: Long
) {
    fun toStreamItem() = StreamItem(
        url = videoId,
        type = TYPE_STREAM,
        title = title,
        thumbnail = thumbnail,
        uploaderName = uploaderName,
        uploaderUrl = uploaderUrl,
        uploaderAvatar = uploaderAvatar,
        duration = duration,
        views = views,
        uploaded = uploaded,
        uploadedDate = null,
        uploaderVerified = uploaderVerified,
        shortDescription = shortDescription,
        isShort = isShort
    )

    companion object {
        fun buildCacheKey(categoryId: String, lang: String, videoId: String): String {
            return "$categoryId|$lang|$videoId"
        }
    }
}
