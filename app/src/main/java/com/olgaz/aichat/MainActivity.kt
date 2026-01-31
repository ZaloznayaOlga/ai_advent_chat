package com.olgaz.aichat

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.olgaz.aichat.notification.ReminderNotificationHelper
import com.olgaz.aichat.presentation.chat.ChatScreen
import com.olgaz.aichat.presentation.chat.ChatViewModel
import com.olgaz.aichat.ui.theme.AIChatTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: ChatViewModel by viewModels()

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Permission result handled silently — worker checks permission before posting
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        requestNotificationPermissionIfNeeded()
        handleReminderSummaryIntent(intent)

        setContent {
            AIChatTheme {
                ChatScreen(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleReminderSummaryIntent(intent)
    }

    private fun handleReminderSummaryIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(
                ReminderNotificationHelper.EXTRA_FROM_REMINDER_SUMMARY, false
            ) == true
        ) {
            viewModel.reloadMessages()
            intent.removeExtra(ReminderNotificationHelper.EXTRA_FROM_REMINDER_SUMMARY)
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
