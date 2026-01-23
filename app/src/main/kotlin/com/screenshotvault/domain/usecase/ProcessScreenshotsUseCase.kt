package com.screenshotvault.domain.usecase

import com.screenshotvault.ingest.workers.WorkScheduler
import javax.inject.Inject

class ProcessScreenshotsUseCase @Inject constructor(
    private val workScheduler: WorkScheduler,
) {
    operator fun invoke(batchSize: Int? = null) {
        workScheduler.triggerProcessing(batchSize)
    }
}
