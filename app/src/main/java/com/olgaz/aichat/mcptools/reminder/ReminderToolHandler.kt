package com.olgaz.aichat.mcptools.reminder

import com.olgaz.aichat.domain.model.McpContent
import com.olgaz.aichat.domain.model.McpPropertySchema
import com.olgaz.aichat.domain.model.McpTool
import com.olgaz.aichat.domain.model.McpToolCallResult
import com.olgaz.aichat.domain.model.McpToolInputSchema
import com.olgaz.aichat.domain.repository.LocalToolHandler
import com.olgaz.aichat.notification.ReminderScheduler
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderToolHandler @Inject constructor(
    private val reminderDao: ReminderDao,
    private val reminderScheduler: ReminderScheduler
) : LocalToolHandler {

    companion object {
        private const val TOOL_CREATE = "create_reminder"
        private const val TOOL_LIST = "list_reminders"
        private const val TOOL_COMPLETE = "complete_reminder"
        private const val TOOL_DELETE = "delete_reminder"
        private const val TOOL_DELETE_ALL = "delete_all_reminders"

        private val TOOL_NAMES = setOf(TOOL_CREATE, TOOL_LIST, TOOL_COMPLETE, TOOL_DELETE, TOOL_DELETE_ALL)
    }

    override fun getTools(): List<McpTool> = listOf(
        McpTool(
            name = TOOL_CREATE,
            description = "Создать новое напоминание или задачу. Используй этот инструмент когда пользователь просит создать, добавить или запомнить напоминание/задачу.",
            inputSchema = McpToolInputSchema(
                type = "object",
                properties = mapOf(
                    "text" to McpPropertySchema(
                        type = "string",
                        description = "Текст напоминания или задачи"
                    ),
                    "interval_minutes" to McpPropertySchema(
                        type = "integer",
                        description = "Интервал повторения в минутах (необязательно). Если указан, напоминание будет повторяющимся."
                    ),
                    "reminder_time" to McpPropertySchema(
                        type = "string",
                        description = "Дата и время напоминания в формате ISO 8601 (например, 2025-01-30T15:00:00). Необязательно."
                    )
                ),
                required = listOf("text")
            )
        ),
        McpTool(
            name = TOOL_LIST,
            description = "Получить список напоминаний/задач пользователя. Используй этот инструмент когда пользователь спрашивает о своих напоминаниях, задачах или делах.",
            inputSchema = McpToolInputSchema(
                type = "object",
                properties = mapOf(
                    "status" to McpPropertySchema(
                        type = "string",
                        description = "Фильтр по статусу: active (активные), completed (выполненные), all (все). По умолчанию active.",
                        enum = listOf("active", "completed", "all")
                    )
                )
            )
        ),
        McpTool(
            name = TOOL_COMPLETE,
            description = "Отметить напоминание/задачу как выполненное. Используй когда пользователь просит отметить, завершить или выполнить напоминание.",
            inputSchema = McpToolInputSchema(
                type = "object",
                properties = mapOf(
                    "id" to McpPropertySchema(
                        type = "integer",
                        description = "ID напоминания для отметки выполненным"
                    )
                ),
                required = listOf("id")
            )
        ),
        McpTool(
            name = TOOL_DELETE,
            description = "Удалить напоминание/задачу. Используй когда пользователь просит удалить или убрать напоминание.",
            inputSchema = McpToolInputSchema(
                type = "object",
                properties = mapOf(
                    "id" to McpPropertySchema(
                        type = "integer",
                        description = "ID напоминания для удаления"
                    )
                ),
                required = listOf("id")
            )
        ),
        McpTool(
            name = TOOL_DELETE_ALL,
            description = "Удалить все напоминания/задачи. Используй когда пользователь просит удалить все напоминания, очистить список задач или убрать всё.",
            inputSchema = McpToolInputSchema(
                type = "object"
            )
        )
    )

    override fun canHandle(toolName: String): Boolean = toolName in TOOL_NAMES

    override suspend fun handleToolCall(
        toolName: String,
        arguments: Map<String, Any?>
    ): McpToolCallResult {
        return try {
            when (toolName) {
                TOOL_CREATE -> handleCreate(arguments)
                TOOL_LIST -> handleList(arguments)
                TOOL_COMPLETE -> handleComplete(arguments)
                TOOL_DELETE -> handleDelete(arguments)
                TOOL_DELETE_ALL -> handleDeleteAll()
                else -> McpToolCallResult.Error(
                    toolName = toolName,
                    message = "Неизвестный инструмент: $toolName"
                )
            }
        } catch (e: Exception) {
            McpToolCallResult.Error(
                toolName = toolName,
                message = "Ошибка выполнения инструмента: ${e.message}"
            )
        }
    }

    private suspend fun handleCreate(arguments: Map<String, Any?>): McpToolCallResult {
        val text = arguments["text"]?.toString()
            ?: return McpToolCallResult.Error(
                toolName = TOOL_CREATE,
                message = "Не указан текст напоминания"
            )

        val intervalMinutes = arguments["interval_minutes"]?.toString()?.toIntOrNull()

        val reminderTime = arguments["reminder_time"]?.toString()?.let { timeStr ->
            parseReminderTime(timeStr)
        }

        val entity = ReminderEntity(
            text = text,
            intervalMinutes = intervalMinutes,
            reminderTime = reminderTime,
            createdAt = System.currentTimeMillis()
        )

        val id = reminderDao.insert(entity)
        reminderScheduler.onReminderCreated(id, reminderTime)

        val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        val response = buildString {
            append("Напоминание создано (ID: $id): \"$text\"")
            if (reminderTime != null) {
                append(". Время: ${dateFormat.format(Date(reminderTime))}")
            }
            if (intervalMinutes != null) {
                append(". Повторяется каждые $intervalMinutes мин.")
            }
        }

        return McpToolCallResult.Success(
            toolName = TOOL_CREATE,
            content = listOf(McpContent.Text(response))
        )
    }

    private fun parseReminderTime(timeStr: String): Long? {
        val formats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd HH:mm",
            "dd.MM.yyyy HH:mm",
            "HH:mm"
        )

        for (format in formats) {
            try {
                val sdf = SimpleDateFormat(format, Locale.getDefault())
                val date = sdf.parse(timeStr)
                if (date != null) {
                    // If only time was parsed (HH:mm), set today's date
                    if (format == "HH:mm") {
                        val now = java.util.Calendar.getInstance()
                        val parsed = java.util.Calendar.getInstance().apply { time = date }
                        now.set(java.util.Calendar.HOUR_OF_DAY, parsed.get(java.util.Calendar.HOUR_OF_DAY))
                        now.set(java.util.Calendar.MINUTE, parsed.get(java.util.Calendar.MINUTE))
                        now.set(java.util.Calendar.SECOND, 0)
                        now.set(java.util.Calendar.MILLISECOND, 0)
                        // If the time has already passed today, set it for tomorrow
                        if (now.timeInMillis <= System.currentTimeMillis()) {
                            now.add(java.util.Calendar.DAY_OF_MONTH, 1)
                        }
                        return now.timeInMillis
                    }
                    return date.time
                }
            } catch (_: Exception) {
                // Try next format
            }
        }
        return null
    }

    private suspend fun handleList(arguments: Map<String, Any?>): McpToolCallResult {
        val status = arguments["status"]?.toString() ?: "active"

        val reminders = when (status) {
            "completed" -> reminderDao.getCompletedReminders()
            "all" -> reminderDao.getAllReminders()
            else -> reminderDao.getActiveReminders()
        }

        if (reminders.isEmpty()) {
            val statusText = when (status) {
                "completed" -> "выполненных"
                "all" -> ""
                else -> "активных"
            }
            return McpToolCallResult.Success(
                toolName = TOOL_LIST,
                content = listOf(McpContent.Text("Нет ${statusText} напоминаний."))
            )
        }

        val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

        val response = buildString {
            append("Напоминания (${reminders.size}):\n")
            reminders.forEach { r ->
                val statusIcon = if (r.isCompleted) "✅" else "📌"
                append("$statusIcon ID:${r.id} — ${r.text}")
                if (r.reminderTime != null) {
                    append(" [время: ${dateFormat.format(Date(r.reminderTime))}]")
                }
                if (r.intervalMinutes != null) {
                    append(" [каждые ${r.intervalMinutes} мин.]")
                }
                append(" (создано: ${dateFormat.format(Date(r.createdAt))})")
                if (r.isCompleted && r.completedAt != null) {
                    append(" (выполнено: ${dateFormat.format(Date(r.completedAt))})")
                }
                append("\n")
            }
        }

        return McpToolCallResult.Success(
            toolName = TOOL_LIST,
            content = listOf(McpContent.Text(response.trimEnd()))
        )
    }

    private suspend fun handleComplete(arguments: Map<String, Any?>): McpToolCallResult {
        val id = arguments["id"]?.toString()?.toLongOrNull()
            ?: return McpToolCallResult.Error(
                toolName = TOOL_COMPLETE,
                message = "Не указан ID напоминания"
            )

        val updated = reminderDao.markCompleted(id, System.currentTimeMillis())

        return if (updated > 0) {
            reminderScheduler.onReminderCompleted(id)
            McpToolCallResult.Success(
                toolName = TOOL_COMPLETE,
                content = listOf(McpContent.Text("Напоминание ID:$id отмечено как выполненное."))
            )
        } else {
            McpToolCallResult.Error(
                toolName = TOOL_COMPLETE,
                message = "Напоминание с ID:$id не найдено."
            )
        }
    }

    private suspend fun handleDelete(arguments: Map<String, Any?>): McpToolCallResult {
        val id = arguments["id"]?.toString()?.toLongOrNull()
            ?: return McpToolCallResult.Error(
                toolName = TOOL_DELETE,
                message = "Не указан ID напоминания"
            )

        val deleted = reminderDao.delete(id)

        return if (deleted > 0) {
            reminderScheduler.onReminderCompleted(id)
            McpToolCallResult.Success(
                toolName = TOOL_DELETE,
                content = listOf(McpContent.Text("Напоминание ID:$id удалено."))
            )
        } else {
            McpToolCallResult.Error(
                toolName = TOOL_DELETE,
                message = "Напоминание с ID:$id не найдено."
            )
        }
    }

    private suspend fun handleDeleteAll(): McpToolCallResult {
        val deleted = reminderDao.deleteAll()

        if (deleted > 0) {
            reminderScheduler.onAllRemindersDeleted()
        }

        return McpToolCallResult.Success(
            toolName = TOOL_DELETE_ALL,
            content = listOf(McpContent.Text(
                if (deleted > 0) "Все напоминания удалены (всего: $deleted)."
                else "Список напоминаний уже пуст."
            ))
        )
    }
}
