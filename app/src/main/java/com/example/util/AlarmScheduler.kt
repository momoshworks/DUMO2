package com.example.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.model.DulmsTask
import com.example.receiver.AlarmNotificationReceiver

object AlarmScheduler {
    private const val TAG = "AlarmScheduler"

    fun scheduleTaskAlarms(context: Context, task: DulmsTask) {
        if (task.isCompleted) return

        val now = System.currentTimeMillis()
        val due = task.dueDateMillis

        // 24 hours before
        val time24h = due - (24 * 60 * 60 * 1000L)
        if (time24h > now) {
            scheduleSingleAlarm(context, task, time24h, "24h", 1)
        }

        // 2 hours before
        val time2h = due - (2 * 60 * 60 * 1000L)
        if (time2h > now) {
            scheduleSingleAlarm(context, task, time2h, "2h", 2)
        }
    }

    private fun scheduleSingleAlarm(
        context: Context,
        task: DulmsTask,
        triggerAtMillis: Long,
        stage: String,
        requestCodeSuffix: Int
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, AlarmNotificationReceiver::class.java).apply {
            putExtra(AlarmNotificationReceiver.EXTRA_TASK_ID, task.id)
            putExtra(AlarmNotificationReceiver.EXTRA_TASK_TITLE, task.title)
            putExtra(AlarmNotificationReceiver.EXTRA_COURSE_NAME, task.courseName)
            putExtra(AlarmNotificationReceiver.EXTRA_STAGE, stage)
        }

        val requestCode = (task.id.hashCode() * 10 + requestCodeSuffix) and 0x7FFFFFFF

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerAtMillis,
                            pendingIntent
                        )
                    } else {
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerAtMillis,
                            pendingIntent
                        )
                    }
                } else {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
            Log.d(TAG, "Scheduled $stage alarm for ${task.title} at $triggerAtMillis")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule alarm for ${task.title}", e)
        }
    }
}
