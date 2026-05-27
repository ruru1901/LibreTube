package com.github.libretube.ui.models

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.libretube.LibreTubeApp
import com.github.libretube.api.MediaServiceRepository
import com.github.libretube.api.PlaylistsHelper
import com.github.libretube.api.SubscriptionHelper
import com.github.libretube.api.TrendingCategory
import com.github.libretube.api.obj.Playlists
import com.github.libretube.api.obj.StreamItem
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.db.DatabaseHelper
import com.github.libretube.db.DatabaseHolder
import com.github.libretube.db.obj.CachedCategoryFeedItem
import com.github.libretube.db.obj.PlaylistBookmark
import com.github.libretube.db.obj.SeenVideo
import com.github.libretube.extensions.runSafely
import com.github.libretube.extensions.toID
import com.github.libretube.extensions.updateIfChanged
import com.github.libretube.helpers.PlayerHelper
import com.github.libretube.helpers.PreferenceHelper
import com.github.libretube.repo.CategoryFeedRepository
import com.github.libretube.util.CategoryFeedManager
import com.github.libretube.util.FeedScorer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.coroutineScope

class HomeViewModel : ViewModel() {
    private val hideWatched
        get() = PreferenceHelper.getBoolean(
            PreferenceKeys.HIDE_WATCHED_FROM_FEED,
            false
        )
    private val showUpcoming
        get() = PreferenceHelper.getBoolean(
            PreferenceKeys.SHOW_UPCOMING_IN_FEED,
            true
        )

    val trending: MutableLiveData<Pair<TrendingCategory, TrendsViewModel.TrendingStreams>> =
        MutableLiveData(null)
    val feed: MutableLiveData<List<StreamItem>> = MutableLiveData(null)
    val bookmarks: MutableLiveData<List<PlaylistBookmark>> = MutableLiveData(null)
    val playlists: MutableLiveData<List<Playlists>> = MutableLiveData(null)
    val continueWatching: MutableLiveData<List<StreamItem>> = MutableLiveData(null)
    private var continueWatchingLoaded = false

    data class CategoryFeedData(
        val categoryIds: List<String>,
        val labels: List<String>,
        val queries: List<String>,
        val videos: List<List<StreamItem>>
    )
    val categoryFeeds: MutableLiveData<CategoryFeedData?> = MutableLiveData(null)

    val isLoading: MutableLiveData<Boolean> = MutableLiveData(true)
    val loadedSuccessfully: MutableLiveData<Boolean> = MutableLiveData(false)

    private val sections get() = listOf(trending, feed, bookmarks, playlists, continueWatching)

    private var loadHomeJob: Job? = null

    private val categoryFeedRepository = CategoryFeedRepository()
    private var lastPrefs: Pair<List<String>, List<String>>? = null

    fun loadHomeFeed(
        context: Context,
        subscriptionsViewModel: SubscriptionsViewModel,
        visibleItems: Set<String>,
        forceRefresh: Boolean = false
    ) {
        isLoading.value = true
        if (forceRefresh) continueWatchingLoaded = false

        loadHomeJob?.cancel()
        loadHomeJob = viewModelScope.launch {
            awaitAll(
                async { if (visibleItems.contains(TRENDING)) loadTrending(context) },
                async { if (visibleItems.contains(FEATURED)) loadFeed(subscriptionsViewModel) },
                async { if (visibleItems.contains(BOOKMARKS)) loadBookmarks() },
                async { if (visibleItems.contains(PLAYLISTS)) loadPlaylists() },
                async { if (visibleItems.contains(WATCHING) && !continueWatchingLoaded) loadVideosToContinueWatching() },
                async { if (visibleItems.contains(PERSONALIZED)) loadPersonalizedCategories(context) }
            )
            loadedSuccessfully.value = sections.any { it.value != null } || categoryFeeds.value != null
            isLoading.value = false
        }
    }

    private suspend fun loadTrending(context: Context) {
        val region = PreferenceHelper.getTrendingRegion(context)
        val category = PreferenceHelper.getString(
            PreferenceKeys.TRENDING_CATEGORY,
            TrendingCategory.LIVE.name
        ).let { TrendingCategory.valueOf(it) }

        runSafely(
            onSuccess = { videos ->
                trending.updateIfChanged(
                    Pair(
                        category,
                        TrendsViewModel.TrendingStreams(region, videos)
                    )
                )
            },
            ioBlock = {
                MediaServiceRepository.instance.getTrending(region, category)
            }
        )
    }

    private suspend fun loadFeed(subscriptionsViewModel: SubscriptionsViewModel) {
        runSafely(
            onSuccess = { videos -> feed.updateIfChanged(videos) },
            ioBlock = { tryLoadFeed(subscriptionsViewModel) }
        )
    }

