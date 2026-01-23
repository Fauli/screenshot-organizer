package com.screenshotvault.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.screenshotvault.data.db.entities.EssenceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EssenceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(essence: EssenceEntity)

    @Query("SELECT * FROM essences WHERE screenshot_id = :screenshotId")
    suspend fun getByScreenshotId(screenshotId: String): EssenceEntity?

    @Query("SELECT * FROM essences WHERE screenshot_id = :screenshotId")
    fun observeByScreenshotId(screenshotId: String): Flow<EssenceEntity?>

    @Query("DELETE FROM essences WHERE screenshot_id = :screenshotId")
    suspend fun deleteByScreenshotId(screenshotId: String)

    @Query("DELETE FROM essences")
    suspend fun deleteAll()

    @Query("SELECT * FROM essences")
    suspend fun getAll(): List<EssenceEntity>
}
