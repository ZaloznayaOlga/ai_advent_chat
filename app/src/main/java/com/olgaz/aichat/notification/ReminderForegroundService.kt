package com.olgaz.aichat.notification

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.olgaz.aichat.mcptools.reminder.ReminderDao
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "ReminderFgService"

@AndroidEntryPoint
class ReminderForegroundService : Service() {

    @Inject lateinit var reminderDao: ReminderDao
    @Inject lateinit var notificationHelper: ReminderNotificationHelper

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startForegroundServiceInternal()
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            ACTION_UPDATE -> updateNotification()
        }
        return START_STICKY
    }

    private fun startForegroundServiceInternal() {
        notificationHelper.createNotificationChannels()

        serviceScope.launch {
            val count = reminderDao.getActiveReminderCount()
            if (count == 0) {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return@launch
            }

            val notification = notificationHelper.buildForegroundServiceNotification(count)

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(
                        ReminderNotificationHelper.NOTIFICATION_ID_FOREGROUND,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                    )
                } else {
                    startForeground(
                        ReminderNotificationHelper.NOTIFICATION_ID_FOREGROUND,
                        notification
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start foreground", e)
            }
        }
    }

    private fun updateNotification() {
        serviceScope.launch {
            val count = reminderDao.getActiveReminderCount()
            if (count == 0) {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return@launch
            }

            val notification = notificationHelper.buildForegroundServiceNotification(count)
            NotificationManagerCompat.from(this@ReminderForegroundService)
                .notify(ReminderNotificationHelper.NOTIFICATION_ID_FOREGROUND, notification)
        }
    }

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_UPDATE = "ACTION_UPDATE"

        fun start(context: Context) {
            val intent = Intent(context, ReminderForegroundService::class.java).apply {
                action = ACTION_START
            }
            try {
                ContextCompat.startForegroundService(context, intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start service", e)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, ReminderForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to stop service", e)
            }
        }

        fun update(context: Context) {
            val intent = Intent(context, ReminderForegroundService::class.java).apply {
                action = ACTION_UPDATE
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update service", e)
            }
        }
    }
}
