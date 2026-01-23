package com.screenshotvault.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.screenshotvault.data.db.entities.ScreenshotItemEntity
import com.screenshotvault.data.db.fts.SearchableContent
import kotlinx.coroutines.flow.Flow

data class SearchResult(
    val screenshotId: String,
    val title: String,
    val matchInfo: String,
)

@Dao
interface SearchDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearchableContent(content: SearchableContent)

    @Query("DELETE FROM searchable_content WHERE screenshot_id = :screenshotId")
    suspend fun deleteSearchableContent(screenshotId: String)

    @Query("DELETE FROM searchable_content")
    suspend fun deleteAllSearchableContent()

    @Query(
        """
        SELECT s.* FROM screenshot_items s
        INNER JOIN searchable_content sc ON s.id = sc.screenshot_id
        INNER JOIN search_index si ON sc.screenshot_id = si.screenshot_id
        WHERE search_index MATCH :query
        AND (:includeSolved = 1 OR s.solved = 0)
        ORDER BY s.captured_at DESC
        LIMIT :limit
        """,
    )
    suspend fun search(
        query: String,
        includeSolved: Boolean = false,
        limit: Int = 50,
    ): List<ScreenshotItemEntity>

    @Query(
        """
        SELECT s.* FROM screenshot_items s
        INNER JOIN searchable_content sc ON s.id = sc.screenshot_id
        INNER JOIN search_index si ON sc.screenshot_id = si.screenshot_id
        WHERE search_index MATCH :query
        AND (:includeSolved = 1 OR s.solved = 0)
        ORDER BY s.captured_at DESC
        LIMIT :limit
        """,
    )
    fun searchFlow(
        query: String,
        includeSolved: Boolean = false,
        limit: Int = 50,
    ): Flow<List<ScreenshotItemEntity>>

    @Query("SELECT COUNT(*) FROM searchable_content")
    suspend fun getIndexedCount(): Int
}
