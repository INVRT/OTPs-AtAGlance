package com.example.cabglance2

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews

/**
 * Implementation of App Widget functionality.
 */
class MoveInSyncAppWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        // Enter relevant functionality for when the first widget is created
    }

    override fun onDisabled(context: Context) {
        // Enter relevant functionality for when the last widget is disabled
    }
    
    companion object {
        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val rideInfo = RideDataStore.getRideInfo(context)

            val views = RemoteViews(context.packageName, R.layout.moveinsync_widget_layout)

            if (rideInfo == null) {
                views.setTextViewText(R.id.widget_title, "No Ride Info")
                views.setTextViewText(R.id.widget_otp, "---")
            } else {
                if (rideInfo.type == NotificationType.IDLE) {
                    views.setViewVisibility(R.id.widget_title, android.view.View.GONE)
                    views.setViewVisibility(R.id.widget_otp, android.view.View.GONE)
                    views.setViewVisibility(R.id.widget_subtitle, android.view.View.GONE)
                    views.setViewVisibility(R.id.widget_idle_text, android.view.View.VISIBLE)
                } else {
                    views.setViewVisibility(R.id.widget_title, android.view.View.VISIBLE)
                    views.setViewVisibility(R.id.widget_otp, android.view.View.VISIBLE)
                    views.setViewVisibility(R.id.widget_subtitle, android.view.View.VISIBLE)
                    views.setViewVisibility(R.id.widget_idle_text, android.view.View.GONE)

                    if (rideInfo.type == NotificationType.LOGIN) {
                        views.setTextViewText(R.id.widget_title, "SignIn/Out ETP: ${rideInfo.etp ?: "--:--"}")
                        views.setTextViewText(R.id.widget_otp, "${rideInfo.signInOtp ?: "----"} / ${rideInfo.signOutOtp ?: "----"}")
                        views.setTextViewText(R.id.widget_subtitle, "Cab: ${rideInfo.cabNo ?: "--"}")
                    } else if (rideInfo.type == NotificationType.LOGOUT) {
                        views.setTextViewText(R.id.widget_title, "Drop OTP [Route ${rideInfo.routeNo ?: "--"}]")
                        views.setTextViewText(R.id.widget_otp, "${rideInfo.signInOtp ?: "----"} / ${rideInfo.signOutOtp ?: "----"}")
                        views.setTextViewText(R.id.widget_subtitle, "")
                    } else if (rideInfo.type == NotificationType.APPROACHING) {
                        views.setTextViewText(R.id.widget_title, "🚐 CAB NEAR! ${rideInfo.cabNo ?: "--"}")
                        views.setTextViewText(R.id.widget_otp, "${rideInfo.signInOtp ?: "----"} / ${rideInfo.signOutOtp ?: "----"}")
                        views.setTextViewText(R.id.widget_subtitle, "")
                    }
                }
            }

            // Instruct the widget manager to update the widget
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}

