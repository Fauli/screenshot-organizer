package com.screenshotvault.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.screenshotvault.data.db.dao.EssenceDao
import com.screenshotvault.data.db.dao.ScreenshotItemDao
import com.screenshotvault.data.db.dao.SearchDao
import com.screenshotvault.data.db.dao.TopicsDao
import com.screenshotvault.data.db.entities.EssenceEntity
import com.screenshotvault.data.db.entities.ScreenshotItemEntity
import com.screenshotvault.data.db.fts.SearchIndexEntity
import com.screenshotvault.data.db.fts.SearchableContent

@Database(
    entities = [
        ScreenshotItemEntity::class,
        EssenceEntity::class,
        SearchableContent::class,
        SearchIndexEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun screenshotItemDao(): ScreenshotItemDao

    abstract fun essenceDao(): EssenceDao

    abstract fun searchDao(): SearchDao

    abstract fun topicsDao(): TopicsDao

    companion object {
        const val DATABASE_NAME = "screenshot_vault.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE screenshot_items ADD COLUMN no_content INTEGER NOT NULL DEFAULT 0")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_screenshot_items_no_content ON screenshot_items(no_content)")
            }
        }
    }
}
