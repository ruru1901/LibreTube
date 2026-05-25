package com.github.libretube.ui.models.sources

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.github.libretube.api.MediaServiceRepository
import com.github.libretube.api.obj.ContentItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SearchPagingSource(
    private val searchQuery: String,
    private val searchFilter: String,
    private val onSearchSuggestion: (Pair<String, Boolean>?) -> Unit
) : PagingSource<String, ContentItem>() {
    override fun getRefreshKey(state: PagingState<String, ContentItem>) = null

    override suspend fun load(params: LoadParams<String>): LoadResult<String, ContentItem> {
        return try {
            val allItems = mutableListOf<ContentItem>()
            var nextPage: String? = params.key
            var pagesFetched = 0
            val maxPages = if (params.key == null) 5 else 1

            while ((nextPage != null || params.key == null) && pagesFetched < maxPages) {
                val result = withContext(Dispatchers.IO) {
                    if (nextPage != null) {
                        MediaServiceRepository.instance.getSearchResultsNextPage(
                            searchQuery, searchFilter, nextPage
                        )
                    } else {
                        MediaServiceRepository.instance.getSearchResults(searchQuery, searchFilter)
                            .also {
                                if (it.suggestion.isNullOrEmpty()) onSearchSuggestion(null)
                                else onSearchSuggestion(it.suggestion to it.corrected)
                            }
                    }
                }
                allItems.addAll(result.items)
                nextPage = result.nextpage
                pagesFetched++
                if (result.nextpage == null || result.items.isEmpty()) break
            }

            LoadResult.Page(allItems, null, nextPage)
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}
