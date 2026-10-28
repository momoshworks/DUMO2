package com.example.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity

class AlarmNotificationReceiver : BroadcastReceiver() {
    companion object {
        const val CHANNEL_ID = "dumu_academic_alerts"
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_TASK_TITLE = "extra_task_title"
        const val EXTRA_COURSE_NAME = "extra_course_name"
        const val EXTRA_STAGE = "extra_stage" // "24h" or "2h"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra(EXTRA_TASK_TITLE) ?: "Upcoming Deadline"
        val course = intent.getStringExtra(EXTRA_COURSE_NAME) ?: "Delta University"
        val stage = intent.getStringExtra(EXTRA_STAGE) ?: "soon"

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Academic Deadlines & Quizzes",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders 24 hours and 2 hours before assignments and quizzes"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val message = when (stage) {
            "24h" -> "Due tomorrow (24 hours remaining): $title in $course"
            "2h" -> "URGENT: Due in 2 hours! $title in $course"
            else -> "Due soon: $title ($course)"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("DUMU: $course")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationId = (title.hashCode() + stage.hashCode()) and 0x7FFFFFFF
        notificationManager.notify(notificationId, notification)
    }
}
