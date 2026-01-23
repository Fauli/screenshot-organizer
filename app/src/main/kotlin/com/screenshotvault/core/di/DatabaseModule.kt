package com.screenshotvault.core.di

import android.content.Context
import androidx.room.Room
import com.screenshotvault.data.db.AppDatabase
import com.screenshotvault.data.db.dao.EssenceDao
import com.screenshotvault.data.db.dao.ScreenshotItemDao
import com.screenshotvault.data.db.dao.SearchDao
import com.screenshotvault.data.db.dao.TopicsDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
    ): AppDatabase = Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        AppDatabase.DATABASE_NAME,
    )
        .addMigrations(AppDatabase.MIGRATION_1_2)
        .build()

    @Provides
    fun provideScreenshotItemDao(database: AppDatabase): ScreenshotItemDao =
        database.screenshotItemDao()

    @Provides
    fun provideEssenceDao(database: AppDatabase): EssenceDao =
        database.essenceDao()

    @Provides
    fun provideSearchDao(database: AppDatabase): SearchDao =
        database.searchDao()

    @Provides
    fun provideTopicsDao(database: AppDatabase): TopicsDao =
        database.topicsDao()
}
