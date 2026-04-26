package com.example.cabglance2

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

class IdleWorker(appContext: Context, workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {

    override fun doWork(): Result {
        val rideInfo = RideInfo(
            type = NotificationType.IDLE,
            rawText = "Idle"
        )
        WidgetUpdateHelper.updateWidgetAndNotification(applicationContext, rideInfo)
        return Result.success()
    }
}
