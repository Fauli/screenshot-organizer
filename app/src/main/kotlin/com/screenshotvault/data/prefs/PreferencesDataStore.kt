package com.screenshotvault.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "user_preferences",
)

@Singleton
class PreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val SELECTED_FOLDER_URI = stringPreferencesKey("selected_folder_uri")
        val AI_MODE = stringPreferencesKey("ai_mode")
        val HIDE_SOLVED_BY_DEFAULT = booleanPreferencesKey("hide_solved_by_default")
        val HIDE_NO_CONTENT_BY_DEFAULT = booleanPreferencesKey("hide_no_content_by_default")
        val LAST_SCAN_AT = longPreferencesKey("last_scan_at")
        val OPENAI_API_KEY = stringPreferencesKey("openai_api_key")
        val AUTO_PROCESS_ON_STARTUP = booleanPreferencesKey("auto_process_on_startup")
    }

    val userPreferences: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        UserPreferences(
            selectedFolderUri = prefs[Keys.SELECTED_FOLDER_URI],
            aiMode = prefs[Keys.AI_MODE]?.let {
                try { AiMode.valueOf(it) } catch (e: Exception) { AiMode.OCR_ONLY }
            } ?: AiMode.OCR_ONLY,
            hideSolvedByDefault = prefs[Keys.HIDE_SOLVED_BY_DEFAULT] ?: true,
            hideNoContentByDefault = prefs[Keys.HIDE_NO_CONTENT_BY_DEFAULT] ?: true,
            lastScanAt = prefs[Keys.LAST_SCAN_AT] ?: 0L,
            openAiApiKey = prefs[Keys.OPENAI_API_KEY],
            autoProcessOnStartup = prefs[Keys.AUTO_PROCESS_ON_STARTUP] ?: false,
        )
    }

    suspend fun setSelectedFolderUri(uri: String?) {
        context.dataStore.edit { prefs ->
            if (uri != null) {
                prefs[Keys.SELECTED_FOLDER_URI] = uri
            } else {
                prefs.remove(Keys.SELECTED_FOLDER_URI)
            }
        }
    }

    suspend fun setAiMode(mode: AiMode) {
        context.dataStore.edit { prefs ->
            prefs[Keys.AI_MODE] = mode.name
        }
    }

    suspend fun setHideSolvedByDefault(hide: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.HIDE_SOLVED_BY_DEFAULT] = hide
        }
    }

    suspend fun setLastScanAt(timestamp: Long) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LAST_SCAN_AT] = timestamp
        }
    }

    suspend fun setOpenAiApiKey(apiKey: String?) {
        context.dataStore.edit { prefs ->
            if (apiKey != null) {
                prefs[Keys.OPENAI_API_KEY] = apiKey
            } else {
                prefs.remove(Keys.OPENAI_API_KEY)
            }
        }
    }

    suspend fun setAutoProcessOnStartup(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.AUTO_PROCESS_ON_STARTUP] = enabled
        }
    }
}
