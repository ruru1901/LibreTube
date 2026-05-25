package com.github.libretube.repo

import com.github.libretube.api.obj.StreamItem
import com.github.libretube.api.toStreamItem
import com.github.libretube.helpers.NewPipeExtractorInstance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.Page
import org.schabi.newpipe.extractor.search.SearchExtractor
import org.schabi.newpipe.extractor.stream.StreamInfoItem

class CategoryFeedRepository {
    private val extractorCache = mutableMapOf<String, SearchExtractor>()
    private val pageCursors = mutableMapOf<String, Page?>()

    suspend fun getSearchPage(query: String): List<StreamItem> = withContext(Dispatchers.IO) {
        val extractor = try {
            extractorCache.getOrPut(query) {
                NewPipeExtractorInstance.extractor
                    .getSearchExtractor(query, listOf("videos"), "")
                    .also { it.fetchPage() }
            }
        } catch (e: Exception) {
            extractorCache.remove(query)
            throw e
        }

        val currentCursor = pageCursors[query]
        val resultPage = if (currentCursor == null) {
            extractor.initialPage
        } else {
            extractor.getPage(currentCursor)
        }

        pageCursors[query] = resultPage.nextPage

        resultPage.items
            .filterIsInstance<StreamInfoItem>()
            .map { it.toStreamItem() }
    }

    fun getNextPageCursor(query: String): Page? = pageCursors[query]

    fun setNextPageCursor(query: String, page: Page?) {
        pageCursors[query] = page
    }

    fun reset() {
        extractorCache.clear()
        pageCursors.clear()
    }
}
