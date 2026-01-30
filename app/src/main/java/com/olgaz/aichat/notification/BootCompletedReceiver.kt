package com.olgaz.aichat.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "BootCompletedReceiver"

@AndroidEntryPoint
class BootCompletedReceiver : BroadcastReceiver() {

    @Inject lateinit var reminderScheduler: ReminderScheduler
    @Inject lateinit var reminderAlarmManager: ReminderAlarmManager

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            Log.d(TAG, "Boot completed, re-scheduling reminder check and alarms")

            val pendingResult = goAsync()

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    reminderScheduler.schedulePeriodicCheck()
                    reminderAlarmManager.rescheduleAllAlarms()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to schedule after boot", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
