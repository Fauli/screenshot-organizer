package com.screenshotvault.data.repository

import com.screenshotvault.data.db.dao.DomainWithCount
import com.screenshotvault.data.db.dao.EssenceDao
import com.screenshotvault.data.db.dao.ScreenshotItemDao
import com.screenshotvault.data.model.toDomain
import com.screenshotvault.domain.model.ScreenshotItem
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DomainsRepository @Inject constructor(
    private val screenshotItemDao: ScreenshotItemDao,
    private val essenceDao: EssenceDao,
) {
    fun observeDomainsWithCount(): Flow<List<DomainWithCount>> =
        screenshotItemDao.observeDomainsWithCount()

    suspend fun getScreenshotsForDomain(domain: String): List<ScreenshotItem> {
        val entities = screenshotItemDao.getByDomain(domain)
        return entities.map { entity ->
            val essence = essenceDao.getByScreenshotId(entity.id)
            entity.toDomain(essence)
        }
    }
}
