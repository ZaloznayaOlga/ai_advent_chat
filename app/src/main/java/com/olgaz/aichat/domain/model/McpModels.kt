package com.olgaz.aichat.domain.model

/**
 * Состояние подключения к MCP серверу
 */
sealed class McpConnectionState {
    data object Disconnected : McpConnectionState()
    data object Connecting : McpConnectionState()
    data object Connected : McpConnectionState()
    data class Error(val message: String, val throwable: Throwable? = null) : McpConnectionState()
}

/**
 * MCP Tool - инструмент, предоставляемый сервером
 */
data class McpTool(
    val name: String,
    val description: String?,
    val inputSchema: McpToolInputSchema
)

/**
 * JSON Schema для входных параметров инструмента
 */
data class McpToolInputSchema(
    val type: String = "object",
    val properties: Map<String, McpPropertySchema> = emptyMap(),
    val required: List<String> = emptyList()
)

/**
 * Описание свойства в JSON Schema
 */
data class McpPropertySchema(
    val type: String,
    val description: String? = null,
    val enum: List<String>? = null
)

/**
 * Результат вызова MCP инструмента
 */
sealed class McpToolCallResult {
    data class Success(
        val toolName: String,
        val content: List<McpContent>
    ) : McpToolCallResult()

    data class Error(
        val toolName: String,
        val message: String,
        val isRetryable: Boolean = false
    ) : McpToolCallResult()
}

/**
 * Контент из MCP ответа
 */
sealed class McpContent {
    data class Text(val text: String) : McpContent()
    data class Image(val data: String, val mimeType: String) : McpContent()
    data class Resource(val uri: String, val mimeType: String?, val text: String?) : McpContent()
}

/**
 * Конфигурация MCP сервера
 */
data class McpServerConfig(
    val url: String,
    val enabled: Boolean = true,
    val connectionTimeoutMs: Long = 30_000,
    val readTimeoutMs: Long = 60_000
)
