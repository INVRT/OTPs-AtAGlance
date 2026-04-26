package com.example.cabglance2

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews

class PocketWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_TOGGLE_SIGNIN) {
            val isSignIn = getTabState(context)
            setTabState(context, !isSignIn)
            WidgetUpdateHelper.triggerWidgetUpdate(context, PocketWidgetProvider::class.java)
        }
    }

    companion object {
        const val ACTION_TOGGLE_SIGNIN = "com.example.cabglance2.ACTION_TOGGLE_SIGNIN"
        
        private const val PREFS = "PocketWidgetPrefs"
        private const val KEY_IS_SIGNIN = "is_signin_active"

        fun setTabState(context: Context, isSignIn: Boolean) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_IS_SIGNIN, isSignIn).apply()
        }

        fun getTabState(context: Context): Boolean {
            return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_IS_SIGNIN, true)
        }

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_pocket_2x1)
            val rideInfo = RideDataStore.getRideInfo(context)
            val isSignIn = getTabState(context)

            // Setup single Toggle Intent on the Root and OTP text
            val toggleIntent = Intent(context, PocketWidgetProvider::class.java).apply { action = ACTION_TOGGLE_SIGNIN }
            val togglePending = PendingIntent.getBroadcast(context, 1, toggleIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            // Prevent app opening
            views.setOnClickPendingIntent(R.id.widget_root, togglePending)
            views.setOnClickPendingIntent(R.id.widget_pocket_otp, togglePending)
            views.setOnClickPendingIntent(R.id.widget_pocket_idle_text, togglePending)

            if (rideInfo == null) {
                views.setTextViewText(R.id.widget_pocket_header, "No Ride")
                views.setTextViewText(R.id.widget_pocket_in_out, "IN")
                views.setTextViewText(R.id.widget_pocket_otp, "---")
                views.setInt(R.id.widget_pocket_dot, "setColorFilter", Color.GRAY)
            } else {
                val stateText = if (isSignIn) "IN" else "OUT"
                views.setTextViewText(R.id.widget_pocket_in_out, stateText)
                
                var headerText = ""
                var dotColor = Color.parseColor("#4CAF50") // Green for Login
                
                if (rideInfo.type == NotificationType.LOGIN) {
                    headerText = "${rideInfo.etp ?: "--:--"}"
                } else if (rideInfo.type == NotificationType.LOGOUT) {
                    headerText = "${rideInfo.routeNo ?: "R-??"}"
                    dotColor = Color.parseColor("#F44336") // Red for Logout
                } else if (rideInfo.type == NotificationType.APPROACHING) {
                    val etpText = rideInfo.etp
                    headerText = if (etpText != null) "NEAR • $etpText" else "NEAR"
                    dotColor = Color.parseColor("#FFC107") // Yellow for Near
                } else if (rideInfo.type == NotificationType.IDLE) {
                    headerText = "IDLE"
                    dotColor = Color.parseColor("#D3D3D3") // Light grey
                }
                
                if (headerText.isBlank() && rideInfo.cabNo != null) {
                    headerText = "${rideInfo.cabNo}"
                }
                
                views.setTextViewText(R.id.widget_pocket_header, headerText)
                views.setInt(R.id.widget_pocket_dot, "setColorFilter", dotColor)

                if (rideInfo.type == NotificationType.IDLE) {
                    views.setViewVisibility(R.id.widget_pocket_otp, android.view.View.GONE)
                    views.setViewVisibility(R.id.widget_pocket_in_out, android.view.View.GONE)
                    views.setViewVisibility(R.id.widget_pocket_idle_text, android.view.View.VISIBLE)
                    
                    val idleColor = if (isSignIn) Color.parseColor("#A0B0C0") else Color.parseColor("#FDD835")
                    views.setTextColor(R.id.widget_pocket_idle_text, idleColor)
                } else {
                    views.setViewVisibility(R.id.widget_pocket_otp, android.view.View.VISIBLE)
                    views.setViewVisibility(R.id.widget_pocket_in_out, android.view.View.VISIBLE)
                    views.setViewVisibility(R.id.widget_pocket_idle_text, android.view.View.GONE)
                    val activeOtp = if (isSignIn) rideInfo.signInOtp else rideInfo.signOutOtp
                    views.setTextViewText(R.id.widget_pocket_otp, activeOtp ?: "----")
                }
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
