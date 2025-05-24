package com.example.finalproject7

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import android.os.Build

// This BroadcastReceiver gets triggered when a scheduled broadcast is received
class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Gets notification manager to show the notifications
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Takes the data received from the intent
        val taskTitle = intent.getStringExtra("taskTitle") ?: "Task Reminder"
        val taskType = intent.getStringExtra("taskType") ?: "Task" // Default to "Task" if not provided
        val taskId = intent.getIntExtra("notificationId", 0)

        // Creates an intent to open app when notification is clicked
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Determine notification title based on task type
        val notificationTitle = when (taskType) {
            "Assignment" -> "Upcoming Assignment"
            "Task" -> "Upcoming Task"
            else -> "Upcoming Reminder" // Fallback for unexpected types
        }

        // Build notification using notificationCompat
        val notification = NotificationCompat.Builder(context, "task_channel")
            .setSmallIcon(R.drawable.ic_notification) // Use your custom icon
            .setContentTitle(notificationTitle) // Dynamic title based on task type
            .setContentText("$taskTitle is due in 30 minutes!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(taskId, notification)
    }

    companion object {
        //Creates a notification channel which is necessary for the version of android we are using
        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    "task_channel",
                    "Task Reminders",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Channel for task reminders"
                }

                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)
            }
        }
    }
}