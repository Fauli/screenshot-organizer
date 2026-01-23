package com.screenshotvault.data.repository

import com.screenshotvault.data.prefs.AiMode
import com.screenshotvault.data.prefs.PreferencesDataStore
import com.screenshotvault.data.prefs.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesRepository @Inject constructor(
    private val preferencesDataStore: PreferencesDataStore,
) {
    val userPreferences: Flow<UserPreferences> = preferencesDataStore.userPreferences

    suspend fun getSelectedFolderUri(): String? =
        preferencesDataStore.userPreferences.first().selectedFolderUri

    suspend fun setSelectedFolderUri(uri: String?) =
        preferencesDataStore.setSelectedFolderUri(uri)

    suspend fun getAiMode(): AiMode =
        preferencesDataStore.userPreferences.first().aiMode

    suspend fun setAiMode(mode: AiMode) =
        preferencesDataStore.setAiMode(mode)

    suspend fun getHideSolvedByDefault(): Boolean =
        preferencesDataStore.userPreferences.first().hideSolvedByDefault

    suspend fun setHideSolvedByDefault(hide: Boolean) =
        preferencesDataStore.setHideSolvedByDefault(hide)

    suspend fun getLastScanAt(): Long =
        preferencesDataStore.userPreferences.first().lastScanAt

    suspend fun setLastScanAt(timestamp: Long) =
        preferencesDataStore.setLastScanAt(timestamp)
}
