package com.example.cabglance2

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class MoveInSyncNotificationListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        sbn?.let {
            val packageName = it.packageName
            // We can listen to SMS apps or the specific MoveInSync app
            // Common MoveInSync package name involves "com.moveinsync" or we just parse all incoming text
            // For safety and privacy, you might want to filter this.
            
            val extras = it.notification.extras
            val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

            // Check if we are in Notification App sourcing mode
            if (SettingsManager.getSourcingMode(applicationContext) != SourcingMode.APP_NOTIFICATION) {
                return
            }

            if (text.contains("MovelnSync", ignoreCase = true) || text.contains("MoveInSync", ignoreCase = true) || title.contains("MoveInSync", ignoreCase = true)) {
                val rideInfo = OtpParser.parseMessage(text)
                if (rideInfo.type != NotificationType.UNKNOWN) {
                    Log.d("CabGlance", "Parsed Ride Info: $rideInfo")
                    // Broadcast this to updating the Widget and our own Sticky Notification
                    WidgetUpdateHelper.updateWidgetAndNotification(applicationContext, rideInfo)
                }
            }
        }
    }
}

