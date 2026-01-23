package com.screenshotvault.domain.usecase

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.screenshotvault.data.repository.PreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class SelectFolderUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesRepository: PreferencesRepository,
) {
    suspend fun saveSelectedFolder(uri: Uri) {
        // Take persistable permission
        val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
            Intent.FLAG_GRANT_WRITE_URI_PERMISSION

        try {
            context.contentResolver.takePersistableUriPermission(uri, takeFlags)
        } catch (e: SecurityException) {
            // Try with read-only permission
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }

        preferencesRepository.setSelectedFolderUri(uri.toString())
    }

    suspend fun getSelectedFolderUri(): Uri? {
        val uriString = preferencesRepository.getSelectedFolderUri() ?: return null
        return Uri.parse(uriString)
    }

    suspend fun clearSelectedFolder() {
        val uriString = preferencesRepository.getSelectedFolderUri() ?: return
        val uri = Uri.parse(uriString)

        try {
            context.contentResolver.releasePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        } catch (e: SecurityException) {
            // Permission might already be released
        }

        preferencesRepository.setSelectedFolderUri(null)
    }

    fun hasValidPermission(uri: Uri): Boolean {
        val persistedUris = context.contentResolver.persistedUriPermissions
        return persistedUris.any { it.uri == uri && it.isReadPermission }
    }
}
