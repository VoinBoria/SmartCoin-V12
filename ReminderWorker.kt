package com.serhio.homeaccountingapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters

class ReminderWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    override fun doWork(): Result {
        val taskTitle = inputData.getString("TASK_TITLE")
        val reminderTime = inputData.getString("REMINDER_TIME")

        showNotification(taskTitle, reminderTime)

        return Result.success()
    }

    private fun showNotification(taskTitle: String?, reminderTime: String?) {
        val channelId = "task_reminder_channel"
        val channelName = "Task Reminder"
        val importance = NotificationManager.IMPORTANCE_HIGH

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = "Канал для нагадувань про задачі"
            }
            val notificationManager: NotificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(applicationContext.getString(R.string.reminder))
            .setContentText(applicationContext.getString(R.string.task_reminder_message, taskTitle, reminderTime))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        try {
            with(NotificationManagerCompat.from(applicationContext)) {
                notify(System.currentTimeMillis().toInt() and 0xFFFFFFF, builder.build())
            }
        } catch (e: SecurityException) {
            // Handle the SecurityException if permission is denied
            e.printStackTrace()
        }
    }
}