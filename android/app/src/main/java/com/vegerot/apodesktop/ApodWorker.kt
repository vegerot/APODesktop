package com.vegerot.apodesktop

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class ApodWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val success = ApodDesktop.updateWallpaper(applicationContext)
        return if (success) {
            Result.success()
        } else {
            Result.retry()
        }
    }
}
