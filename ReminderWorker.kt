package com.serhio.homeaccountingapp

import android.content.Context
import android.content.Intent
import androidx.work.Worker
import androidx.work.WorkerParameters

class ReminderWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    override fun doWork(): Result {
        val taskTitle = inputData.getString("TASK_TITLE")
        val reminderTime = inputData.getString("REMINDER_TIME")

        val intent = Intent(applicationContext, ReminderBroadcastReceiver::class.java).apply {
            putExtra("TASK_TITLE", taskTitle)
            putExtra("REMINDER_TIME", reminderTime)
        }
        applicationContext.sendBroadcast(intent)

        return Result.success()
    }
}
