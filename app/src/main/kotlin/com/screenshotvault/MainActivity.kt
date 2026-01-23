package com.screenshotvault

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.screenshotvault.data.prefs.PreferencesDataStore
import com.screenshotvault.ingest.workers.WorkScheduler
import com.screenshotvault.ui.navigation.ScreenshotVaultNavHost
import com.screenshotvault.ui.theme.ScreenshotVaultTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var preferencesDataStore: PreferencesDataStore

    @Inject
    lateinit var workScheduler: WorkScheduler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Check if auto-process on startup is enabled
        lifecycleScope.launch {
            val prefs = preferencesDataStore.userPreferences.first()
            if (prefs.autoProcessOnStartup && prefs.selectedFolderUri != null) {
                workScheduler.triggerProcessing()
            }
        }

        setContent {
            ScreenshotVaultTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    ScreenshotVaultNavHost()
                }
            }
        }
    }
}
