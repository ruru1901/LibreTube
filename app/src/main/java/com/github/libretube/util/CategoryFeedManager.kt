package com.github.libretube.util

import android.content.Context
import com.github.libretube.R
import com.github.libretube.api.obj.StreamItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object CategoryFeedManager {

    data class CategoryDef(
        val labelRes: Int,
        val keywords: List<String>
    )

    val definitions: Map<String, CategoryDef> = mapOf(
        "music" to CategoryDef(R.string.cat_music, listOf("music", "songs")),
        "gaming" to CategoryDef(R.string.cat_gaming, listOf("gaming", "gameplay")),
        "news" to CategoryDef(R.string.cat_news, listOf("news", "current affairs")),
        "education" to CategoryDef(R.string.cat_education, listOf("education", "tutorial", "learning")),
        "comedy" to CategoryDef(R.string.cat_comedy, listOf("comedy", "funny")),
        "tech" to CategoryDef(R.string.cat_tech, listOf("technology", "tech", "gadgets")),
        "sports" to CategoryDef(R.string.cat_sports, listOf("sports", "highlights")),
        "vlogs" to CategoryDef(R.string.cat_vlogs, listOf("vlogs", "daily vlog")),
        "cooking" to CategoryDef(R.string.cat_cooking, listOf("cooking", "recipes", "food")),
        "finance" to CategoryDef(R.string.cat_finance, listOf("finance", "investing", "money")),
        "fitness" to CategoryDef(R.string.cat_fitness, listOf("fitness", "workout")),
        "movies" to CategoryDef(R.string.cat_movies, listOf("movies", "movie review", "film")),
        "anime" to CategoryDef(R.string.cat_anime, listOf("anime", "anime review")),
        "podcasts" to CategoryDef(R.string.cat_podcasts, listOf("podcasts", "podcast episodes")),
    )

    val languageSearchTerms: Map<String, String> = mapOf(
        "ta" to "tamil", "hi" to "hindi", "te" to "telugu",
        "ml" to "malayalam", "bn" to "bengali", "mr" to "marathi",
        "pa" to "punjabi", "kn" to "kannada",
        "en" to "", "other" to ""
    )

    fun resolveCategoryIds(context: Context, raw: List<String>): List<String> {
        val labelToId = mapOf(
            context.getString(R.string.cat_music) to "music",
            context.getString(R.string.cat_gaming) to "gaming",
            context.getString(R.string.cat_news) to "news",
            context.getString(R.string.cat_education) to "education",
            context.getString(R.string.cat_comedy) to "comedy",
            context.getString(R.string.cat_tech) to "tech",
            context.getString(R.string.cat_sports) to "sports",
            context.getString(R.string.cat_vlogs) to "vlogs",
            context.getString(R.string.cat_cooking) to "cooking",
            context.getString(R.string.cat_finance) to "finance",
            context.getString(R.string.cat_fitness) to "fitness",
            context.getString(R.string.cat_movies) to "movies",
            context.getString(R.string.cat_anime) to "anime",
            context.getString(R.string.cat_podcasts) to "podcasts",
        )
        return raw.map { labelToId[it] ?: it }
    }

    fun resolveLanguageCodes(context: Context, raw: List<String>): List<String> {
        val labelToCode = mapOf(
            context.getString(R.string.lang_english) to "en",
            context.getString(R.string.lang_hindi) to "hi",
            context.getString(R.string.lang_tamil) to "ta",
            context.getString(R.string.lang_telugu) to "te",
            context.getString(R.string.lang_malayalam) to "ml",
            context.getString(R.string.lang_kannada) to "kn",
            context.getString(R.string.lang_bengali) to "bn",
            context.getString(R.string.lang_marathi) to "mr",
            context.getString(R.string.lang_punjabi) to "pa",
            context.getString(R.string.lang_other) to "other",
        )
        return raw.map { labelToCode[it] ?: it }
    }

    data class QueryDef(
        val categoryId: String,
        val languageCode: String,
        val query: String
    )

    fun buildQueries(
        categoryIds: List<String>,
        languageCodes: List<String>,
        maxCategories: Int = 5,
        maxLanguages: Int = 2
    ): List<QueryDef> {
        val cats = categoryIds.take(maxCategories)
        val langs = languageCodes.take(maxLanguages)
        return cats.flatMap { cat ->
            val def = definitions[cat] ?: return@flatMap emptyList()
            langs.mapNotNull { lang ->
                val suffix = languageSearchTerms[lang] ?: return@mapNotNull null
                val keyword = def.keywords.firstOrNull() ?: return@mapNotNull null
                val query = if (suffix.isNotEmpty()) "$keyword $suffix" else keyword
                QueryDef(cat, lang, query)
            }
        }
    }

    fun getLabel(context: Context, categoryId: String): String {
        val def = definitions[categoryId] ?: return categoryId
        return context.getString(def.labelRes)
    }

    suspend fun scoreAndFilter(
        items: List<StreamItem>,
        watchedIds: Set<String>,
        context: Context
    ): List<StreamItem> = withContext(Dispatchers.Default) {
        val seenIds = mutableSetOf<String>()
        items
            .filter { !it.isShort }
            .filter { it.duration == null || it.duration > 60L }
            .filter { it.uploaded > 0L }
            .filter { it.url != null && seenIds.add(it.url) }
            .filter { it.url !in watchedIds }
            .let { FeedScorer.sortByRelevance(it, context) }
    }
}
