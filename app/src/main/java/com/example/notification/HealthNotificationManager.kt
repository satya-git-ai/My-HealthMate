package com.example.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity

class HealthNotificationManager(private val context: Context) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
    val alarmPlayer = AlarmSoundPlayer(context)

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && notificationManager != null) {
            val waterChannel = NotificationChannel(
                CHANNEL_WATER,
                "Hydration Reminders & Alarm",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Timely reminders to drink water and alarm alerts"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 150, 250)
            }

            val moveChannel = NotificationChannel(
                CHANNEL_MOVE,
                "Movement & Inactivity",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Gentle nudges to stand up and walk after prolonged sitting"
            }

            val goalChannel = NotificationChannel(
                CHANNEL_GOAL,
                "Daily Goal Milestones",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Celebrations and progress updates on step and water goals"
            }

            val workoutChannel = NotificationChannel(
                CHANNEL_WORKOUT,
                "Workout Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily scheduled activity and outdoor walk reminders"
            }

            notificationManager.createNotificationChannels(
                listOf(waterChannel, moveChannel, goalChannel, workoutChannel)
            )
        }
    }

    fun sendWaterReminder(
        customMessage: String? = null,
        playAlarmSound: Boolean = true,
        soundType: String = "ALARM_RING",
        vibrate: Boolean = true
    ) {
        val message = customMessage ?: "Drink a glass of water (250 ml) to stay energized and keep your body performing at its best."
        sendNotification(
            channelId = CHANNEL_WATER,
            notificationId = 1001,
            title = "Time to Hydrate! \uD83D\uDCA7",
            message = message
        )

        if (playAlarmSound) {
            alarmPlayer.play(soundType)
        }
        if (vibrate) {
            alarmPlayer.vibrate(600L)
        }
    }

    fun sendMovementReminder() {
        sendNotification(
            channelId = CHANNEL_MOVE,
            notificationId = 1002,
            title = "Stand Up & Move! \uD83C\uDFC3",
            message = "You've been still for a while. Take a 2-minute stroll around to improve circulation and burn calories."
        )
    }

    fun sendGoalMilestone(steps: Int, goal: Int) {
        val percent = (steps.toFloat() / goal * 100).toInt()
        sendNotification(
            channelId = CHANNEL_GOAL,
            notificationId = 1003,
            title = "\uD83C\uDF89 Goal Milestone Reached!",
            message = "You've reached $steps of your $goal steps today ($percent%). Keep up the amazing momentum!"
        )
    }

    fun sendWorkoutReminder() {
        sendNotification(
            channelId = CHANNEL_WORKOUT,
            notificationId = 1004,
            title = "Ready for a Workout? \uD83C\uDFDE\uFE0F",
            message = "A brisk 20-minute outdoor walk will help clear your mind and conquer your fitness goals."
        )
    }

    private fun sendNotification(
        channelId: String,
        notificationId: Int,
        title: String,
        message: String
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (e: SecurityException) {
            // Permission not granted on Android 13+
        }
    }

    companion object {
        const val CHANNEL_WATER = "healthfit_water"
        const val CHANNEL_MOVE = "healthfit_move"
        const val CHANNEL_GOAL = "healthfit_goal"
        const val CHANNEL_WORKOUT = "healthfit_workout"
    }
}
