package com.screenshotvault.data.repository

import com.screenshotvault.data.db.dao.EssenceDao
import com.screenshotvault.data.db.dao.TopicsDao
import com.screenshotvault.data.model.toDomain
import com.screenshotvault.domain.model.ScreenshotItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class TopicWithCount(
    val name: String,
    val count: Int,
)

@Singleton
class TopicsRepository @Inject constructor(
    private val topicsDao: TopicsDao,
    private val essenceDao: EssenceDao,
) {
    fun getTopicsWithCounts(): Flow<List<TopicWithCount>> {
        return topicsDao.getAllTopicsJson().map { topicsJsonList ->
            // Parse and aggregate topics
            val topicCounts = mutableMapOf<String, Int>()

            for (topicsStr in topicsJsonList) {
                // Topics are stored as space-separated in searchable_content
                val topics = topicsStr.split(" ")
                    .map { it.trim() }
                    .filter { it.isNotBlank() }

                for (topic in topics) {
                    topicCounts[topic] = (topicCounts[topic] ?: 0) + 1
                }
            }

            topicCounts.entries
                .map { TopicWithCount(it.key, it.value) }
                .sortedByDescending { it.count }
        }
    }

    suspend fun getScreenshotsByTopic(
        topic: String,
        includeSolved: Boolean = false,
    ): List<ScreenshotItem> {
        val entities = topicsDao.getScreenshotsByTopic(topic, includeSolved)
        return entities.map { entity ->
            val essence = essenceDao.getByScreenshotId(entity.id)
            entity.toDomain(essence)
        }
    }

    fun getScreenshotsByTopicFlow(
        topic: String,
        includeSolved: Boolean = false,
    ): Flow<List<ScreenshotItem>> {
        return topicsDao.getScreenshotsByTopicFlow(topic, includeSolved)
            .map { entities ->
                entities.map { entity ->
                    val essence = essenceDao.getByScreenshotId(entity.id)
                    entity.toDomain(essence)
                }
            }
    }
}
