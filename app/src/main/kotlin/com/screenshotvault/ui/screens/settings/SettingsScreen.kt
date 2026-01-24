package com.screenshotvault.ui.screens.settings

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.screenshotvault.data.prefs.AiMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToAbout: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                viewModel.onFolderSelected(uri)
            }
        }
    }

    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        uri?.let { viewModel.exportBackup(it) }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let { viewModel.importBackup(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Folder Selection Card
            Card(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                ) {
                    Text(
                        text = "Screenshot Folder",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (uiState.preferences.selectedFolderUri != null) {
                        Text(
                            text = uiState.preferences.selectedFolderUri!!
                                .substringAfterLast("/")
                                .substringAfterLast("%3A"),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row {
                            OutlinedButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                                        addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    folderPickerLauncher.launch(intent)
                                },
                            ) {
                                Icon(Icons.Default.Folder, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Change")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            TextButton(onClick = viewModel::clearFolder) {
                                Text("Clear")
                            }
                        }
                    } else {
                        Text(
                            text = "No folder selected",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                                    addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                folderPickerLauncher.launch(intent)
                            },
                        ) {
                            Icon(Icons.Default.Folder, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Select Folder")
                        }
                    }
                }
            }

            // Stats Card
            if (uiState.totalScreenshots > 0) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                    ) {
                        Text(
                            text = "Library",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${uiState.totalScreenshots} screenshots (${uiState.processedCount} processed)",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        if (uiState.noContentCount > 0) {
                            Text(
                                text = "${uiState.noContentCount} with no content",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        // Processing indicator
                        if (uiState.currentlyProcessing > 0 || uiState.batchTotal > 0) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                    )
                                    val progressText = if (uiState.batchTotal > 0) {
                                        val current = uiState.batchTotal - uiState.batchRemaining + 1
                                        "Processing $current of ${uiState.batchTotal}"
                                    } else {
                                        "Processing ${uiState.currentlyProcessing} item${if (uiState.currentlyProcessing > 1) "s" else ""}"
                                    }
                                    Text(
                                        text = progressText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                                OutlinedButton(
                                    onClick = { viewModel.resetStuckProcessing() },
                                ) {
                                    Text("Reset stuck items")
                                }
                            }
                        }

                        if (uiState.pendingProcessing > 0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${uiState.pendingProcessing} waiting to process",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary,
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "Process batch:",
                                style = MaterialTheme.typography.labelMedium,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                listOf(5, 10, 50, 100).forEach { count ->
                                    OutlinedButton(
                                        onClick = { viewModel.processBatch(count) },
                                        enabled = uiState.pendingProcessing > 0,
                                    ) {
                                        Text("$count")
                                    }
                                }
                                OutlinedButton(
                                    onClick = viewModel::processNow,
                                    enabled = uiState.pendingProcessing > 0,
                                ) {
                                    Text("All")
                                }
                            }
                        }

                        // Reprocess button (useful when switching AI modes)
                        Spacer(modifier = Modifier.height(16.dp))

                        var showReprocessDialog by remember { mutableStateOf(false) }

                        OutlinedButton(
                            onClick = { showReprocessDialog = true },
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Reset All for Reprocessing")
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Use after switching AI modes to reprocess with new settings",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        if (showReprocessDialog) {
                            AlertDialog(
                                onDismissRequest = { showReprocessDialog = false },
                                title = { Text("Reset for Reprocessing?") },
                                text = {
                                    Text(
                                        "This will delete all extracted data (titles, summaries, topics) " +
                                        "and mark all ${uiState.totalScreenshots} screenshots for reprocessing.\n\n" +
                                        "You'll need to process them again using the batch buttons."
                                    )
                                },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            viewModel.resetForReprocessing()
                                            showReprocessDialog = false
                                        },
                                    ) {
                                        Text("Reset All")
                                    }
                                },
                                dismissButton = {
                                    TextButton(
                                        onClick = { showReprocessDialog = false },
                                    ) {
                                        Text("Cancel")
                                    }
                                },
                            )
                        }
                    }
                }
            }

            // Scan Card
            Card(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                ) {
                    Text(
                        text = "Scan",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (uiState.preferences.lastScanAt > 0) {
                        val dateFormat = SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault())
                        Text(
                            text = "Last scan: ${dateFormat.format(Date(uiState.preferences.lastScanAt))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Button(
                        onClick = viewModel::scanNow,
                        enabled = !uiState.isScanning && uiState.preferences.selectedFolderUri != null,
                    ) {
                        if (uiState.isScanning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Scanning...")
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Scan Now")
                        }
                    }

                    // Scan Result
                    uiState.lastScanResult?.let { result ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Found ${result.scannedCount} files: ${result.newCount} new, ${result.skippedCount} skipped",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }

                    // Error Message
                    uiState.errorMessage?.let { error ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            // AI Settings Card
            Card(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                ) {
                    Text(
                        text = "AI Processing",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // AI Mode Selection
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilterChip(
                            selected = uiState.preferences.aiMode == AiMode.OCR_ONLY,
                            onClick = { viewModel.setAiMode(AiMode.OCR_ONLY) },
                            label = { Text("OCR Only") },
                        )
                        FilterChip(
                            selected = uiState.preferences.aiMode == AiMode.OPENAI,
                            onClick = { viewModel.setAiMode(AiMode.OPENAI) },
                            label = { Text("OpenAI") },
                        )
                    }

                    // OpenAI API Key Input
                    if (uiState.preferences.aiMode == AiMode.OPENAI) {
                        Spacer(modifier = Modifier.height(12.dp))

                        var apiKeyInput by remember(uiState.preferences.openAiApiKey) {
                            mutableStateOf(uiState.preferences.openAiApiKey ?: "")
                        }
                        var showApiKey by remember { mutableStateOf(false) }

                        OutlinedTextField(
                            value = apiKeyInput,
                            onValueChange = { apiKeyInput = it },
                            label = { Text("OpenAI API Key") },
                            placeholder = { Text("sk-...") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            leadingIcon = {
                                Icon(Icons.Default.Key, contentDescription = null)
                            },
                            trailingIcon = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    IconButton(onClick = { showApiKey = !showApiKey }) {
                                        Icon(
                                            if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = if (showApiKey) "Hide" else "Show",
                                        )
                                    }
                                }
                            },
                            visualTransformation = if (showApiKey) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Button(
                                onClick = { viewModel.setOpenAiApiKey(apiKeyInput) },
                                enabled = apiKeyInput != (uiState.preferences.openAiApiKey ?: ""),
                            ) {
                                Text("Save Key")
                            }
                            if (!uiState.preferences.openAiApiKey.isNullOrBlank()) {
                                OutlinedButton(
                                    onClick = {
                                        apiKeyInput = ""
                                        viewModel.setOpenAiApiKey("")
                                    },
                                ) {
                                    Text("Clear")
                                }
                            }
                        }

                        if (uiState.preferences.openAiApiKey.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Enter your OpenAI API key to enable GPT-5.2 vision processing",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "✓ API key configured. Screenshots will be processed with GPT-5.2.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Using on-device ML Kit OCR (offline, faster, less accurate)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Auto-process switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Auto-process on startup",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                text = "Automatically process pending screenshots when app opens",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = uiState.preferences.autoProcessOnStartup,
                            onCheckedChange = { viewModel.setAutoProcessOnStartup(it) },
                        )
                    }
                }
            }

            // Backup & Import Card
            Card(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                ) {
                    Text(
                        text = "Backup & Import",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Export or import your screenshot metadata and essences",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(
                            onClick = {
                                val filename = "screenshot_vault_backup_${
                                    SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                                        .format(Date())
                                }.json"
                                backupLauncher.launch(filename)
                            },
                            enabled = !uiState.isBackingUp && uiState.totalScreenshots > 0,
                        ) {
                            if (uiState.isBackingUp) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Icon(Icons.Default.Upload, contentDescription = null)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Export")
                        }

                        OutlinedButton(
                            onClick = {
                                importLauncher.launch(arrayOf("application/json"))
                            },
                            enabled = !uiState.isImporting,
                        ) {
                            if (uiState.isImporting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Icon(Icons.Default.Download, contentDescription = null)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Import")
                        }
                    }

                    // Success Message
                    uiState.successMessage?.let { message ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            // About Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = onNavigateToAbout,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "About",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Screenshot Vault v1.0.0",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Go to About",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
