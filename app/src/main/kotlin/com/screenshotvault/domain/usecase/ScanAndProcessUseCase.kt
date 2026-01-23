package com.screenshotvault.domain.usecase

import com.screenshotvault.ingest.workers.WorkScheduler
import javax.inject.Inject

class ScanAndProcessUseCase @Inject constructor(
    private val workScheduler: WorkScheduler,
) {
    operator fun invoke() {
        workScheduler.triggerScanAndProcess()
    }
}
