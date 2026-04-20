package com.example.cabglance2

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews

class DashboardWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_dashboard_4x4)
            val rideInfo = RideDataStore.getRideInfo(context)

            // Open MoveInSync Intent mapped fully to the Root or the Cab Card
            val pm = context.packageManager
            var launchIntent = pm.getLaunchIntentForPackage("com.moveinsync")
            if (launchIntent == null) {
                launchIntent = Intent(context, MainActivity::class.java)
            }
            val pendingIntent = PendingIntent.getActivity(
                context, 0, launchIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

            if (rideInfo == null) {
                views.setTextViewText(R.id.widget_dash_status, "No Active Commute")
                views.setTextViewText(R.id.widget_dash_cab, "---")
                views.setTextViewText(R.id.widget_dash_route, "---")
                views.setTextViewText(R.id.widget_dash_otp_in, "----")
                views.setTextViewText(R.id.widget_dash_otp_out, "----")
                views.setInt(R.id.widget_dash_dot, "setColorFilter", Color.GRAY)
            } else {
                var headerText = ""
                var dotColor = Color.parseColor("#4CAF50") // Green
                
                if (rideInfo.type == NotificationType.LOGIN) {
                    headerText = "Morning Commute"
                } else if (rideInfo.type == NotificationType.LOGOUT) {
                    headerText = "Evening Commute"
                    dotColor = Color.parseColor("#F44336") // Red
                } else if (rideInfo.type == NotificationType.APPROACHING) {
                    headerText = "CAB APPROACHING!"
                    dotColor = Color.parseColor("#FFC107") // Yellow
                }
                
                views.setTextViewText(R.id.widget_dash_status, headerText)
                views.setInt(R.id.widget_dash_dot, "setColorFilter", dotColor)

                views.setTextViewText(R.id.widget_dash_cab, rideInfo.cabNo ?: "Unknown")

                val routeTimeText = if (rideInfo.etp != null) {
                    "${rideInfo.etp}"
                } else if (rideInfo.routeNo != null) {
                    "${rideInfo.routeNo}"
                } else {
                    "---"
                }
                views.setTextViewText(R.id.widget_dash_route, routeTimeText)

                views.setTextViewText(R.id.widget_dash_otp_in, rideInfo.signInOtp ?: "----")
                views.setTextViewText(R.id.widget_dash_otp_out, rideInfo.signOutOtp ?: "----")
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
