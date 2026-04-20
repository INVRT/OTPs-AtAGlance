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
        return Pair(prefs.getInt("m_hour", 7), prefs.getInt("m_min", 30))
    }

    fun getEveningTime(context: Context): Pair<Int, Int> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return Pair(prefs.getInt("e_hour", 14), prefs.getInt("e_min", 0))
    }

    fun setupReminders(context: Context) {
        val workManager = WorkManager.getInstance(context)
        val mTime = getMorningTime(context)
        val eTime = getEveningTime(context)

        // Setup Morning Reminder
        val morningDelay = calculateDelayMinutes(mTime.first, mTime.second)
        val morningData = Data.Builder().putString("shift", "Morning").build()
        val morningWork = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(morningDelay, TimeUnit.MINUTES)
            .setInputData(morningData)
            .build()
        
        workManager.enqueueUniquePeriodicWork(
            "MorningReminder",
            ExistingPeriodicWorkPolicy.UPDATE,
            morningWork
        )

        // Setup Evening Reminder
        val eveningDelay = calculateDelayMinutes(eTime.first, eTime.second)
        val eveningData = Data.Builder().putString("shift", "Evening").build()
        val eveningWork = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(eveningDelay, TimeUnit.MINUTES)
            .setInputData(eveningData)
            .build()
        
        workManager.enqueueUniquePeriodicWork(
            "EveningReminder",
            ExistingPeriodicWorkPolicy.UPDATE,
            eveningWork
        )
    }

    private fun calculateDelayMinutes(targetHour: Int, targetMin: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, targetHour)
            set(Calendar.MINUTE, targetMin)
            set(Calendar.SECOND, 0)
        }

        if (target.before(now)) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }

        val diffMs = target.timeInMillis - now.timeInMillis
        return TimeUnit.MILLISECONDS.toMinutes(diffMs)
    }
}
