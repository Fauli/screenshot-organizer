package com.screenshotvault.data.repository

import com.screenshotvault.data.db.dao.ActionCount
import com.screenshotvault.data.db.dao.ContentTypeCount
import com.screenshotvault.data.db.dao.DomainWithCount
import com.screenshotvault.data.db.dao.EssenceDao
import com.screenshotvault.data.db.dao.ScreenshotItemDao
import com.screenshotvault.data.db.dao.TimePeriodCount
import com.screenshotvault.data.model.toDomain
import com.screenshotvault.domain.model.ScreenshotItem
import com.screenshotvault.domain.model.TimePeriod
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InsightsRepository @Inject constructor(
    private val screenshotItemDao: ScreenshotItemDao,
    private val essenceDao: EssenceDao,
) {
    fun observeActionCounts(includeSolved: Boolean): Flow<List<ActionCount>> =
        screenshotItemDao.observeActionCounts(includeSolved)

    fun observeContentTypeCounts(includeSolved: Boolean): Flow<List<ContentTypeCount>> =
        screenshotItemDao.observeContentTypeCounts(includeSolved)

    fun observeTimePeriodCounts(includeSolved: Boolean): Flow<List<TimePeriodCount>> {
        val (todayStart, weekStart, monthStart) = getTimeBoundaries()
        return screenshotItemDao.observeTimePeriodCounts(
            todayStart = todayStart,
            weekStart = weekStart,
            monthStart = monthStart,
            includeSolved = includeSolved,
        )
    }

    fun observeDomainsWithCount(includeSolved: Boolean): Flow<List<DomainWithCount>> =
        screenshotItemDao.observeDomainsWithCountFiltered(includeSolved)

    fun observeTotalCount(includeSolved: Boolean): Flow<Int> =
        screenshotItemDao.observeTotalCountFiltered(includeSolved)

    fun observeThisWeekCount(includeSolved: Boolean): Flow<Int> {
        val (_, weekStart, _) = getTimeBoundaries()
        return screenshotItemDao.observeThisWeekCount(weekStart, includeSolved)
    }

    fun observePendingCount(): Flow<Int> = screenshotItemDao.observePendingCount()

    suspend fun getScreenshotsByAction(action: String, includeSolved: Boolean): List<ScreenshotItem> {
        val entities = screenshotItemDao.getByAction(action, includeSolved)
        return entities.map { entity ->
            val essence = essenceDao.getByScreenshotId(entity.id)
            entity.toDomain(essence)
        }
    }

    suspend fun getScreenshotsByContentType(type: String, includeSolved: Boolean): List<ScreenshotItem> {
        val entities = screenshotItemDao.getByContentType(type, includeSolved)
        return entities.map { entity ->
            val essence = essenceDao.getByScreenshotId(entity.id)
            entity.toDomain(essence)
        }
    }

    suspend fun getScreenshotsByTimePeriod(period: TimePeriod, includeSolved: Boolean): List<ScreenshotItem> {
        val (start, end) = getTimeRangeForPeriod(period)
        val entities = screenshotItemDao.getByTimeRange(start, end, includeSolved)
        return entities.map { entity ->
            val essence = essenceDao.getByScreenshotId(entity.id)
            entity.toDomain(essence)
        }
    }

    suspend fun getScreenshotsByDomain(domain: String, includeSolved: Boolean): List<ScreenshotItem> {
        val entities = screenshotItemDao.getByDomainFiltered(domain, includeSolved)
        return entities.map { entity ->
            val essence = essenceDao.getByScreenshotId(entity.id)
            entity.toDomain(essence)
        }
    }

    private fun getTimeBoundaries(): Triple<Long, Long, Long> {
        val calendar = Calendar.getInstance()

        // Today start (midnight)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val todayStart = calendar.timeInMillis

        // Week start (beginning of current week, Sunday)
        calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
        val weekStart = calendar.timeInMillis

        // Month start (first day of current month)
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val monthStart = calendar.timeInMillis

        return Triple(todayStart, weekStart, monthStart)
    }

    private fun getTimeRangeForPeriod(period: TimePeriod): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        val now = System.currentTimeMillis()

        return when (period) {
            TimePeriod.TODAY -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val todayStart = calendar.timeInMillis
                Pair(todayStart, now + 1)
            }
            TimePeriod.THIS_WEEK -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val todayStart = calendar.timeInMillis

                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                val weekStart = calendar.timeInMillis

                Pair(weekStart, todayStart)
            }
            TimePeriod.THIS_MONTH -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                val weekStart = calendar.timeInMillis

                calendar.set(Calendar.DAY_OF_MONTH, 1)
                val monthStart = calendar.timeInMillis

                Pair(monthStart, weekStart)
            }
            TimePeriod.OLDER -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                val monthStart = calendar.timeInMillis

                Pair(0L, monthStart)
            }
        }
    }
}
