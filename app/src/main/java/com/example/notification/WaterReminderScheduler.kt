package com.example.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.preferences.UserPreferences
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class WaterReminderScheduler(
    private val context: Context,
    private val userPreferences: UserPreferences
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    fun scheduleNextReminder(delayMinutes: Int? = null) {
        val settings = userPreferences.settings.value
        if (!settings.waterReminderEnabled) {
            cancelReminders()
            return
        }

        val interval = delayMinutes ?: settings.waterReminderIntervalMinutes
        val nextTriggerTime = calculateNextTriggerTime(
            intervalMinutes = interval,
            startHour = settings.waterReminderStartHour,
            endHour = settings.waterReminderEndHour
        )

        val intent = Intent(context, WaterReminderReceiver::class.java).apply {
            action = ACTION_WATER_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_WATER,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager?.canScheduleExactAlarms() == true) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        nextTriggerTime,
                        pendingIntent
                    )
                } else {
                    alarmManager?.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        nextTriggerTime,
                        pendingIntent
                    )
                }
            } else {
                alarmManager?.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    nextTriggerTime,
                    pendingIntent
                )
            }
        } catch (e: Exception) {
            Log.e("WaterReminderScheduler", "Failed to schedule alarm", e)
        }
    }

    fun cancelReminders() {
        val intent = Intent(context, WaterReminderReceiver::class.java).apply {
            action = ACTION_WATER_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_WATER,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager?.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    fun getNextReminderTimeString(): String {
        val settings = userPreferences.settings.value
        if (!settings.waterReminderEnabled) {
            return "Reminders are paused"
        }
        val nextTrigger = calculateNextTriggerTime(
            settings.waterReminderIntervalMinutes,
            settings.waterReminderStartHour,
            settings.waterReminderEndHour
        )
        val format = SimpleDateFormat("h:mm a", Locale.getDefault())
        val calNow = Calendar.getInstance()
        val calNext = Calendar.getInstance().apply { timeInMillis = nextTrigger }

        return if (calNext.get(Calendar.DAY_OF_YEAR) != calNow.get(Calendar.DAY_OF_YEAR)) {
            "Tomorrow at " + format.format(Date(nextTrigger))
        } else {
            "Today at " + format.format(Date(nextTrigger))
        }
    }

    private fun calculateNextTriggerTime(intervalMinutes: Int, startHour: Int, endHour: Int): Long {
        val cal = Calendar.getInstance()
        val currentHour = cal.get(Calendar.HOUR_OF_DAY)

        if (currentHour < startHour) {
            // Before start hour: schedule for today's start hour
            cal.set(Calendar.HOUR_OF_DAY, startHour)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        } else if (currentHour >= endHour) {
            // After end hour: schedule for tomorrow's start hour
            cal.add(Calendar.DAY_OF_YEAR, 1)
            cal.set(Calendar.HOUR_OF_DAY, startHour)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        } else {
            // Within active hours: add intervalMinutes
            cal.add(Calendar.MINUTE, intervalMinutes)
            if (cal.get(Calendar.HOUR_OF_DAY) >= endHour) {
                // If it pushes past end hour, schedule for tomorrow morning
                cal.add(Calendar.DAY_OF_YEAR, 1)
                cal.set(Calendar.HOUR_OF_DAY, startHour)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
            }
            return cal.timeInMillis
        }
    }

    companion object {
        const val ACTION_WATER_REMINDER = "com.example.ACTION_WATER_REMINDER"
        const val REQUEST_CODE_WATER = 4001
    }
}
