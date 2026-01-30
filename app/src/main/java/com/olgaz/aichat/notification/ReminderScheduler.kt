package com.olgaz.aichat.notification

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import com.olgaz.aichat.data.local.dao.SettingsDao
import com.olgaz.aichat.mcptools.reminder.ReminderDao
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val reminderDao: ReminderDao,
    private val settingsDao: SettingsDao,
    private val reminderAlarmManager: ReminderAlarmManager
) {

    companion object {
        const val WORK_NAME = "reminder_periodic_check"
    }

    suspend fun schedulePeriodicCheck() {
        val settingsEntity = settingsDao.getSettings()
        val intervalMinutes = settingsEntity?.reminderCheckIntervalMinutes ?: 30
        schedulePeriodicCheck(intervalMinutes)
    }

    fun schedulePeriodicCheck(intervalMinutes: Int) {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        val interval = intervalMinutes.toLong().coerceAtLeast(15)

        val workRequest = PeriodicWorkRequestBuilder<ReminderCheckWorker>(
            interval, TimeUnit.MINUTES,
            5, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }

    fun cancelPeriodicCheck() {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        ReminderForegroundService.stop(context)
    }

    suspend fun onReminderCreated(reminderId: Long, reminderTime: Long?) {
        val count = reminderDao.getActiveReminderCount()
        if (count > 0) {
            schedulePeriodicCheck()
            ReminderForegroundService.start(context)
        }

        if (reminderTime != null) {
            reminderAlarmManager.scheduleExactAlarm(reminderId, reminderTime)
        }
    }

    suspend fun onReminderCompleted(reminderId: Long) {
        reminderAlarmManager.cancelAlarm(reminderId)

        val count = reminderDao.getActiveReminderCount()
        if (count == 0) {
            ReminderForegroundService.stop(context)
        } else {
            ReminderForegroundService.update(context)
        }
    }

    suspend fun onAllRemindersDeleted() {
        val ids = reminderDao.getPendingTimedReminderIds()
        reminderAlarmManager.cancelAllAlarms(ids)
        ReminderForegroundService.stop(context)
    }
}
