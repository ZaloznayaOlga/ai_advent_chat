package com.olgaz.aichat.notification

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.olgaz.aichat.data.local.dao.SettingsDao
import com.olgaz.aichat.data.local.mapper.SettingsMapper
import com.olgaz.aichat.data.remote.api.ChatApi
import com.olgaz.aichat.data.remote.dto.ChatRequestDto
import com.olgaz.aichat.data.remote.dto.MessageDto
import com.olgaz.aichat.di.DeepSeekApi
import com.olgaz.aichat.di.HuggingFaceApi
import com.olgaz.aichat.di.OpenAiApi
import com.olgaz.aichat.domain.model.AiProvider
import com.olgaz.aichat.domain.model.ChatSettings
import com.olgaz.aichat.mcptools.reminder.ReminderDao
import com.olgaz.aichat.mcptools.reminder.ReminderEntity
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TAG = "ReminderCheckWorker"

@HiltWorker
class ReminderCheckWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val reminderDao: ReminderDao,
    private val settingsDao: SettingsDao,
    private val notificationHelper: ReminderNotificationHelper,
    @DeepSeekApi private val deepSeekApi: ChatApi,
    @OpenAiApi private val openAiApi: ChatApi,
    @HuggingFaceApi private val huggingFaceApi: ChatApi
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "ReminderCheckWorker started")

            val activeCount = reminderDao.getActiveReminderCount()
            if (activeCount == 0) {
                Log.d(TAG, "No active reminders, stopping service")
                ReminderForegroundService.stop(applicationContext)
                return Result.success()
            }

            // Ensure foreground service is running
            ReminderForegroundService.start(applicationContext)

            // Generate summary notification
            val activeReminders = reminderDao.getActiveRemindersSortedByTime()
            val summaryText = generateSummary(activeReminders)

            Log.d(TAG, "Summary: $summaryText")
            notificationHelper.showSummaryNotification(summaryText, activeCount)

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error in doWork", e)
            Result.retry()
        }
    }

    private suspend fun generateSummary(reminders: List<ReminderEntity>): String {
        return try {
            generateAiSummary(reminders)
        } catch (e: Exception) {
            Log.w(TAG, "AI summary failed, using fallback", e)
            generateLocalFallbackSummary(reminders)
        }
    }

    private suspend fun generateAiSummary(reminders: List<ReminderEntity>): String {
        val settingsEntity = settingsDao.getSettings()
        val settings = settingsEntity?.let { SettingsMapper.toDomain(it) } ?: ChatSettings()

        val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        val reminderList = reminders.joinToString("\n") { r ->
            buildString {
                append("- ${r.text}")
                r.reminderTime?.let { append(" (время: ${dateFormat.format(Date(it))})") }
                r.intervalMinutes?.let { append(" [каждые $it мин.]") }
            }
        }

        val prompt = """
Создай краткую сводку напоминаний для push-уведомления (максимум 3 предложения).
Укажи количество задач, самую срочную задачу, и общий статус.
Текст должен быть на русском языке, краткий и информативный.

Активные напоминания:
$reminderList
        """.trimIndent()

        val request = ChatRequestDto(
            model = settings.model.apiName,
            messages = listOf(
                MessageDto(
                    role = "system",
                    content = "Ты — краткий помощник для уведомлений. Отвечай коротко, на русском языке."
                ),
                MessageDto(role = "user", content = prompt)
            ),
            temperature = 0.3f
        )

        val api = selectApi(settings.provider)
        val response = api.sendMessage(request)

        return response.choices.firstOrNull()?.message?.content
            ?: generateLocalFallbackSummary(reminders)
    }

    private fun selectApi(provider: AiProvider): ChatApi {
        return when (provider) {
            AiProvider.DEEPSEEK -> deepSeekApi
            AiProvider.OPENAI -> openAiApi
            AiProvider.HUGGINGFACE -> huggingFaceApi
        }
    }

    private fun generateLocalFallbackSummary(reminders: List<ReminderEntity>): String {
        val dateFormat = SimpleDateFormat("dd.MM HH:mm", Locale.getDefault())
        val count = reminders.size

        val nearest = reminders.firstOrNull { it.reminderTime != null }
        val nearestText = nearest?.let {
            val timeStr = dateFormat.format(Date(it.reminderTime!!))
            "Ближайшее: \"${it.text.take(30)}\" в $timeStr"
        }

        val taskList = reminders.take(3).joinToString(", ") { it.text.take(20) }
        val moreText = if (count > 3) " и ещё ${count - 3}" else ""

        return buildString {
            append("Активных задач: $count.")
            if (nearestText != null) append(" $nearestText.")
            append(" Задачи: $taskList$moreText")
        }
    }
}
