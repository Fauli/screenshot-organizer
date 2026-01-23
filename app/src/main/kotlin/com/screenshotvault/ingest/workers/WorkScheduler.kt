package com.screenshotvault.ingest.workers

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val workManager = WorkManager.getInstance(context)

    fun schedulePeriodicScan() {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        val periodicWork = PeriodicWorkRequestBuilder<ScanScreenshotsWorker>(
            repeatInterval = 12,
            repeatIntervalTimeUnit = TimeUnit.HOURS,
        )
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniquePeriodicWork(
            PERIODIC_SCAN_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicWork,
        )
    }

    fun triggerImmediateScan() {
        val scanWork = OneTimeWorkRequestBuilder<ScanScreenshotsWorker>()
            .build()

        workManager.enqueueUniqueWork(
            ONE_TIME_SCAN_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            scanWork,
        )
    }

    fun triggerProcessing(batchSize: Int? = null) {
        val inputData = if (batchSize != null && batchSize > 0) {
            Data.Builder()
                .putInt(ProcessScreenshotsWorker.KEY_BATCH_SIZE, batchSize)
                .build()
        } else {
            Data.EMPTY
        }

        val processWork = OneTimeWorkRequestBuilder<ProcessScreenshotsWorker>()
            .setInputData(inputData)
            .build()

        workManager.enqueueUniqueWork(
            PROCESS_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            processWork,
        )
    }

    fun triggerScanAndProcess() {
        val scanWork = OneTimeWorkRequestBuilder<ScanScreenshotsWorker>()
            .build()

        val processWork = OneTimeWorkRequestBuilder<ProcessScreenshotsWorker>()
            .build()

        workManager.beginUniqueWork(
            SCAN_AND_PROCESS_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            scanWork,
        )
            .then(processWork)
            .enqueue()
    }

    fun cancelAllWork() {
        workManager.cancelAllWork()
    }

    companion object {
        private const val PERIODIC_SCAN_WORK_NAME = "periodic_scan"
        private const val ONE_TIME_SCAN_WORK_NAME = "one_time_scan"
        private const val PROCESS_WORK_NAME = "process_screenshots"
        private const val SCAN_AND_PROCESS_WORK_NAME = "scan_and_process"
    }
}
