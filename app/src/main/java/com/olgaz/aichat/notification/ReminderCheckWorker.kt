package com.olgaz.aichat.notification

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.olgaz.aichat.BuildConfig
import com.olgaz.aichat.data.local.dao.MessageDao
import com.olgaz.aichat.data.local.dao.SettingsDao
import com.olgaz.aichat.data.local.entity.MessageEntity
import com.olgaz.aichat.data.local.mapper.SettingsMapper
import com.olgaz.aichat.data.remote.api.ChatApi
import com.olgaz.aichat.data.remote.dto.ChatRequestDto
import com.olgaz.aichat.data.remote.dto.FunctionDto
import com.olgaz.aichat.data.remote.dto.MessageDto
import com.olgaz.aichat.data.remote.dto.ToolDto
import com.olgaz.aichat.di.DeepSeekApi
import com.olgaz.aichat.di.HuggingFaceApi
import com.olgaz.aichat.di.OpenAiApi
import com.olgaz.aichat.domain.model.AiProvider
import com.olgaz.aichat.domain.model.ChatSettings
import com.olgaz.aichat.domain.model.McpConnectionState
import com.olgaz.aichat.domain.model.McpContent
import com.olgaz.aichat.domain.model.McpServerConfig
import com.olgaz.aichat.domain.model.McpTool
import com.olgaz.aichat.domain.model.McpToolCallResult
import com.olgaz.aichat.domain.repository.McpRepository
import com.olgaz.aichat.mcptools.reminder.ReminderDao
import com.olgaz.aichat.mcptools.reminder.ReminderEntity
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

private const val TAG = "ReminderCheckWorker"

