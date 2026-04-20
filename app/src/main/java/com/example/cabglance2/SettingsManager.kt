package com.example.cabglance2

import android.content.Context
import android.content.SharedPreferences

enum class SourcingMode {
    APP_NOTIFICATION, SMS
}

object SettingsManager {
    private const val PREFS_NAME = "CabGlancePrefs"
    private const val KEY_SOURCING_MODE = "SourcingMode"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getSourcingMode(context: Context): SourcingMode {
        val modeStr = getPrefs(context).getString(KEY_SOURCING_MODE, SourcingMode.APP_NOTIFICATION.name)
        return try {
            SourcingMode.valueOf(modeStr ?: SourcingMode.APP_NOTIFICATION.name)
        } catch (e: Exception) {
            SourcingMode.APP_NOTIFICATION
        }
    }

    fun setSourcingMode(context: Context, mode: SourcingMode) {
        getPrefs(context).edit().putString(KEY_SOURCING_MODE, mode.name).apply()
    }

    fun isStickyNotificationEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean("StickyNotificationEnabled", true)
    }

    fun setStickyNotificationEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean("StickyNotificationEnabled", enabled).apply()
    }
}

