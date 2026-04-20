package com.example.cabglance2

import android.content.Context
import org.json.JSONArray
import org.json.JSONException

object RideDataStore {
    private const val PREF_NAME = "RideDataPrefs"

    fun saveRideInfo(context: Context, info: RideInfo) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        
        // Save history first
        val historyRaw = prefs.getString("history", "[]") ?: "[]"
        val historyArr = try { JSONArray(historyRaw) } catch(e: Exception) { JSONArray() }
        
        val timestamp = System.currentTimeMillis()
        val entry = "[$timestamp] ${info.type}: ${info.rawText}"
        historyArr.put(entry)
        
        // Keep only last 10
        val newArr = JSONArray()
        val startIdx = if (historyArr.length() > 10) historyArr.length() - 10 else 0
        for (i in startIdx until historyArr.length()) {
            newArr.put(historyArr.getString(i))
        }

        prefs.edit()
            .putString("history", newArr.toString())
            .putString("type", info.type.name)
            .putString("signInOtp", info.signInOtp)
            .putString("signOutOtp", info.signOutOtp)
            .putString("etp", info.etp)
            .putString("cabNo", info.cabNo)
            .putString("routeNo", info.routeNo)
            .putBoolean("isApproaching", info.isApproaching)
            .putString("rawText", info.rawText)
            .apply()
    }

    fun getHistory(context: Context): List<String> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val historyRaw = prefs.getString("history", "[]") ?: "[]"
        val list = mutableListOf<String>()
        try {
            val arr = JSONArray(historyRaw)
            for (i in 0 until arr.length()) {
                list.add(arr.getString(i))
            }
        } catch (_: JSONException) {}
        return list.reversed()
    }

    fun getRideInfo(context: Context): RideInfo? {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        if (!prefs.contains("type")) return null

        val typeStr = prefs.getString("type", NotificationType.UNKNOWN.name) ?: NotificationType.UNKNOWN.name
        val type = try { NotificationType.valueOf(typeStr) } catch (e: Exception) { NotificationType.UNKNOWN }

        return RideInfo(
            type = type,
            signInOtp = prefs.getString("signInOtp", null),
            signOutOtp = prefs.getString("signOutOtp", null),
            etp = prefs.getString("etp", null),
            cabNo = prefs.getString("cabNo", null),
            routeNo = prefs.getString("routeNo", null),
            isApproaching = prefs.getBoolean("isApproaching", false),
            rawText = prefs.getString("rawText", "") ?: ""
        )
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit().clear().apply()
    }
}
