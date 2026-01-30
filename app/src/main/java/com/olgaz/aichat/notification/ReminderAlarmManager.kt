package com.olgaz.aichat.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.olgaz.aichat.mcptools.reminder.ReminderDao
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "ReminderAlarmManager"

@Singleton
class ReminderAlarmManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val reminderDao: ReminderDao
) {

    private val alarmManager: AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleExactAlarm(reminderId: Long, reminderTime: Long) {
        if (reminderTime <= System.currentTimeMillis()) {
            Log.w(TAG, "Reminder $reminderId time is in the past, skipping alarm")
            return
        }

        val pendingIntent = createPendingIntent(reminderId)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                // Fallback: inexact but Doze-aware
                Log.w(TAG, "Exact alarms not permitted, using setAndAllowWhileIdle")
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    reminderTime,
                    pendingIntent
                )
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    reminderTime,
                    pendingIntent
                )
            }
            Log.d(TAG, "Scheduled exact alarm for reminder $reminderId at $reminderTime")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException scheduling exact alarm, using fallback", e)
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                reminderTime,
                pendingIntent
            )
        }
    }

    fun cancelAlarm(reminderId: Long) {
        val pendingIntent = createPendingIntent(reminderId)
        alarmManager.cancel(pendingIntent)
        Log.d(TAG, "Cancelled alarm for reminder $reminderId")
    }

    fun cancelAllAlarms(reminderIds: List<Long>) {
        for (id in reminderIds) {
            cancelAlarm(id)
        }
    }

    suspend fun rescheduleAllAlarms() {
        val now = System.currentTimeMillis()
        val pendingReminders = reminderDao.getPendingTimedReminders(now)

        Log.d(TAG, "Rescheduling ${pendingReminders.size} alarms after boot")
        for (reminder in pendingReminders) {
            scheduleExactAlarm(reminder.id, reminder.reminderTime!!)
        }
    }

    private fun createPendingIntent(reminderId: Long): PendingIntent {
        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            action = ReminderAlarmReceiver.ACTION_REMINDER_ALARM
            putExtra(ReminderAlarmReceiver.EXTRA_REMINDER_ID, reminderId)
        }
        return PendingIntent.getBroadcast(
            context,
            reminderId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
