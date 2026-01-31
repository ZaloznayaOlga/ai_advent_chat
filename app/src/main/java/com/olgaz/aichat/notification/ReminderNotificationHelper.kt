package com.olgaz.aichat.notification

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.olgaz.aichat.MainActivity
import com.olgaz.aichat.R
import com.olgaz.aichat.mcptools.reminder.ReminderEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderNotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        const val CHANNEL_REMINDER_ALERTS = "reminder_alerts"
        const val CHANNEL_REMINDER_SUMMARY = "reminder_summary"
        const val CHANNEL_FOREGROUND_SERVICE = "reminder_foreground_service"

        const val NOTIFICATION_ID_FOREGROUND = 1001
        const val NOTIFICATION_ID_SUMMARY = 1002
        const val NOTIFICATION_ID_URGENT_BASE = 2000

        const val EXTRA_FROM_REMINDER_SUMMARY = "extra_from_reminder_summary"
    }

    fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)

            val alertChannel = NotificationChannel(
                CHANNEL_REMINDER_ALERTS,
                "Срочные напоминания",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Уведомления о напоминаниях, которые скоро наступят"
                enableVibration(true)
                enableLights(true)
            }

            val summaryChannel = NotificationChannel(
                CHANNEL_REMINDER_SUMMARY,
                "Сводка напоминаний",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Периодическая сводка активных напоминаний"
            }

            val serviceChannel = NotificationChannel(
                CHANNEL_FOREGROUND_SERVICE,
                "Фоновый сервис напоминаний",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Постоянное уведомление при наличии активных напоминаний"
                setShowBadge(false)
            }

            manager.createNotificationChannels(
                listOf(alertChannel, summaryChannel, serviceChannel)
            )
        }
    }

    fun buildForegroundServiceNotification(activeCount: Int): Notification {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_FOREGROUND_SERVICE)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("AIChat — Напоминания")
            .setContentText("Активных напоминаний: $activeCount")
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    fun showSummaryNotification(summaryText: String, activeCount: Int) {
        if (!hasNotificationPermission()) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_FROM_REMINDER_SUMMARY, true)
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 1, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDER_SUMMARY)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Сводка: $activeCount задач")
            .setStyle(NotificationCompat.BigTextStyle().bigText(summaryText))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_SUMMARY, notification)
    }

    fun showUrgentReminderNotification(reminder: ReminderEntity) {
        if (!hasNotificationPermission()) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, (NOTIFICATION_ID_URGENT_BASE + reminder.id).toInt(), intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val timeText = formatTimeRemaining(reminder.reminderTime)

        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDER_ALERTS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Скоро: ${reminder.text}")
            .setContentText(timeText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationId = (NOTIFICATION_ID_URGENT_BASE + reminder.id).toInt()
        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }

    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun formatTimeRemaining(reminderTime: Long?): String {
        if (reminderTime == null) return ""

        val now = System.currentTimeMillis()
        val diffMs = reminderTime - now

        if (diffMs <= 0) {
            val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            return "Время наступило: ${dateFormat.format(Date(reminderTime))}"
        }

        val diffMinutes = diffMs / (60 * 1000)
        return when {
            diffMinutes < 1 -> "Менее минуты"
            diffMinutes < 60 -> "Через $diffMinutes мин."
            else -> {
                val hours = diffMinutes / 60
                val mins = diffMinutes % 60
                if (mins > 0) "Через ${hours}ч ${mins}мин."
                else "Через ${hours}ч"
            }
        }
    }
}
