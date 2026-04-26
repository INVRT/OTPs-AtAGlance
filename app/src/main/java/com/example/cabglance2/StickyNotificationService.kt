package com.example.cabglance2

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat

class StickyNotificationService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val rideInfo = RideDataStore.getRideInfo(this)
        
        if (rideInfo == null || !SettingsManager.isStickyNotificationEnabled(this)) {
            stopSelf()
            return START_NOT_STICKY
        }

        createNotificationChannel()

        var launchIntent = packageManager.getLaunchIntentForPackage("com.moveinsync")
        if (launchIntent == null) {
            launchIntent = Intent(this, MainActivity::class.java).apply {
                this.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, launchIntent, PendingIntent.FLAG_IMMUTABLE)

        var titleText = ""
        if (rideInfo.type == NotificationType.APPROACHING) {
            titleText = "CAB APPROACHING!"
        } else if (rideInfo.type == NotificationType.LOGOUT) {
            titleText = "EVENING DROP OTP"
        } else {
            titleText = "COMMUTE OTP" // Fallback
        }

        val contentText = "IN: ${rideInfo.signInOtp ?: "----"}   •   OUT: ${rideInfo.signOutOtp ?: "----"}"

        val appIconBitmap = android.graphics.BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher)

        val builder = NotificationCompat.Builder(this, WidgetUpdateHelper.CHANNEL_ID)
            .setSmallIcon(R.drawable.widget_dot) // Alpha-only icon for status bar
            .setLargeIcon(appIconBitmap) // Full color app icon for the notification body
            .setContentTitle(titleText)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(pendingIntent)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(WidgetUpdateHelper.NOTIFICATION_ID, builder.build(), android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(WidgetUpdateHelper.NOTIFICATION_ID, builder.build())
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "MoveInSync Sticky Service"
            val descriptionText = "Displays the current ride OTP and details on top"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(WidgetUpdateHelper.CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