@HiltWorker
class ReminderCheckWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val reminderDao: ReminderDao,
    private val settingsDao: SettingsDao,
    private val notificationHelper: ReminderNotificationHelper,
    private val messageDao: MessageDao,
    @DeepSeekApi private val deepSeekApi: ChatApi,
    @OpenAiApi private val openAiApi: ChatApi,
    @HuggingFaceApi private val huggingFaceApi: ChatApi,
    private val mcpRepository: McpRepository
) : CoroutineWorker(appContext, workerParams) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

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

            val settingsEntity = settingsDao.getSettings()
            val settings = settingsEntity?.let { SettingsMapper.toDomain(it) } ?: ChatSettings()

            // Generate summary notification
            val activeReminders = reminderDao.getActiveRemindersSortedByTime()
            val summaryText = generateSummary(activeReminders, settings)

            Log.d(TAG, "Summary: $summaryText")

            // Save summary as assistant message in chat
            val messageEntity = MessageEntity(
                id = UUID.randomUUID().toString(),
                content = summaryText,
                role = "ASSISTANT",
                timestamp = System.currentTimeMillis(),
                displayContent = summaryText
            )
            messageDao.insertMessage(messageEntity)

            notificationHelper.showSummaryNotification(summaryText, activeCount)

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error in doWork", e)
            Result.retry()
        }
    }

    private suspend fun generateSummary(
        reminders: List<ReminderEntity>,
        settings: ChatSettings
    ): String {
        return try {
            generateAiSummary(reminders, settings)
        } catch (e: Exception) {
            Log.w(TAG, "AI summary failed, using fallback", e)
            generateLocalFallbackSummary(reminders)
        }
    }

    private suspend fun generateAiSummary(
        reminders: List<ReminderEntity>,
        settings: ChatSettings
    ): String {
        val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        val reminderList = reminders.joinToString("\n") { r ->
            buildString {
                append("- ${r.text}")
                r.reminderTime?.let { append(" (время: ${dateFormat.format(Date(it))})") }
                r.intervalMinutes?.let { append(" [каждые $it мин.]") }
            }
        }

        // Prepare weather tools if enabled
        val weatherTools = getWeatherTools(settings)
        val hasWeatherTools = weatherTools.isNotEmpty()

        val weatherInstruction = if (hasWeatherTools) {
            "\n- ОБЯЗАТЕЛЬНО вызови инструмент погоды для города ${settings.selectedWeatherCity}, чтобы получить актуальные данные" +
            "\n- После списка задач добавь блок с погодой: кратко какая сегодня погода, и шутку или рекомендацию по погоде с эмодзи"
        } else ""

        val systemPrompt = buildString {
            append("Ты — помощник, который красиво оформляет уведомления с эмодзи. Отвечай на русском языке.")
            if (hasWeatherTools) {
                append("\n\nУ тебя есть доступ к инструменту погоды. ОБЯЗАТЕЛЬНО используй его для получения актуальной погоды.")
                append("\nГород пользователя по умолчанию: ${settings.selectedWeatherCity}")
            }
        }

        val prompt = """
Оформи красивую сводку активных напоминаний для push-уведомления.
Используй эмодзи для оформления. Формат:
- Заголовок с эмодзи и количеством задач
- Самая срочная задача (выдели эмодзи ⏰)
- Список остальных задач с подходящими эмодзи$weatherInstruction
- Краткое мотивирующее завершение с эмодзи
Текст на русском, красивый и аккуратный. Максимум 12 строк.

Активные напоминания:
$reminderList
        """.trimIndent()

        val toolDtos = weatherTools.map { it.toToolDto() }.ifEmpty { null }

        val allMessages = mutableListOf(
            MessageDto(role = "system", content = systemPrompt),
            MessageDto(role = "user", content = prompt)
        )

        val api = selectApi(settings.provider)

        // Tool calling loop (max 3 iterations for summary generation)
        var iterations = 0
        val maxIterations = 3

        while (iterations < maxIterations) {
            iterations++

            val request = ChatRequestDto(
                model = settings.model.apiName,
                messages = allMessages.toList(),
                temperature = 0.3f,
                tools = toolDtos,
                toolChoice = if (toolDtos != null) "auto" else null
            )

            Log.d(TAG, "Sending summary request (iteration $iterations), tools: ${toolDtos?.size ?: 0}")
            val response = api.sendMessage(request)
            val assistantMsg = response.choices.firstOrNull()?.message ?: break

            val toolCalls = assistantMsg.toolCalls
            if (toolCalls.isNullOrEmpty()) {
                // No tool calls — final response
                return assistantMsg.content
                    ?: generateLocalFallbackSummary(reminders)
            }

            // AI requested tool calls — add assistant message and process each call
            allMessages.add(
                MessageDto(
                    role = "assistant",
                    content = assistantMsg.content,
                    toolCalls = toolCalls
                )
            )

            for (toolCall in toolCalls) {
                val toolName = toolCall.function.name
                val argsJson = toolCall.function.arguments

                Log.d(TAG, "AI called tool: $toolName with args: $argsJson")

                val arguments = parseToolArguments(argsJson)
                val toolContent = executeToolCall(toolName, arguments)

                allMessages.add(
                    MessageDto(
                        role = "tool",
                        content = toolContent,
                        toolCallId = toolCall.id
                    )
                )

                Log.d(TAG, "Tool $toolName result: ${toolContent.take(100)}...")
            }
        }

        // Reached max iterations — return last content or fallback
        return generateLocalFallbackSummary(reminders)
    }

    /**
     * Подключается к MCP и возвращает доступные weather tools.
     * Если погода не включена или MCP недоступен — возвращает пустой список.
     */
    private suspend fun getWeatherTools(settings: ChatSettings): List<McpTool> {
        if (!settings.mcpWeatherEnabled) return emptyList()

        return try {
            val mcpUrl = settings.mcpServerUrl.ifEmpty { BuildConfig.MCP_SERVER_URL }
            if (mcpUrl.isBlank()) return emptyList()

            // Connect to MCP if not already connected
            val state = mcpRepository.connectionState.value
            if (state !is McpConnectionState.Connected) {
                val connectResult = mcpRepository.connect(McpServerConfig(url = mcpUrl))
                if (connectResult.isFailure) {
                    Log.w(TAG, "Failed to connect to MCP for weather tools")
                    return emptyList()
                }
            }

            val tools = mcpRepository.availableTools.value
            tools.filter { it.name.contains("weather", ignoreCase = true) }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get weather tools from MCP", e)
            emptyList()
        }
    }

    private fun parseToolArguments(argsJson: String): Map<String, Any?> {
        return try {
            val parsed = json.parseToJsonElement(argsJson)
            if (parsed is JsonObject) {
                parsed.entries.associate { (k, v) ->
                    k to when (v) {
                        is JsonPrimitive -> v.content
                        else -> v.toString()
                    }
                }
            } else {
                emptyMap()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse tool arguments", e)
            emptyMap()
        }
    }

    private suspend fun executeToolCall(
        toolName: String,
        arguments: Map<String, Any?>
    ): String {
        // Call MCP tool with automatic reconnect on failure
        val firstAttempt = mcpRepository.callTool(toolName, arguments)
        val result = if (firstAttempt.isFailure) {
            Log.w(TAG, "MCP tool call failed, attempting reconnect...")
            val reconnectResult = mcpRepository.reconnect()
            if (reconnectResult.isSuccess) {
                mcpRepository.callTool(toolName, arguments)
            } else {
                return "Error: Не удалось получить данные от MCP сервера"
            }
        } else {
            firstAttempt
        }

        return when (val callResult = result.getOrNull()) {
            is McpToolCallResult.Success -> {
                callResult.content.joinToString("\n") { content ->
                    when (content) {
                        is McpContent.Text -> content.text
                        is McpContent.Image -> "[Image: ${content.mimeType}]"
                        is McpContent.Resource -> content.text ?: "[Resource: ${content.uri}]"
                    }
                }
            }
            is McpToolCallResult.Error -> "Error: ${callResult.message}"
            null -> "Error: ${result.exceptionOrNull()?.message ?: "Unknown error"}"
        }
    }

    private fun McpTool.toToolDto(): ToolDto {
        val parameters = buildJsonObject {
            put("type", inputSchema.type)
            if (inputSchema.properties.isNotEmpty()) {
                putJsonObject("properties") {
                    inputSchema.properties.forEach { (propName, propSchema) ->
                        putJsonObject(propName) {
                            put("type", propSchema.type)
                            propSchema.description?.let { put("description", it) }
                            propSchema.enum?.let { enumList ->
                                putJsonArray("enum") {
                                    enumList.forEach { add(JsonPrimitive(it)) }
                                }
                            }
                        }
                    }
                }
            }
            if (inputSchema.required.isNotEmpty()) {
                putJsonArray("required") {
                    inputSchema.required.forEach { add(JsonPrimitive(it)) }
                }
            }
        }

        return ToolDto(
            type = "function",
            function = FunctionDto(
                name = name,
                description = description,
                parameters = parameters
            )
        )
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

        return buildString {
            appendLine("\uD83D\uDCCB Сводка задач: $count активных")
            appendLine()

            if (nearest != null) {
                val timeStr = dateFormat.format(Date(nearest.reminderTime!!))
                appendLine("⏰ Ближайшее: \"${nearest.text.take(30)}\" в $timeStr")
                appendLine()
            }

            reminders.take(5).forEachIndexed { index, reminder ->
                val emoji = when (index) {
                    0 -> "\uD83D\uDD34"
                    1 -> "\uD83D\uDFE0"
                    2 -> "\uD83D\uDFE1"
                    else -> "\uD83D\uDD35"
                }
                val timeInfo = reminder.reminderTime?.let {
                    " (${dateFormat.format(Date(it))})"
                } ?: ""
                appendLine("$emoji ${reminder.text.take(40)}$timeInfo")
            }

            if (count > 5) {
                appendLine("... и ещё ${count - 5}")
            }

            appendLine()
            append("\uD83D\uDCAA Вперёд к выполнению!")
        }
    }
}