    private suspend fun loadBookmarks() {
        runSafely(
            onSuccess = { newBookmarks -> bookmarks.updateIfChanged(newBookmarks) },
            ioBlock = { DatabaseHolder.Database.playlistBookmarkDao().getAll() }
        )
    }

    private suspend fun loadPlaylists() {
        runSafely(
            onSuccess = { newPlaylists -> playlists.updateIfChanged(newPlaylists) },
            ioBlock = { PlaylistsHelper.getPlaylists() }
        )
    }

    private suspend fun loadVideosToContinueWatching() {
        if (!PlayerHelper.watchHistoryEnabled) return
        runSafely(
            onSuccess = { videos ->
                continueWatching.updateIfChanged(videos)
                continueWatchingLoaded = true
            },
            ioBlock = ::loadWatchingFromDB
        )
    }

    private suspend fun loadWatchingFromDB(): List<StreamItem> {
        val videos = DatabaseHelper.getWatchHistoryPage(1, 50)
        val streamItems = videos.map { it.toStreamItem() }

        // only include items that have a saved watch position
        return streamItems.filter { item ->
            val videoId = item.url?.toID() ?: return@filter false
            val position = DatabaseHelper.getWatchPosition(videoId)
            position != null && position > 0
        }.let { filtered ->
            DatabaseHelper.filterUnwatched(filtered)
        }
    }

    private suspend fun tryLoadFeed(subscriptionsViewModel: SubscriptionsViewModel): List<StreamItem> {
        val feed = subscriptionsViewModel.videoFeed.value ?: run {
            SubscriptionHelper.getFeed(forceRefresh = false).also {
                subscriptionsViewModel.videoFeed.postValue(it)
            }
        }

        return FeedScorer.sortByRelevance(
            DatabaseHelper.filterByStreamTypeAndWatchPosition(feed, hideWatched, showUpcoming),
            LibreTubeApp.instance
        )
    }

    private suspend fun fetchFilteredPage(
        queryDef: CategoryFeedManager.QueryDef,
        context: Context,
        watchedIds: Set<String>,
        seenIds: Set<String>
    ): List<StreamItem> {
        val query = queryDef.query
        repeat(MAX_RETRIES) {
            val items = runCatching {
                categoryFeedRepository.getSearchPage(query)
            }.onFailure {
                return@repeat
            }.getOrThrow()

            if (items.isEmpty()) return emptyList()

            val filtered = CategoryFeedManager.scoreAndFilter(items, watchedIds, context)
                .filter { it.url !in seenIds }
            if (filtered.isNotEmpty()) {
                runCatching { cacheResults(queryDef, items) }
                if (filtered.size >= MIN_CATEGORY_ITEMS) {
                    return filtered.take(20)
                }
            }
        }
        return loadCachedResults(queryDef, watchedIds, seenIds, context)
    }

