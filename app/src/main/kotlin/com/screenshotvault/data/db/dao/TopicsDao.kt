package com.screenshotvault.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import com.screenshotvault.data.db.entities.ScreenshotItemEntity
import kotlinx.coroutines.flow.Flow

data class TopicCount(
    val topic: String,
    val count: Int,
)

@Dao
interface TopicsDao {

    @Query(
        """
        SELECT topics FROM searchable_content sc
        INNER JOIN screenshot_items s ON sc.screenshot_id = s.id
        WHERE s.solved = 0
        AND topics != ''
        """,
    )
    fun getAllTopicsJson(): Flow<List<String>>

    @Query(
        """
        SELECT s.* FROM screenshot_items s
        INNER JOIN searchable_content sc ON s.id = sc.screenshot_id
        WHERE sc.topics LIKE '%' || :topic || '%'
        AND (:includeSolved = 1 OR s.solved = 0)
        ORDER BY s.captured_at DESC
        LIMIT :limit
        """,
    )
    suspend fun getScreenshotsByTopic(
        topic: String,
        includeSolved: Boolean = false,
        limit: Int = 100,
    ): List<ScreenshotItemEntity>

    @Query(
        """
        SELECT s.* FROM screenshot_items s
        INNER JOIN searchable_content sc ON s.id = sc.screenshot_id
        WHERE sc.topics LIKE '%' || :topic || '%'
        AND (:includeSolved = 1 OR s.solved = 0)
        ORDER BY s.captured_at DESC
        LIMIT :limit
        """,
    )
    fun getScreenshotsByTopicFlow(
        topic: String,
        includeSolved: Boolean = false,
        limit: Int = 100,
    ): Flow<List<ScreenshotItemEntity>>
}
