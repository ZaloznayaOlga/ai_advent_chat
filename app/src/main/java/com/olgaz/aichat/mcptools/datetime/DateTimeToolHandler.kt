package com.olgaz.aichat.mcptools.datetime

import com.olgaz.aichat.domain.model.McpContent
import com.olgaz.aichat.domain.model.McpPropertySchema
import com.olgaz.aichat.domain.model.McpTool
import com.olgaz.aichat.domain.model.McpToolCallResult
import com.olgaz.aichat.domain.model.McpToolInputSchema
import com.olgaz.aichat.domain.repository.LocalToolHandler
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * MCP инструмент для получения текущего времени и даты
 */
@Singleton
class DateTimeToolHandler @Inject constructor() : LocalToolHandler {

    companion object {
        private const val TOOL_NAME = "get_current_datetime"
        private const val TOOL_DESCRIPTION = """
            Получить текущие дату, время и день недели. 
            Используй этот инструмент когда пользователь спрашивает о текущем времени, дате, дне недели или любых связанных вопросах.
        """

        private val TOOL_NAMES = setOf(TOOL_NAME)
    }

    override fun getTools(): List<McpTool> = listOf(
        McpTool(
            name = TOOL_NAME,
            description = TOOL_DESCRIPTION,
            inputSchema = McpToolInputSchema(
                type = "object",
                properties = mapOf(
                    "format" to McpPropertySchema(
                        type = "string",
                        description = "Формат вывода: short (короткий), full (полный), time_only (только время), date_only (только дата). По умолчанию full.",
                        enum = listOf("short", "full", "time_only", "date_only")
                    ),
                    "timezone_offset" to McpPropertySchema(
                        type = "integer",
                        description = "Смещение часового пояса в минутах относительно UTC (необязательно). Например, для UTC+3 укажите 180."
                    )
                )
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
                TOOL_NAME -> handleGetDateTime(arguments)
                else -> McpToolCallResult.Error(
                    toolName = toolName,
                    message = "Неизвестный инструмент: $toolName"
                )
            }
        } catch (e: Exception) {
            McpToolCallResult.Error(
                toolName = toolName,
                message = "Ошибка получения даты и времени: ${e.message}"
            )
        }
    }

    private fun handleGetDateTime(arguments: Map<String, Any?>): McpToolCallResult {
        val format = arguments["format"]?.toString() ?: "full"
        val timezoneOffset = arguments["timezone_offset"]?.toString()?.toIntOrNull()

        val calendar = Calendar.getInstance()

        // Применяем смещение часового пояса если указано
        if (timezoneOffset != null) {
            calendar.timeZone = java.util.TimeZone.getTimeZone("GMT")
            calendar.add(Calendar.MINUTE, timezoneOffset)
        }

        val date = calendar.time

        // Форматируем вывод в зависимости от запроса
        val response = when (format) {
            "short" -> getShortFormat(date)
            "time_only" -> getTimeOnly(date)
            "date_only" -> getDateOnly(date)
            else -> getFullFormat(date)
        }

        return McpToolCallResult.Success(
            toolName = TOOL_NAME,
            content = listOf(McpContent.Text(response))
        )
    }

    private fun getFullFormat(date: Date): String {
        val calendar = Calendar.getInstance().apply { time = date }
        val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dateFormat = SimpleDateFormat("d MMMM", Locale("ru"))
        val dayOfWeekFormat = SimpleDateFormat("EEEE", Locale("ru"))

        val month = dateFormat.format(date).split(" ")[1]
        val time = timeFormat.format(date)
        val dayOfWeek = dayOfWeekFormat.format(date).lowercase(Locale("ru"))

        // Определяем правильное окончание для дня месяца
        val daySuffix = when (dayOfMonth) {
            1, 21, 31 -> "-е"
            2, 22 -> "-е"
            3, 23 -> "-е"
            4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 24, 25, 26, 27, 28, 29, 30 -> "-е"
            else -> "-е"
        }

        return "сегодня $dayOfMonth$daySuffix $month $time, $dayOfWeek!"
    }

    private fun getShortFormat(date: Date): String {
        val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        return dateFormat.format(date)
    }

    private fun getTimeOnly(date: Date): String {
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return "Текущее время: ${timeFormat.format(date)}"
    }

    private fun getDateOnly(date: Date): String {
        val dateFormat = SimpleDateFormat("d MMMM yyyy года, EEEE", Locale("ru"))
        return "Сегодня ${dateFormat.format(date)}"
    }

}