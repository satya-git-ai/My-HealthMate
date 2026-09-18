package com.example.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.preferences.UserSettings
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class SleepReminderScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    companion object {
        const val ACTION_TRIGGER_BEDTIME = "com.example.ACTION_TRIGGER_BEDTIME"
        const val ACTION_TRIGGER_WAKEUP = "com.example.ACTION_TRIGGER_WAKEUP"

        const val REQUEST_CODE_BEDTIME = 40001
        const val REQUEST_CODE_WAKEUP = 40002
        const val REQUEST_CODE_SNOOZE = 40003

        fun formatTime12Hour(hour: Int, minute: Int): String {
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
            }
            val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
            return sdf.format(cal.time)
        }

        fun calculateNextOccurrence(hour: Int, minute: Int): Long {
            val now = System.currentTimeMillis()
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (cal.timeInMillis <= now) {
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }
            return cal.timeInMillis
        }

        fun getNextReminderDescription(settings: UserSettings): String {
            if (!settings.sleepReminderEnabled) return "Reminder is currently turned off"

            val bedtimeNext = calculateNextOccurrence(settings.sleepBedtimeHour, settings.sleepBedtimeMinute)
            val wakeupNext = calculateNextOccurrence(settings.sleepWakeupHour, settings.sleepWakeupMinute)

            val now = System.currentTimeMillis()
            val calNow = Calendar.getInstance()
            val calBed = Calendar.getInstance().apply { timeInMillis = bedtimeNext }
            val calWake = Calendar.getInstance().apply { timeInMillis = wakeupNext }

            val bedtimeStr = formatTime12Hour(settings.sleepBedtimeHour, settings.sleepBedtimeMinute)
            val wakeupStr = formatTime12Hour(settings.sleepWakeupHour, settings.sleepWakeupMinute)

            return if (bedtimeNext < wakeupNext) {
                val isToday = calBed.get(Calendar.DAY_OF_YEAR) == calNow.get(Calendar.DAY_OF_YEAR)
                if (isToday) "Bedtime tonight at $bedtimeStr" else "Bedtime tomorrow at $bedtimeStr"
            } else {
                val isToday = calWake.get(Calendar.DAY_OF_YEAR) == calNow.get(Calendar.DAY_OF_YEAR)
                if (isToday) "Wake-up today at $wakeupStr" else "Wake-up tomorrow at $wakeupStr"
            }
        }
    }

    fun scheduleSleepReminders(settings: UserSettings) {
        if (!settings.sleepReminderEnabled) {
            cancelSleepReminders()
            return
        }

        scheduleBedtime(settings.sleepBedtimeHour, settings.sleepBedtimeMinute, settings.sleepSoundType, settings.sleepSoundVolume)
        scheduleWakeup(settings.sleepWakeupHour, settings.sleepWakeupMinute, settings.sleepSoundType, settings.sleepSoundVolume)
    }

    fun scheduleBedtime(hour: Int, minute: Int, soundType: String, volume: Float) {
        if (alarmManager == null) return

        val triggerTime = calculateNextOccurrence(hour, minute)
        val intent = Intent(context, SleepReminderReceiver::class.java).apply {
            action = ACTION_TRIGGER_BEDTIME
            putExtra("sound_type", soundType)
            putExtra("volume", volume)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_BEDTIME,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
            Log.d("SleepScheduler", "Bedtime scheduled for timestamp $triggerTime (${formatTime12Hour(hour, minute)})")
        } catch (e: SecurityException) {
            Log.w("SleepScheduler", "Cannot schedule exact bedtime alarm, falling back", e)
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        } catch (e: Exception) {
            Log.e("SleepScheduler", "Error scheduling bedtime alarm", e)
        }
    }

    fun scheduleWakeup(hour: Int, minute: Int, soundType: String, volume: Float) {
        if (alarmManager == null) return

        val triggerTime = calculateNextOccurrence(hour, minute)
        val intent = Intent(context, SleepReminderReceiver::class.java).apply {
            action = ACTION_TRIGGER_WAKEUP
            putExtra("sound_type", soundType)
            putExtra("volume", volume)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_WAKEUP,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val showAppIntent = Intent(context, com.example.MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val showAppPending = PendingIntent.getActivity(
                    context,
                    REQUEST_CODE_WAKEUP + 100,
                    showAppIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerTime, showAppPending)
                alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
            Log.d("SleepScheduler", "Wake-up alarm scheduled for timestamp $triggerTime (${formatTime12Hour(hour, minute)})")
        } catch (e: SecurityException) {
            Log.w("SleepScheduler", "Cannot schedule exact wake-up alarm, falling back", e)
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        } catch (e: Exception) {
            Log.e("SleepScheduler", "Error scheduling wake-up alarm", e)
        }
    }

    fun scheduleSnooze(minutes: Int = 10, soundType: String, volume: Float) {
        if (alarmManager == null) return

        val triggerTime = System.currentTimeMillis() + (minutes * 60 * 1000L)
        val intent = Intent(context, SleepReminderReceiver::class.java).apply {
            action = ACTION_TRIGGER_WAKEUP
            putExtra("sound_type", soundType)
            putExtra("volume", volume)
            putExtra("is_snooze", true)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_SNOOZE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
            Log.d("SleepScheduler", "Snoozed wake-up alarm for $minutes minutes ($triggerTime)")
        } catch (e: Exception) {
            Log.e("SleepScheduler", "Error scheduling snooze alarm", e)
        }
    }

    fun cancelSleepReminders() {
        if (alarmManager == null) return

        // Cancel Bedtime
        val bedtimeIntent = Intent(context, SleepReminderReceiver::class.java).apply {
            action = ACTION_TRIGGER_BEDTIME
        }
        val bedtimePending = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_BEDTIME,
            bedtimeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(bedtimePending)

        // Cancel Wakeup
        val wakeupIntent = Intent(context, SleepReminderReceiver::class.java).apply {
            action = ACTION_TRIGGER_WAKEUP
        }
        val wakeupPending = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_WAKEUP,
            wakeupIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(wakeupPending)

        // Cancel Snooze
        val snoozeIntent = Intent(context, SleepReminderReceiver::class.java).apply {
            action = ACTION_TRIGGER_WAKEUP
        }
        val snoozePending = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_SNOOZE,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(snoozePending)

        Log.d("SleepScheduler", "All sleep reminders cancelled")
    }
}
