package com.example.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity

class SleepNotificationManager(private val context: Context) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
    val alarmPlayer = AlarmSoundPlayer(context)

    companion object {
        const val CHANNEL_SLEEP = "channel_sleep_reminders"
        const val ACTION_SLEEP_DISMISS = "com.example.ACTION_SLEEP_DISMISS"
        const val ACTION_SLEEP_SNOOZE = "com.example.ACTION_SLEEP_SNOOZE"

        const val NOTIFICATION_ID_BEDTIME = 40001
        const val NOTIFICATION_ID_WAKEUP = 40002
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && notificationManager != null) {
            val channel = NotificationChannel(
                CHANNEL_SLEEP,
                "Sleep & Wake-up Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Gentle bedtime reminders and morning wake-up alarms"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 250, 500, 250, 500)
                setShowBadge(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showBedtimeNotification(soundType: String, volume: Float) {
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("route", "home")
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_BEDTIME,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Dismiss Action
        val dismissIntent = Intent(context, SleepReminderReceiver::class.java).apply {
            action = ACTION_SLEEP_DISMISS
            putExtra("notification_id", NOTIFICATION_ID_BEDTIME)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            NOTIFICATION_ID_BEDTIME * 10 + 1,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_SLEEP)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("It's time to sleep 😴")
            .setContentText("Wind down and get a restful night's sleep.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(openAppPendingIntent)
            .setAutoCancel(true)
            .setOngoing(false)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Dismiss", dismissPendingIntent)
            .build()

        notificationManager?.notify(NOTIFICATION_ID_BEDTIME, notification)

        // Play selected sound continuous or once at volume
        alarmPlayer.play(soundType, volume)
        alarmPlayer.vibratePattern(longArrayOf(0, 400, 200, 400))
    }

    fun showWakeupNotification(soundType: String, volume: Float) {
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("route", "home")
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_WAKEUP,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Snooze Action (10 min)
        val snoozeIntent = Intent(context, SleepReminderReceiver::class.java).apply {
            action = ACTION_SLEEP_SNOOZE
            putExtra("notification_id", NOTIFICATION_ID_WAKEUP)
            putExtra("sound_type", soundType)
            putExtra("volume", volume)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            NOTIFICATION_ID_WAKEUP * 10 + 1,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Dismiss Action
        val dismissIntent = Intent(context, SleepReminderReceiver::class.java).apply {
            action = ACTION_SLEEP_DISMISS
            putExtra("notification_id", NOTIFICATION_ID_WAKEUP)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            NOTIFICATION_ID_WAKEUP * 10 + 2,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_SLEEP)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Good morning! ☀️ Time to wake up.")
            .setContentText("Start your day refreshed with My HealthMate.")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(openAppPendingIntent)
            .setAutoCancel(true)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_popup_sync, "Snooze", snoozePendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Dismiss", dismissPendingIntent)
            .build()

        notificationManager?.notify(NOTIFICATION_ID_WAKEUP, notification)

        // Play alarm sound continuously for up to 45 seconds or until dismissed
        alarmPlayer.playContinuous(soundType, volume, maxDurationSeconds = 45)
        alarmPlayer.vibratePattern(longArrayOf(0, 500, 300, 500, 300, 800))
    }

    fun dismissReminder(notificationId: Int) {
        notificationManager?.cancel(notificationId)
        alarmPlayer.stop()
        AlarmSoundPlayer.stopActiveSound()
    }
}