    private suspend fun cacheResults(
        queryDef: CategoryFeedManager.QueryDef,
        items: List<StreamItem>
    ) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val cachedItems = items.mapNotNull { item ->
            val url = item.url ?: return@mapNotNull null
            CachedCategoryFeedItem(
                cacheKey = CachedCategoryFeedItem.buildCacheKey(
                    queryDef.categoryId, queryDef.languageCode, url
                ),
                categoryId = queryDef.categoryId,
                languageCode = queryDef.languageCode,
                videoId = url,
                title = item.title,
                thumbnail = item.thumbnail,
                uploaderName = item.uploaderName,
                uploaderUrl = item.uploaderUrl,
                uploaderAvatar = item.uploaderAvatar,
                duration = item.duration,
                views = item.views,
                uploaded = item.uploaded,
                uploaderVerified = item.uploaderVerified ?: false,
                shortDescription = item.shortDescription,
                isShort = item.isShort,
                score = 0.0,
                fetchedAt = now
            )
        }
        DatabaseHolder.Database.categoryFeedDao().insertAll(cachedItems)
        DatabaseHolder.Database.categoryFeedDao().deleteOlderThan(
            System.currentTimeMillis() - CACHE_TTL_DAYS * 24 * 60 * 60 * 1000L
        )
    }

    private suspend fun loadCachedResults(
        queryDef: CategoryFeedManager.QueryDef,
        watchedIds: Set<String>,
        seenIds: Set<String>,
        context: Context
    ): List<StreamItem> {
        val cached = runCatching {
            withContext(Dispatchers.IO) {
                DatabaseHolder.Database.categoryFeedDao()
                    .getForQuery(queryDef.categoryId, queryDef.languageCode, 20)
            }
        }.getOrDefault(emptyList())
        if (cached.isEmpty()) return emptyList()
        val streamItems = cached.map { it.toStreamItem() }
        return runCatching {
            CategoryFeedManager.scoreAndFilter(streamItems, watchedIds, context)
                .filter { it.url !in seenIds }
                .take(20)
        }.getOrDefault(emptyList())
    }

    private suspend fun loadPersonalizedCategories(context: Context) {
        val rawCategories = PreferenceHelper.getString(PreferenceKeys.PREFERRED_CATEGORIES, "")
            .split(",").map { it.trim() }.filter { it.isNotBlank() }
        val rawLanguages = PreferenceHelper.getString(PreferenceKeys.PREFERRED_LANGUAGES, "")
            .split(",").map { it.trim() }.filter { it.isNotBlank() }

        val resolvedCategories = CategoryFeedManager.resolveCategoryIds(context, rawCategories)
        val resolvedLanguages = CategoryFeedManager.resolveLanguageCodes(context, rawLanguages)

        val categories = resolvedCategories.ifEmpty { listOf("gaming", "music", "education", "tech", "movies") }
        val languages = resolvedLanguages.ifEmpty { listOf("en") }

        val currentPrefs = Pair(categories, languages)
        val prefsChanged = lastPrefs != null && lastPrefs != currentPrefs
        if (prefsChanged) {
            categoryFeedRepository.reset()
        }
        lastPrefs = currentPrefs

        val watchedIds = if (PlayerHelper.watchHistoryEnabled) {
            withContext(Dispatchers.IO) {
                runCatching {
                    DatabaseHolder.Database.watchHistoryDao().getAll().map { it.videoId }.toSet()
                }.getOrDefault(emptySet())
            }
        } else {
            emptySet()
        }

        val seenIds = withContext(Dispatchers.IO) {
            runCatching {
                DatabaseHolder.Database.seenVideoDao()
                    .getSeenSince(System.currentTimeMillis() - SEEN_TTL_DAYS * 24 * 60 * 60 * 1000L)
                    .toMutableSet()
            }.getOrDefault(mutableSetOf())
        }

        val queries = CategoryFeedManager.buildQueries(categories, languages)

        val results = withContext(Dispatchers.Default) {
            coroutineScope {
                queries.map { queryDef ->
                    async {
                        val videos = fetchFilteredPage(queryDef, context, watchedIds, seenIds.toSet())
                        // add fetched URLs to shared seenIds for intra-batch dedup
                        val urls = videos.mapNotNull { it.url }
                        synchronized(seenIds) { seenIds.addAll(urls) }
                        queryDef to videos
                    }
                }.awaitAll()
            }
        }

        val allSeenUrls = results.flatMap { it.second }.mapNotNull { it.url }
        if (allSeenUrls.isNotEmpty()) {
            withContext(Dispatchers.IO) {
                val now = System.currentTimeMillis()
                val seenVideos = allSeenUrls.map { SeenVideo(it, now) }
                DatabaseHolder.Database.seenVideoDao().insertAll(seenVideos)
                DatabaseHolder.Database.seenVideoDao().deleteOlderThan(
                    now - SEEN_TTL_DAYS * 24 * 60 * 60 * 1000L
                )
            }
        }

        val nonEmpty = results.filter { it.second.isNotEmpty() }

        if (nonEmpty.isNotEmpty()) {
            val dedupedResults = nonEmpty.mapNotNull { (queryDef, videos) ->
                if (videos.isEmpty()) null else queryDef to videos
            }

            if (dedupedResults.isNotEmpty()) {
                categoryFeeds.postValue(
                    CategoryFeedData(
                        categoryIds = dedupedResults.map { it.first.categoryId },
                        labels = dedupedResults.map { CategoryFeedManager.getLabel(context, it.first.categoryId) },
                        queries = dedupedResults.map { it.first.query },
                        videos = dedupedResults.map { it.second }
                    )
                )
                return
            }
        }
        if (prefsChanged) {
            categoryFeeds.postValue(null)
        }
    }

    companion object {
        private const val UNUSUAL_LOAD_TIME_MS = 10000L
        private const val FEATURED = "featured"
        private const val WATCHING = "watching"
        private const val TRENDING = "trending"
        private const val BOOKMARKS = "bookmarks"
        private const val PLAYLISTS = "playlists"
        private const val MAX_RETRIES = 3
        private const val MIN_CATEGORY_ITEMS = 5
        private const val CACHE_TTL_DAYS = 7L
        private const val SEEN_TTL_DAYS = 90L
        private const val PERSONALIZED = "personalized_categories"
    }
}
