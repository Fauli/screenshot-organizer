package com.screenshotvault.data.prefs

data class UserPreferences(
    val selectedFolderUri: String? = null,
    val aiMode: AiMode = AiMode.OCR_ONLY,
    val hideSolvedByDefault: Boolean = true,
    val hideNoContentByDefault: Boolean = true,
    val lastScanAt: Long = 0L,
    val openAiApiKey: String? = null,
    val autoProcessOnStartup: Boolean = false,
)
