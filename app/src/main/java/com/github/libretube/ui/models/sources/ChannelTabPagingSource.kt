package com.github.libretube.ui.models.sources

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.github.libretube.api.MediaServiceRepository
import com.github.libretube.api.obj.ChannelTab
import com.github.libretube.api.obj.ContentItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ChannelTabPagingSource(
    private val tab: ChannelTab
): PagingSource<String, ContentItem>() {
    override fun getRefreshKey(state: PagingState<String, ContentItem>) = null

    override suspend fun load(params: LoadParams<String>): LoadResult<String, ContentItem> {
        return try {
            val allItems = mutableListOf<ContentItem>()
            var nextPage: String? = params.key
            var pagesFetched = 0
            val maxPages = if (params.key == null) 5 else 1

            while ((nextPage != null || params.key == null) && pagesFetched < maxPages) {
                val resp = withContext(Dispatchers.IO) {
                    MediaServiceRepository.instance.getChannelTab(tab.data, nextPage)
                }
                allItems.addAll(resp.content)
                nextPage = resp.nextpage
                pagesFetched++
                if (resp.nextpage == null || resp.content.isEmpty()) break
            }

            LoadResult.Page(allItems, null, nextPage)
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}
