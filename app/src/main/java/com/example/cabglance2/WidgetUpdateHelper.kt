package com.example.cabglance2

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object WidgetUpdateHelper {
    const val CHANNEL_ID = "MoveInSyncSticky"
    const val NOTIFICATION_ID = 1001

    fun updateWidgetAndNotification(context: Context, rideInfo: RideInfo) {
        // Ignore random SMS or notifications that are not MoveInSync
        if (rideInfo.type == NotificationType.UNKNOWN) return

        // 1. Persist data
        RideDataStore.saveRideInfo(context, rideInfo)
        val finalInfo = RideDataStore.getRideInfo(context) ?: rideInfo
        
        // Schedule Idle Worker
        if (finalInfo.type == NotificationType.LOGIN || finalInfo.type == NotificationType.LOGOUT) {
            scheduleIdleState(context, finalInfo.etp)
        } else if (finalInfo.type == NotificationType.IDLE) {
            androidx.work.WorkManager.getInstance(context).cancelUniqueWork("IdleStateTransition")
        }
        
        // Handle Sticky Notification Service
        if (!SettingsManager.isStickyNotificationEnabled(context) || finalInfo.type == NotificationType.IDLE || finalInfo.type == NotificationType.LOGIN) {
            context.stopService(Intent(context, StickyNotificationService::class.java))
        } else if (finalInfo.type == NotificationType.APPROACHING || finalInfo.type == NotificationType.LOGOUT) {
            val intent = Intent(context, StickyNotificationService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        // 3. Trigger AppWidget updates
        triggerWidgetUpdate(context, MoveInSyncAppWidgetProvider::class.java)
        triggerWidgetUpdate(context, PocketWidgetProvider::class.java)
        triggerWidgetUpdate(context, DashboardWidgetProvider::class.java)
    }

    fun triggerWidgetUpdate(context: Context, providerClass: Class<*>) {
        val intent = Intent(context, providerClass).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        }
        val ids = AppWidgetManager.getInstance(context).getAppWidgetIds(ComponentName(context, providerClass))
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        context.sendBroadcast(intent)
    }

    private fun scheduleIdleState(context: Context, etpStr: String?) {
        val workManager = androidx.work.WorkManager.getInstance(context)
        if (etpStr == null) {
            workManager.cancelUniqueWork("IdleStateTransition")
            return
        }
        try {
            val parts = etpStr.split(":")
            val targetHour = parts[0].toInt()
            val targetMin = parts[1].toInt()

            val now = java.util.Calendar.getInstance()
            val target = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, targetHour)
                set(java.util.Calendar.MINUTE, targetMin)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }
            
            // If ETP is < now by more than 12 hours, assume it's for tomorrow
            if (now.timeInMillis - target.timeInMillis > 12 * 60 * 60 * 1000L) {
                target.add(java.util.Calendar.DAY_OF_YEAR, 1)
            }

            // Trigger IDLE exactly 2 hours after ETP/Logout
            target.add(java.util.Calendar.HOUR_OF_DAY, 2)

            var delayMs = target.timeInMillis - now.timeInMillis
            if (delayMs < 0) delayMs = 0

            val idleWork = androidx.work.OneTimeWorkRequestBuilder<IdleWorker>()
                .setInitialDelay(delayMs, java.util.concurrent.TimeUnit.MILLISECONDS)
                .build()

            workManager.enqueueUniqueWork(
                "IdleStateTransition",
                androidx.work.ExistingWorkPolicy.REPLACE,
                idleWork
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showStickyNotification(context: Context, rideInfo: RideInfo) {
        // Obsolete: Handled by StickyNotificationService now.
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "MoveInSync Sticky Service"
            val descriptionText = "Displays the current ride OTP and details on top"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
