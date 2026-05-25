package com.github.libretube.util

import android.content.Context
import com.github.libretube.api.obj.StreamItem
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.db.DatabaseHolder
import com.github.libretube.extensions.toID
import com.github.libretube.helpers.PreferenceHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object FeedScorer {

    suspend fun sortByRelevance(
        feed: List<StreamItem>,
        context: Context
    ): List<StreamItem> = withContext(Dispatchers.Default) {
        val history = DatabaseHolder.Database.watchHistoryDao().getAll()

        if (history.isEmpty()) {
            return@withContext scoreByPreferences(feed)
        }

        val channelHistory = history.groupBy { it.channelId.orEmpty() }
        val videoIdsWatched = history.map { it.videoId }.toSet()
        val uniqueTagsCache = mutableMapOf<String, Int>()
        val languageCache = mutableMapOf<String, String?>()
        val lowWatchCountCache = mutableMapOf<String, Int>()
        val topCategory = history.groupingBy { it.categoryId }.eachCount()
            .maxByOrNull { it.value }?.key

        feed.map { item ->
            val channelId = item.uploaderUrl?.toID().orEmpty()
            var score = 0f

            val channelEntries = channelHistory[channelId].orEmpty()

            if (channelEntries.size >= 3) score += 3f

            if (topCategory != null) {
                val channelTopCat = channelEntries.groupingBy { it.categoryId }.eachCount()
                    .maxByOrNull { it.value }?.key
                if (channelTopCat == topCategory) score += 2f
            }

            val uniqueTags = uniqueTagsCache.getOrPut(channelId) {
                channelEntries
                    .flatMap { it.tags?.split(",").orEmpty() }
                    .map { it.trim() }.filter { it.isNotBlank() }.distinct().size
            }
            score += uniqueTags

            val lang = languageCache.getOrPut(channelId) {
                channelEntries.groupingBy { it.languageCode }.eachCount()
                    .maxByOrNull { it.value }?.key
            }
            if (lang != null) {
                val preferred = PreferenceHelper.getString(PreferenceKeys.PREFERRED_LANGUAGES, "")
                    .split(",").map { it.trim() }.filter { it.isNotBlank() }
                if (lang in preferred) score += 3f
            }

            val videoId = item.url?.toID().orEmpty()
            if (videoId in videoIdsWatched) score -= 5f

            val lowCount = lowWatchCountCache.getOrPut(channelId) {
                channelEntries.count { it.watchPercent != null && it.watchPercent < 10f }
            }
            if (lowCount >= 2) score -= 3f

            item to score
        }.sortedByDescending { it.second }.map { it.first }
    }

    private fun scoreByPreferences(feed: List<StreamItem>): List<StreamItem> {
        return feed
    }
}
