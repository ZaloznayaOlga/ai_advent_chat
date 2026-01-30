package com.olgaz.aichat.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.olgaz.aichat.mcptools.reminder.ReminderDao
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "ReminderAlarmReceiver"

@AndroidEntryPoint
class ReminderAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_REMINDER_ALARM = "com.olgaz.aichat.REMINDER_ALARM"
        const val EXTRA_REMINDER_ID = "reminder_id"
    }

    @Inject lateinit var reminderDao: ReminderDao
    @Inject lateinit var notificationHelper: ReminderNotificationHelper

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_REMINDER_ALARM) return

        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1)
        if (reminderId == -1L) {
            Log.w(TAG, "Received alarm without reminder ID")
            return
        }

        Log.d(TAG, "Alarm fired for reminder $reminderId")

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val reminder = reminderDao.getReminderById(reminderId)

                if (reminder == null) {
                    Log.w(TAG, "Reminder $reminderId not found in DB")
                    return@launch
                }

                if (reminder.isCompleted) {
                    Log.d(TAG, "Reminder $reminderId already completed, skipping")
                    return@launch
                }

                notificationHelper.showUrgentReminderNotification(reminder)
                reminderDao.markNotified(reminderId)

                Log.d(TAG, "Notification shown for reminder $reminderId: ${reminder.text}")
            } catch (e: Exception) {
                Log.e(TAG, "Error processing alarm for reminder $reminderId", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
