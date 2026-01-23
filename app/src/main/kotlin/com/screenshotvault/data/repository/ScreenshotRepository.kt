package com.screenshotvault.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.screenshotvault.data.db.dao.EssenceDao
import com.screenshotvault.data.db.dao.ScreenshotItemDao
import com.screenshotvault.data.db.dao.SearchDao
import com.screenshotvault.ui.screens.feed.ProcessingFilter
import com.screenshotvault.data.db.entities.EssenceEntity
import com.screenshotvault.data.db.entities.ProcessingStatus
import com.screenshotvault.data.db.entities.ScreenshotItemEntity
import com.screenshotvault.data.model.toDomain
import com.screenshotvault.domain.model.Essence
import com.screenshotvault.domain.model.ScreenshotItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScreenshotRepository @Inject constructor(
    private val screenshotItemDao: ScreenshotItemDao,
    private val essenceDao: EssenceDao,
    private val searchDao: SearchDao,
) {
    fun getFeedPagingData(includeSolved: Boolean, includeNoContent: Boolean = false, processingFilter: ProcessingFilter = ProcessingFilter.ALL): Flow<PagingData<ScreenshotItem>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                prefetchDistance = 10,
                enablePlaceholders = false,
            ),
            pagingSourceFactory = { screenshotItemDao.pagingSource(includeSolved, includeNoContent, processingFilter.name) },
        ).flow.map { pagingData ->
            pagingData.map { entity -> entity.toDomain() }
        }
    }

    fun getNoContentPagingData(): Flow<PagingData<ScreenshotItem>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                prefetchDistance = 10,
                enablePlaceholders = false,
            ),
            pagingSourceFactory = { screenshotItemDao.noContentPagingSource() },
        ).flow.map { pagingData ->
            pagingData.map { entity -> entity.toDomain() }
        }
    }

    suspend fun insert(item: ScreenshotItemEntity): Long =
        screenshotItemDao.insert(item)

    suspend fun insertAll(items: List<ScreenshotItemEntity>): List<Long> =
        screenshotItemDao.insertAll(items)

    suspend fun getById(id: String): ScreenshotItem? {
        val entity = screenshotItemDao.getById(id) ?: return null
        val essence = essenceDao.getByScreenshotId(id)
        return entity.toDomain().copy(
            essence = essence?.toDomain(),
        )
    }

    fun observeById(id: String): Flow<ScreenshotItem?> {
        return combine(
            screenshotItemDao.observeById(id),
            essenceDao.observeByScreenshotId(id),
        ) { screenshot, essence ->
            screenshot?.toDomain()?.copy(essence = essence?.toDomain())
        }
    }

    suspend fun existsBySha256(sha256: String): Boolean =
        screenshotItemDao.existsBySha256(sha256)

    suspend fun getItemsForProcessing(limit: Int): List<ScreenshotItemEntity> =
        screenshotItemDao.getByStatus(ProcessingStatus.NEW, limit)

    suspend fun updateStatus(id: String, status: ProcessingStatus) =
        screenshotItemDao.updateStatus(id, status)

    suspend fun updateStatusWithError(id: String, status: ProcessingStatus, error: String?) =
        screenshotItemDao.updateStatusWithError(id, status, error)

    suspend fun updateProcessingResult(
        id: String,
        status: ProcessingStatus,
        domain: String?,
        type: String?,
        ocrText: String?,
    ) = screenshotItemDao.updateProcessingResult(id, status, domain, type, ocrText)

    suspend fun toggleSolved(id: String, solved: Boolean) =
        screenshotItemDao.updateSolved(id, solved)

    suspend fun updateNoContent(id: String, noContent: Boolean) =
        screenshotItemDao.updateNoContent(id, noContent)

    suspend fun saveEssence(essence: EssenceEntity) =
        essenceDao.insert(essence)

    fun observeTotalCount(): Flow<Int> = screenshotItemDao.observeTotalCount()

    fun observeUnsolvedCount(): Flow<Int> = screenshotItemDao.observeUnsolvedCount()

    fun observeNewCount(): Flow<Int> =
        screenshotItemDao.observeCountByStatus(ProcessingStatus.NEW)

    fun observeProcessingCount(): Flow<Int> =
        screenshotItemDao.observeCountByStatus(ProcessingStatus.PROCESSING)

    fun observeNoContentCount(): Flow<Int> =
        screenshotItemDao.observeNoContentCount()

    fun observeProcessedCount(): Flow<Int> =
        screenshotItemDao.observeCountByStatus(ProcessingStatus.DONE)

    fun observePendingCount(): Flow<Int> =
        screenshotItemDao.observePendingCount()

    suspend fun deleteAll() {
        essenceDao.deleteAll()
        screenshotItemDao.deleteAll()
    }

    suspend fun resetAllForReprocessing(): Int {
        // Delete all essences so they get regenerated
        essenceDao.deleteAll()
        // Clear search index
        searchDao.deleteAllSearchableContent()
        // Reset status to NEW so they get picked up for processing
        val count = screenshotItemDao.resetAllForReprocessing()
        // Also reset noContent flag since AI might categorize differently
        screenshotItemDao.resetNoContentFlag()
        return count
    }

    suspend fun reprocessItem(id: String) {
        // Delete essence for this item
        essenceDao.deleteByScreenshotId(id)
        // Delete from search index
        searchDao.deleteSearchableContent(id)
        // Reset status to NEW
        screenshotItemDao.resetForReprocessing(id)
    }

    suspend fun resetStuckProcessing(): Int {
        return screenshotItemDao.resetStuckProcessing()
    }

    suspend fun deleteItem(id: String) {
        // Delete from search index
        searchDao.deleteSearchableContent(id)
        // Delete essence
        essenceDao.deleteByScreenshotId(id)
        // Delete the screenshot item
        screenshotItemDao.deleteById(id)
    }
}
