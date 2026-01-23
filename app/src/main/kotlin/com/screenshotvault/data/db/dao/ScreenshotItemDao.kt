package com.screenshotvault.data.db.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.screenshotvault.data.db.entities.ProcessingStatus
import com.screenshotvault.data.db.entities.ScreenshotItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScreenshotItemDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(item: ScreenshotItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(items: List<ScreenshotItemEntity>): List<Long>

    @Update
    suspend fun update(item: ScreenshotItemEntity)

    @Query("SELECT * FROM screenshot_items WHERE id = :id")
    suspend fun getById(id: String): ScreenshotItemEntity?

    @Query("SELECT * FROM screenshot_items WHERE id = :id")
    fun observeById(id: String): Flow<ScreenshotItemEntity?>

    @Query("SELECT * FROM screenshot_items WHERE sha256 = :sha256")
    suspend fun getBySha256(sha256: String): ScreenshotItemEntity?

    @Query("SELECT * FROM screenshot_items WHERE content_uri = :uri")
    suspend fun getByContentUri(uri: String): ScreenshotItemEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM screenshot_items WHERE sha256 = :sha256)")
    suspend fun existsBySha256(sha256: String): Boolean

    @Query(
        """
        SELECT * FROM screenshot_items
        WHERE (:includeSolved = 1 OR solved = 0)
        AND (:includeNoContent = 1 OR no_content = 0)
        AND (
            :processingFilter = 'ALL'
            OR (:processingFilter = 'PROCESSED' AND status = 'DONE')
            OR (:processingFilter = 'PENDING' AND status != 'DONE')
        )
        ORDER BY captured_at DESC
        """,
    )
    fun pagingSource(includeSolved: Boolean, includeNoContent: Boolean = false, processingFilter: String = "ALL"): PagingSource<Int, ScreenshotItemEntity>

    @Query(
        """
        SELECT * FROM screenshot_items
        WHERE no_content = 1
        ORDER BY captured_at DESC
        """,
    )
    fun noContentPagingSource(): PagingSource<Int, ScreenshotItemEntity>

    @Query(
        """
        SELECT * FROM screenshot_items
        WHERE status = :status
        ORDER BY captured_at DESC
        LIMIT :limit
        """,
    )
    suspend fun getByStatus(status: ProcessingStatus, limit: Int): List<ScreenshotItemEntity>

    @Query("UPDATE screenshot_items SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: ProcessingStatus)

    @Query("UPDATE screenshot_items SET status = :status, error_message = :errorMessage WHERE id = :id")
    suspend fun updateStatusWithError(id: String, status: ProcessingStatus, errorMessage: String?)

    @Query("UPDATE screenshot_items SET solved = :solved WHERE id = :id")
    suspend fun updateSolved(id: String, solved: Boolean)

    @Query("UPDATE screenshot_items SET no_content = :noContent WHERE id = :id")
    suspend fun updateNoContent(id: String, noContent: Boolean)

    @Query(
        """
        UPDATE screenshot_items SET
            status = :status,
            domain = :domain,
            type = :type,
            ocr_text = :ocrText
        WHERE id = :id
        """,
    )
    suspend fun updateProcessingResult(
        id: String,
        status: ProcessingStatus,
        domain: String?,
        type: String?,
        ocrText: String?,
    )

    @Query("SELECT COUNT(*) FROM screenshot_items")
    fun observeTotalCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM screenshot_items WHERE status = :status")
    fun observeCountByStatus(status: ProcessingStatus): Flow<Int>

    @Query("SELECT COUNT(*) FROM screenshot_items WHERE status != 'DONE'")
    fun observePendingCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM screenshot_items WHERE solved = 0")
    fun observeUnsolvedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM screenshot_items WHERE no_content = 1")
    fun observeNoContentCount(): Flow<Int>

    @Query("DELETE FROM screenshot_items WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM screenshot_items")
    suspend fun deleteAll()

    @Query("UPDATE screenshot_items SET status = 'NEW' WHERE status IN ('DONE', 'FAILED', 'PROCESSING')")
    suspend fun resetAllForReprocessing(): Int

    @Query("UPDATE screenshot_items SET status = 'NEW', no_content = 0 WHERE id = :id")
    suspend fun resetForReprocessing(id: String)

    @Query("UPDATE screenshot_items SET status = 'NEW' WHERE status = 'PROCESSING'")
    suspend fun resetStuckProcessing(): Int

    @Query("UPDATE screenshot_items SET no_content = 0")
    suspend fun resetNoContentFlag()

    @Query("SELECT * FROM screenshot_items ORDER BY captured_at DESC")
    suspend fun getAll(): List<ScreenshotItemEntity>

    @Query(
        """
        SELECT domain, COUNT(*) as count
        FROM screenshot_items
        WHERE domain IS NOT NULL AND domain != '' AND status = 'DONE'
        GROUP BY domain
        ORDER BY count DESC
        """,
    )
    fun observeDomainsWithCount(): Flow<List<DomainWithCount>>

    @Query(
        """
        SELECT * FROM screenshot_items
        WHERE domain = :domain AND status = 'DONE'
        ORDER BY captured_at DESC
        """,
    )
    suspend fun getByDomain(domain: String): List<ScreenshotItemEntity>

    // Insights queries

    @Query(
        """
        SELECT e.suggested_action, COUNT(*) as count
        FROM screenshot_items s
        INNER JOIN essences e ON s.id = e.screenshot_id
        WHERE s.status = 'DONE' AND (:includeSolved = 1 OR s.solved = 0)
        AND e.suggested_action IS NOT NULL AND e.suggested_action != 'unknown'
        GROUP BY e.suggested_action ORDER BY count DESC
        """,
    )
    fun observeActionCounts(includeSolved: Boolean): Flow<List<ActionCount>>

    @Query(
        """
        SELECT type, COUNT(*) as count
        FROM screenshot_items WHERE status = 'DONE' AND (:includeSolved = 1 OR solved = 0)
        AND type IS NOT NULL AND type != 'unknown'
        GROUP BY type ORDER BY count DESC
        """,
    )
    fun observeContentTypeCounts(includeSolved: Boolean): Flow<List<ContentTypeCount>>

    @Query(
        """
        SELECT CASE
            WHEN captured_at >= :todayStart THEN 'TODAY'
            WHEN captured_at >= :weekStart THEN 'THIS_WEEK'
            WHEN captured_at >= :monthStart THEN 'THIS_MONTH'
            ELSE 'OLDER'
        END as time_period, COUNT(*) as count
        FROM screenshot_items WHERE status = 'DONE' AND (:includeSolved = 1 OR solved = 0)
        GROUP BY time_period
        """,
    )
    fun observeTimePeriodCounts(
        todayStart: Long,
        weekStart: Long,
        monthStart: Long,
        includeSolved: Boolean,
    ): Flow<List<TimePeriodCount>>

    @Query(
        """
        SELECT domain, COUNT(*) as count
        FROM screenshot_items
        WHERE domain IS NOT NULL AND domain != '' AND status = 'DONE'
        AND (:includeSolved = 1 OR solved = 0)
        GROUP BY domain
        ORDER BY count DESC
        """,
    )
    fun observeDomainsWithCountFiltered(includeSolved: Boolean): Flow<List<DomainWithCount>>

    @Query(
        """
        SELECT s.* FROM screenshot_items s
        INNER JOIN essences e ON s.id = e.screenshot_id
        WHERE e.suggested_action = :action AND s.status = 'DONE'
        AND (:includeSolved = 1 OR s.solved = 0)
        ORDER BY s.captured_at DESC
        """,
    )
    suspend fun getByAction(action: String, includeSolved: Boolean): List<ScreenshotItemEntity>

    @Query(
        """
        SELECT * FROM screenshot_items
        WHERE type = :type AND status = 'DONE' AND (:includeSolved = 1 OR solved = 0)
        ORDER BY captured_at DESC
        """,
    )
    suspend fun getByContentType(type: String, includeSolved: Boolean): List<ScreenshotItemEntity>

    @Query(
        """
        SELECT * FROM screenshot_items
        WHERE captured_at >= :start AND captured_at < :end AND status = 'DONE'
        AND (:includeSolved = 1 OR solved = 0)
        ORDER BY captured_at DESC
        """,
    )
    suspend fun getByTimeRange(start: Long, end: Long, includeSolved: Boolean): List<ScreenshotItemEntity>

    @Query(
        """
        SELECT * FROM screenshot_items
        WHERE domain = :domain AND status = 'DONE' AND (:includeSolved = 1 OR solved = 0)
        ORDER BY captured_at DESC
        """,
    )
    suspend fun getByDomainFiltered(domain: String, includeSolved: Boolean): List<ScreenshotItemEntity>

    @Query("SELECT COUNT(*) FROM screenshot_items WHERE status = 'DONE' AND (:includeSolved = 1 OR solved = 0)")
    fun observeTotalCountFiltered(includeSolved: Boolean): Flow<Int>

    @Query(
        """
        SELECT COUNT(*) FROM screenshot_items
        WHERE status = 'DONE' AND (:includeSolved = 1 OR solved = 0)
        AND captured_at >= :weekStart
        """,
    )
    fun observeThisWeekCount(weekStart: Long, includeSolved: Boolean): Flow<Int>
}

data class DomainWithCount(
    val domain: String,
    val count: Int,
)

data class ActionCount(
    @androidx.room.ColumnInfo(name = "suggested_action")
    val action: String,
    val count: Int,
)

data class ContentTypeCount(
    @androidx.room.ColumnInfo(name = "type")
    val contentType: String,
    val count: Int,
)

data class TimePeriodCount(
    @androidx.room.ColumnInfo(name = "time_period")
    val period: String,
    val count: Int,
)
