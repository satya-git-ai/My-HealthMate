package com.example.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.local.entity.Medicine

class MedicineNotificationManager(private val context: Context) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
    val alarmPlayer = AlarmSoundPlayer(context)

    companion object {
        const val CHANNEL_MEDICINE = "channel_medicine_reminders"
        const val ACTION_MED_TAKEN = "com.example.ACTION_MED_TAKEN"
        const val ACTION_MED_SNOOZE = "com.example.ACTION_MED_SNOOZE"
        const val ACTION_MED_SKIP = "com.example.ACTION_MED_SKIP"

        const val EXTRA_MEDICINE_ID = "extra_medicine_id"
        const val EXTRA_MEDICINE_NAME = "extra_medicine_name"
        const val EXTRA_MEDICINE_DOSAGE = "extra_medicine_dosage"
        const val EXTRA_MEDICINE_TYPE = "extra_medicine_type"
        const val EXTRA_SCHEDULED_TIME = "extra_scheduled_time"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"

        fun getNotificationId(medicineId: Long): Int = 30000 + (medicineId % 10000).toInt()
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && notificationManager != null) {
            val medChannel = NotificationChannel(
                CHANNEL_MEDICINE,
                "Medicine Reminders & Alarms",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Critical scheduled medicine alarms with actions to take, snooze, or skip"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 400, 200, 400, 200, 800)
                setShowBadge(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(medChannel)
        }
    }

    fun showReminderNotification(medicine: Medicine, scheduledTimeString: String) {
        val notificationId = getNotificationId(medicine.id)

        // Launch app when notification body tapped
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("route", "med_reminder")
            putExtra(EXTRA_MEDICINE_ID, medicine.id)
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // "Taken" Action
        val takenIntent = Intent(context, MedicineReminderReceiver::class.java).apply {
            action = ACTION_MED_TAKEN
            putExtra(EXTRA_MEDICINE_ID, medicine.id)
            putExtra(EXTRA_MEDICINE_NAME, medicine.name)
            putExtra(EXTRA_MEDICINE_DOSAGE, medicine.dosage)
            putExtra(EXTRA_MEDICINE_TYPE, medicine.type)
            putExtra(EXTRA_SCHEDULED_TIME, scheduledTimeString)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }
        val takenPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 10 + 1,
            takenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // "Snooze 10 min" Action
        val snoozeIntent = Intent(context, MedicineReminderReceiver::class.java).apply {
            action = ACTION_MED_SNOOZE
            putExtra(EXTRA_MEDICINE_ID, medicine.id)
            putExtra(EXTRA_MEDICINE_NAME, medicine.name)
            putExtra(EXTRA_MEDICINE_DOSAGE, medicine.dosage)
            putExtra(EXTRA_MEDICINE_TYPE, medicine.type)
            putExtra(EXTRA_SCHEDULED_TIME, scheduledTimeString)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 10 + 2,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // "Skip" Action
        val skipIntent = Intent(context, MedicineReminderReceiver::class.java).apply {
            action = ACTION_MED_SKIP
            putExtra(EXTRA_MEDICINE_ID, medicine.id)
            putExtra(EXTRA_MEDICINE_NAME, medicine.name)
            putExtra(EXTRA_MEDICINE_DOSAGE, medicine.dosage)
            putExtra(EXTRA_MEDICINE_TYPE, medicine.type)
            putExtra(EXTRA_SCHEDULED_TIME, scheduledTimeString)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }
        val skipPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 10 + 3,
            skipIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dosageText = if (medicine.dosage.isNotBlank()) " • ${medicine.dosage}" else ""
        val subtitle = "${medicine.type}$dosageText"

        val builder = NotificationCompat.Builder(context, CHANNEL_MEDICINE)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Time to take your medicine \uD83D\uDC8A")
            .setContentText("${medicine.name} ($subtitle)")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("It is scheduled time for ${medicine.name} ($subtitle). Please take with water as advised.")
            )
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(openAppPendingIntent)
            .setAutoCancel(true)
            .setOngoing(false)
            .addAction(android.R.drawable.checkbox_on_background, "Taken \u2705", takenPendingIntent)
            .addAction(android.R.drawable.ic_popup_sync, "Snooze 10m \u23F0", snoozePendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Skip \u274C", skipPendingIntent)

        if (medicine.vibrate) {
            builder.setVibrate(longArrayOf(0, 400, 200, 400, 200, 800))
        }

        notificationManager?.notify(notificationId, builder.build())

        // Play alarm sound and vibration if enabled
        if (medicine.soundEnabled) {
            alarmPlayer.playContinuous(medicine.soundType, maxDurationSeconds = 45)
        }
        if (medicine.vibrate) {
            alarmPlayer.vibratePattern(longArrayOf(0, 400, 200, 400, 200, 600))
        }
    }

    fun dismissReminder(notificationId: Int) {
        notificationManager?.cancel(notificationId)
        AlarmSoundPlayer.stopActiveSound()
    }
}
