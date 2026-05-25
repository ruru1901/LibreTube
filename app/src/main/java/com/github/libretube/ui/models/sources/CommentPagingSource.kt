package com.github.libretube.ui.models.sources

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.github.libretube.api.MediaServiceRepository
import com.github.libretube.api.obj.Comment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CommentPagingSource(
    private val videoId: String,
    private val onCommentCount: (Long) -> Unit
) : PagingSource<String, Comment>() {
    override fun getRefreshKey(state: PagingState<String, Comment>) = null

    override suspend fun load(params: LoadParams<String>): LoadResult<String, Comment> {
        return try {
            val allComments = mutableListOf<Comment>()
            var nextPage: String? = params.key
            var pagesFetched = 0
            val maxPages = if (params.key == null) 5 else 1

            while ((nextPage != null || params.key == null) && pagesFetched < maxPages) {
                val result = withContext(Dispatchers.IO) {
                    if (nextPage != null) {
                        MediaServiceRepository.instance.getCommentsNextPage(videoId, nextPage)
                    } else {
                        MediaServiceRepository.instance.getComments(videoId).also {
                            withContext(Dispatchers.Main) {
                                onCommentCount(maxOf(0, it.commentCount))
                            }
                        }
                    }
                }
                allComments.addAll(result.comments)
                nextPage = result.nextpage
                pagesFetched++
                if (result.nextpage == null || result.comments.isEmpty()) break
            }

            LoadResult.Page(allComments, null, nextPage)
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}
