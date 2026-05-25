package com.github.libretube.ui.models.sources

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.github.libretube.api.MediaServiceRepository
import com.github.libretube.api.obj.Comment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CommentRepliesPagingSource(
    private val videoId: String,
    private val originalComment: Comment
) : PagingSource<String, Comment>() {
    override fun getRefreshKey(state: PagingState<String, Comment>) = null

    override suspend fun load(params: LoadParams<String>): LoadResult<String, Comment> {
        return try {
            val allReplies = mutableListOf<Comment>()
            val startKey = params.key.orEmpty().ifEmpty { originalComment.repliesPage.orEmpty() }
            var nextPage: String? = startKey
            var pagesFetched = 0
            val maxPages = if (params.key.isNullOrEmpty()) 3 else 1

            while (nextPage != null && pagesFetched < maxPages) {
                val result = withContext(Dispatchers.IO) {
                    MediaServiceRepository.instance.getCommentsNextPage(videoId, nextPage)
                }
                val replies = result.comments.toMutableList()
                if (params.key.isNullOrEmpty() && pagesFetched == 0 && allReplies.isEmpty()) {
                    allReplies.add(originalComment)
                }
                allReplies.addAll(replies)
                nextPage = result.nextpage
                pagesFetched++
                if (result.nextpage == null || result.comments.isEmpty()) break
            }

            LoadResult.Page(allReplies, null, nextPage)
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}
