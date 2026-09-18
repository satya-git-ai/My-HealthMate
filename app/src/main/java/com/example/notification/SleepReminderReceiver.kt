package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import android.widget.Toast
import com.example.data.preferences.UserPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ActiveSleepAlert(
    val isWakeup: Boolean,
    val title: String,
    val message: String
)

class SleepReminderReceiver : BroadcastReceiver() {

    private val receiverScope = CoroutineScope(Dispatchers.IO)

    companion object {
        private val _activeAlert = MutableStateFlow<ActiveSleepAlert?>(null)
        val activeAlert = _activeAlert.asStateFlow()

        fun clearActiveAlert() {
            _activeAlert.value = null
            AlarmSoundPlayer.stopActiveSound()
        }
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return
        val action = intent.action ?: return

        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val wakeLock = pm?.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "HealthFit:SleepReminderWakeLock"
        )
        wakeLock?.acquire(30000L) // 30s timeout

        val notificationManager = SleepNotificationManager(context)
        val scheduler = SleepReminderScheduler(context)
        val prefs = UserPreferences(context)
        val settings = prefs.settings.value

        val soundType = intent.getStringExtra("sound_type") ?: settings.sleepSoundType
        val volume = intent.getFloatExtra("volume", settings.sleepSoundVolume)
        val notificationId = intent.getIntExtra("notification_id", SleepNotificationManager.NOTIFICATION_ID_BEDTIME)

        when (action) {
            SleepReminderScheduler.ACTION_TRIGGER_BEDTIME -> {
                receiverScope.launch {
                    try {
                        if (settings.sleepReminderEnabled) {
                            notificationManager.showBedtimeNotification(soundType, volume)
                            _activeAlert.value = ActiveSleepAlert(
                                isWakeup = false,
                                title = "It's time to sleep 😴",
                                message = "Wind down and get a restful night's sleep."
                            )
                            // Reschedule tomorrow's bedtime
                            scheduler.scheduleBedtime(
                                settings.sleepBedtimeHour,
                                settings.sleepBedtimeMinute,
                                soundType,
                                volume
                            )
                        }
                    } catch (e: Exception) {
                        Log.e("SleepReceiver", "Error triggering bedtime", e)
                    } finally {
                        if (wakeLock?.isHeld == true) wakeLock.release()
                    }
                }
            }

            SleepReminderScheduler.ACTION_TRIGGER_WAKEUP -> {
                receiverScope.launch {
                    try {
                        val isSnooze = intent.getBooleanExtra("is_snooze", false)
                        if (settings.sleepReminderEnabled || isSnooze) {
                            notificationManager.showWakeupNotification(soundType, volume)
                            _activeAlert.value = ActiveSleepAlert(
                                isWakeup = true,
                                title = "Good morning! ☀️ Time to wake up.",
                                message = "Start your day refreshed with My HealthMate."
                            )
                            // If this was regular wake-up (not snooze), reschedule tomorrow's wake-up
                            if (!isSnooze && settings.sleepReminderEnabled) {
                                scheduler.scheduleWakeup(
                                    settings.sleepWakeupHour,
                                    settings.sleepWakeupMinute,
                                    soundType,
                                    volume
                                )
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("SleepReceiver", "Error triggering wake-up", e)
                    } finally {
                        if (wakeLock?.isHeld == true) wakeLock.release()
                    }
                }
            }

            SleepNotificationManager.ACTION_SLEEP_DISMISS -> {
                notificationManager.dismissReminder(notificationId)
                _activeAlert.value = null
                if (wakeLock?.isHeld == true) wakeLock.release()
            }

            SleepNotificationManager.ACTION_SLEEP_SNOOZE -> {
                notificationManager.dismissReminder(notificationId)
                _activeAlert.value = null
                scheduler.scheduleSnooze(10, soundType, volume)
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(context, "Alarm snoozed for 10 minutes ⏰", Toast.LENGTH_SHORT).show()
                }
                if (wakeLock?.isHeld == true) wakeLock.release()
            }
        }
    }
}
