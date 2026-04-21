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
        
        // 2. Update Sticky Notification
        showStickyNotification(context, finalInfo)
        
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

    private fun showStickyNotification(context: Context, rideInfo: RideInfo) {
        val nm = NotificationManagerCompat.from(context)
        if (!SettingsManager.isStickyNotificationEnabled(context)) {
            nm.cancel(NOTIFICATION_ID)
            return
        }

        createNotificationChannel(context)

        var intent = context.packageManager.getLaunchIntentForPackage("com.moveinsync")
        if (intent == null) {
            intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        }
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val views = RemoteViews(context.packageName, R.layout.notification_custom)
        val cabNo = rideInfo.cabNo ?: "Unknown Cab"
        
        var subtitle = ""
        var otpLabel = "OTP"
        var activeOtp = "----"

        if (rideInfo.type == NotificationType.LOGIN) {
            subtitle = "Morning Commute | ETP: ${rideInfo.etp ?: "--:--"}"
            otpLabel = "LOGIN OTP"
            activeOtp = rideInfo.signInOtp ?: "----"
        } else if (rideInfo.type == NotificationType.LOGOUT) {
            subtitle = "Evening Drop | Route: ${rideInfo.routeNo ?: "R-??"}"
            otpLabel = "DROP OTP"
            activeOtp = rideInfo.signOutOtp ?: "----"
        } else if (rideInfo.type == NotificationType.APPROACHING) {
            subtitle = "CAB APPROACHING!" + if (rideInfo.etp != null) " | ETP: ${rideInfo.etp}" else ""
            otpLabel = "LOGIN OTP"
            activeOtp = rideInfo.signInOtp ?: "----"
        }

        views.setTextViewText(R.id.notif_title, "Cab: $cabNo")
        views.setTextViewText(R.id.notif_subtitle, subtitle)
        views.setTextViewText(R.id.notif_otp_label, otpLabel)
        views.setTextViewText(R.id.notif_otp, activeOtp)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setStyle(androidx.core.app.NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(views)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(true)
            .setContentIntent(pendingIntent)

        if (rideInfo.type == NotificationType.APPROACHING) {
            builder.setVibrate(longArrayOf(1000, 1000, 1000))
        }

        try {
            nm.notify(NOTIFICATION_ID, builder.build())
        } catch (e: SecurityException) {
            // Permission lacking silently
        }
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
