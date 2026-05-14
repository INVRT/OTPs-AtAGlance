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

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: android.os.Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        updateAppWidget(context, appWidgetManager, appWidgetId)
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

            val callIntent = Intent(Intent.ACTION_DIAL, android.net.Uri.parse("tel:+914440114070"))
            callIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            val callPendingIntent = PendingIntent.getActivity(
                context, 1, callIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_dash_call_top, callPendingIntent)

            if (rideInfo == null) {
                views.setViewVisibility(R.id.widget_dash_call_top, android.view.View.GONE)
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
                } else if (rideInfo.type == NotificationType.IDLE) {
                    headerText = "IDLE"
                    dotColor = Color.parseColor("#D3D3D3") // Light grey
                }
                
                views.setTextViewText(R.id.widget_dash_status, headerText)
                views.setInt(R.id.widget_dash_dot, "setColorFilter", dotColor)

                if (rideInfo.type == NotificationType.IDLE) {
                    views.setViewVisibility(R.id.widget_dash_call_top, android.view.View.GONE)
                    views.setViewVisibility(R.id.widget_dash_active_top, android.view.View.GONE)
                    views.setViewVisibility(R.id.widget_dash_active_bottom, android.view.View.GONE)
                    views.setViewVisibility(R.id.widget_dash_idle_text, android.view.View.VISIBLE)
                } else {
                    views.setViewVisibility(R.id.widget_dash_call_top, android.view.View.VISIBLE)
                    views.setViewVisibility(R.id.widget_dash_active_top, android.view.View.VISIBLE)
                    views.setViewVisibility(R.id.widget_dash_active_bottom, android.view.View.VISIBLE)
                    views.setViewVisibility(R.id.widget_dash_idle_text, android.view.View.GONE)

                    views.setTextViewText(R.id.widget_dash_cab, rideInfo.cabNo ?: "Unknown")

                    val routeTimeText = if (rideInfo.type == NotificationType.LOGIN && rideInfo.etp != null) {
                        "${rideInfo.etp}"
                    } else if (rideInfo.type == NotificationType.LOGOUT && rideInfo.routeNo != null) {
                        "${rideInfo.routeNo}"
                    } else if (rideInfo.etp != null) {
                        "${rideInfo.etp}"
                    } else if (rideInfo.routeNo != null) {
                        "${rideInfo.routeNo}"
                    } else {
                        "---"
                    }
                    views.setTextViewText(R.id.widget_dash_route, routeTimeText)

                    views.setTextViewText(R.id.widget_dash_otp_in, rideInfo.signInOtp ?: "----")
                    views.setTextViewText(R.id.widget_dash_otp_out, rideInfo.signOutOtp ?: "----")

                    val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
                    val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
                    
                    if (minWidth >= 280) { // Widget is widened
                        views.setViewVisibility(R.id.widget_dash_call_text_top, android.view.View.VISIBLE)
                    } else { // Standard 4x4 compact size
                        views.setViewVisibility(R.id.widget_dash_call_text_top, android.view.View.GONE)
                    }
                }
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
