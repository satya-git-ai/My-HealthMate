package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.preferences.UserPreferences
import java.util.Calendar

class WaterReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == WaterReminderScheduler.ACTION_WATER_REMINDER) {
            val prefs = UserPreferences(context)
            val settings = prefs.settings.value

            if (!settings.waterReminderEnabled) return

            val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            val isWithinWindow = currentHour in settings.waterReminderStartHour until settings.waterReminderEndHour

            if (isWithinWindow) {
                val notificationManager = HealthNotificationManager(context)
                notificationManager.sendWaterReminder(
                    playAlarmSound = settings.waterAlarmSoundEnabled,
                    soundType = settings.waterAlarmSoundType,
                    vibrate = settings.waterVibrateEnabled
                )
            }

            // Reschedule next timely reminder
            val scheduler = WaterReminderScheduler(context, prefs)
            scheduler.scheduleNextReminder()
        }
    }
}
