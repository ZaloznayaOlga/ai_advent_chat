package com.olgaz.aichat

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.olgaz.aichat.notification.ReminderNotificationHelper
import com.olgaz.aichat.notification.ReminderScheduler
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "AIChatApplication"

@HiltAndroidApp
class AIChatApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var reminderNotificationHelper: ReminderNotificationHelper
    @Inject lateinit var reminderScheduler: ReminderScheduler

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()

        reminderNotificationHelper.createNotificationChannels()

        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            try {
                reminderScheduler.schedulePeriodicCheck()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to schedule reminder check on startup", e)
            }
        }
    }
}
