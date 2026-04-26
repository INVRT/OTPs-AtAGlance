package com.example.cabglance2

import android.content.Context
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import java.util.Calendar
import java.util.concurrent.TimeUnit

object ScheduleManager {
    private const val PREF_NAME = "SchedulePrefs"

    fun setMorningTime(context: Context, hour: Int, min: Int) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit()
            .putInt("m_hour", hour).putInt("m_min", min).apply()
        setupReminders(context)
    }

    fun setEveningTime(context: Context, hour: Int, min: Int) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit()
            .putInt("e_hour", hour).putInt("e_min", min).apply()
        setupReminders(context)
    }

    fun getMorningTime(context: Context): Pair<Int, Int> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return Pair(prefs.getInt("m_hour", 21), prefs.getInt("m_min", 0))
    }

    fun getEveningTime(context: Context): Pair<Int, Int> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return Pair(prefs.getInt("e_hour", 1), prefs.getInt("e_min", 0))
    }

    fun setupReminders(context: Context) {
        val workManager = WorkManager.getInstance(context)
        
        if (!SettingsManager.isRemindersEnabled(context)) {
            workManager.cancelUniqueWork("MorningReminder")
            workManager.cancelUniqueWork("EveningReminder")
            return
        }

        val mTime = getMorningTime(context)
        val eTime = getEveningTime(context)

        // Setup Morning Reminder
        val morningDelay = calculateDelayMs(mTime.first, mTime.second)
        val morningData = Data.Builder().putString("shift", "Morning").build()
        val morningWork = androidx.work.OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(morningDelay, TimeUnit.MILLISECONDS)
            .setInputData(morningData)
            .build()
        
        workManager.enqueueUniqueWork(
            "MorningReminder",
            androidx.work.ExistingWorkPolicy.REPLACE,
            morningWork
        )

        // Setup Evening Reminder
        val eveningDelay = calculateDelayMs(eTime.first, eTime.second)
        val eveningData = Data.Builder().putString("shift", "Evening").build()
        val eveningWork = androidx.work.OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(eveningDelay, TimeUnit.MILLISECONDS)
            .setInputData(eveningData)
            .build()
        
        workManager.enqueueUniqueWork(
            "EveningReminder",
            androidx.work.ExistingWorkPolicy.REPLACE,
            eveningWork
        )
    }

    private fun calculateDelayMs(targetHour: Int, targetMin: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, targetHour)
            set(Calendar.MINUTE, targetMin)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // If target is before or perfectly equal to now, push to tomorrow to avoid 0-delay loop
        if (target.timeInMillis <= now.timeInMillis) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }

        return target.timeInMillis - now.timeInMillis
    }
}
