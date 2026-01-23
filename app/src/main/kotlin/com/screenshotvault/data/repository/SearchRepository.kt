package com.screenshotvault.data.repository

import com.screenshotvault.data.db.dao.EssenceDao
import com.screenshotvault.data.db.dao.SearchDao
import com.screenshotvault.data.db.fts.SearchableContent
import com.screenshotvault.data.model.toDomain
import com.screenshotvault.domain.model.Essence
import com.screenshotvault.domain.model.ScreenshotItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchRepository @Inject constructor(
    private val searchDao: SearchDao,
    private val essenceDao: EssenceDao,
) {
    suspend fun indexScreenshot(
        screenshotId: String,
        title: String,
        summaryBullets: List<String>,
        topics: List<String>,
        entities: List<String>,
        ocrText: String?,
        domain: String?,
        type: String?,
    ) {
        val content = SearchableContent(
            screenshotId = screenshotId,
            title = title,
            summary = summaryBullets.joinToString(" "),
            topics = topics.joinToString(" "),
            entities = entities.joinToString(" "),
            ocrText = ocrText ?: "",
            domain = domain ?: "",
            type = type ?: "",
        )
        searchDao.insertSearchableContent(content)
    }

    suspend fun removeFromIndex(screenshotId: String) {
        searchDao.deleteSearchableContent(screenshotId)
    }

    suspend fun clearIndex() {
        searchDao.deleteAllSearchableContent()
    }

    suspend fun search(
        query: String,
        includeSolved: Boolean = false,
        limit: Int = 50,
    ): List<ScreenshotItem> {
        if (query.isBlank()) return emptyList()

        // Format query for FTS - add wildcards for partial matching
        val ftsQuery = query.trim()
            .split(Regex("\\s+"))
            .joinToString(" ") { "$it*" }

        return try {
            val entities = searchDao.search(ftsQuery, includeSolved, limit)
            entities.map { entity ->
                val essence = essenceDao.getByScreenshotId(entity.id)
                entity.toDomain(essence)
            }
        } catch (e: Exception) {
            // FTS query might fail with special characters
            emptyList()
        }
    }

    fun searchFlow(
        query: String,
        includeSolved: Boolean = false,
        limit: Int = 50,
    ): Flow<List<ScreenshotItem>> {
        if (query.isBlank()) {
            return kotlinx.coroutines.flow.flowOf(emptyList())
        }

        val ftsQuery = query.trim()
            .split(Regex("\\s+"))
            .joinToString(" ") { "$it*" }

        return searchDao.searchFlow(ftsQuery, includeSolved, limit)
            .map { entities ->
                entities.map { entity ->
                    val essence = essenceDao.getByScreenshotId(entity.id)
                    entity.toDomain(essence)
                }
            }
    }

    suspend fun getIndexedCount(): Int = searchDao.getIndexedCount()
}
