package com.olgaz.aichat.data.repository

import android.util.Log
import com.olgaz.aichat.domain.model.McpConnectionState
import com.olgaz.aichat.domain.model.McpContent
import com.olgaz.aichat.domain.model.McpServerConfig
import com.olgaz.aichat.domain.model.McpTool
import com.olgaz.aichat.domain.model.McpToolCallResult
import com.olgaz.aichat.domain.model.McpToolInputSchema
import com.olgaz.aichat.domain.repository.McpRepository
import io.ktor.client.HttpClient
import io.modelcontextprotocol.kotlin.sdk.Implementation
import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.client.mcpSseTransport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "McpRepository"

@Singleton
class McpRepositoryImpl @Inject constructor(
    private val httpClient: HttpClient
) : McpRepository {

    private var mcpClient: Client? = null
    private var currentConfig: McpServerConfig? = null

    private val _connectionState = MutableStateFlow<McpConnectionState>(McpConnectionState.Disconnected)
    override val connectionState: StateFlow<McpConnectionState> = _connectionState.asStateFlow()

    private val _availableTools = MutableStateFlow<List<McpTool>>(emptyList())
    override val availableTools: StateFlow<List<McpTool>> = _availableTools.asStateFlow()

    override suspend fun connect(config: McpServerConfig): Result<Unit> = withContext(Dispatchers.IO) {
        if (config.url.isBlank()) {
            val error = McpConnectionState.Error("MCP URL не настроен")
            _connectionState.value = error
            return@withContext Result.failure(IllegalArgumentException(error.message))
        }

        _connectionState.value = McpConnectionState.Connecting

        try {
            // Закрыть предыдущее подключение
            mcpClient?.close()

            val client = Client(
                clientInfo = Implementation(
                    name = "AIChat",
                    version = "1.0.0"
                )
            )

            // Используем SSE транспорт через Ktor HttpClient extension
            Log.i(TAG, "Connect to MCP server... ${config.url}")
            val transport = httpClient.mcpSseTransport(config.url)

            client.connect(transport)

            mcpClient = client
            currentConfig = config
            _connectionState.value = McpConnectionState.Connected

            // Загрузить инструменты после подключения
            refreshTools()

            Log.i(TAG, "Successfully connected to MCP server: ${config.url}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect to MCP server", e)
            _connectionState.value = McpConnectionState.Error(
                message = mapConnectionError(e),
                throwable = e
            )
            Result.failure(e)
        }
    }

    override suspend fun disconnect(): Unit = withContext(Dispatchers.IO) {
        try {
            mcpClient?.close()
        } catch (e: Exception) {
            Log.w(TAG, "Error during disconnect", e)
        } finally {
            mcpClient = null
            _connectionState.value = McpConnectionState.Disconnected
            _availableTools.value = emptyList()
        }
    }

    override suspend fun refreshTools(): Result<List<McpTool>> = withContext(Dispatchers.IO) {
        val client = mcpClient
        if (client == null) {
            return@withContext Result.failure(IllegalStateException("Не подключено к MCP серверу"))
        }

        try {
            val toolsResponse = client.listTools()
            val tools = toolsResponse.tools.map { sdkTool ->
                McpTool(
                    name = sdkTool.name,
                    description = sdkTool.description,
                    inputSchema = McpToolInputSchema() // SDK inputSchema не доступен напрямую
                )
            }

            _availableTools.value = tools
            Log.i(TAG, "Loaded ${tools.size} MCP tools")
            Result.success(tools)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load tools", e)
            Result.failure(e)
        }
    }

    override suspend fun callTool(
        toolName: String,
        arguments: Map<String, Any?>
    ): Result<McpToolCallResult> = withContext(Dispatchers.IO) {
        val client = mcpClient
        if (client == null) {
            return@withContext Result.failure(IllegalStateException("Не подключено к MCP серверу"))
        }

        try {
            Log.d(TAG, "Calling tool: $toolName with args: $arguments")

            // Конвертируем Map в JsonObject для SDK
            val jsonArguments = arguments.mapValues { (_, value) ->
                when (value) {
                    is String -> JsonPrimitive(value)
                    is Number -> JsonPrimitive(value)
                    is Boolean -> JsonPrimitive(value)
                    null -> JsonPrimitive("")
                    else -> JsonPrimitive(value.toString())
                }
            }

            val result = client.callTool(
                name = toolName,
                arguments = jsonArguments
            )

            val content = result.content.mapNotNull { c ->
                when (c) {
                    is io.modelcontextprotocol.kotlin.sdk.TextContent -> McpContent.Text(c.text)
                    is io.modelcontextprotocol.kotlin.sdk.ImageContent -> McpContent.Image(c.data, c.mimeType)
                    is io.modelcontextprotocol.kotlin.sdk.EmbeddedResource -> {
                        val resource = c.resource
                        when (resource) {
                            is io.modelcontextprotocol.kotlin.sdk.TextResourceContents ->
                                McpContent.Resource(resource.uri, resource.mimeType ?: "", resource.text)
                            is io.modelcontextprotocol.kotlin.sdk.BlobResourceContents ->
                                McpContent.Resource(resource.uri, resource.mimeType ?: "", null)
                            else -> null
                        }
                    }
                    else -> null
                }
            }

            Log.d(TAG, "Tool $toolName returned ${content.size} content items")
            Result.success(McpToolCallResult.Success(toolName, content))
        } catch (e: Exception) {
            Log.e(TAG, "Tool call failed: $toolName", e)
            Result.success(
                McpToolCallResult.Error(
                    toolName = toolName,
                    message = e.message ?: "Неизвестная ошибка",
                    isRetryable = isRetryableError(e)
                )
            )
        }
    }

    override suspend fun reconnect(): Result<Unit> = withContext(Dispatchers.IO) {
        val config = currentConfig
        if (config == null) {
            Log.w(TAG, "Cannot reconnect: no previous config")
            return@withContext Result.failure(IllegalStateException("Нет сохранённой конфигурации для переподключения"))
        }

        Log.i(TAG, "Reconnecting to MCP server: ${config.url}")
        disconnect()
        connect(config)
    }

    override suspend fun ping(): Result<Boolean> = withContext(Dispatchers.IO) {
        val client = mcpClient ?: return@withContext Result.success(false)
        try {
            client.ping()
            Result.success(true)
        } catch (e: Exception) {
            Log.w(TAG, "Ping failed", e)
            Result.success(false)
        }
    }

    private fun mapConnectionError(e: Exception): String = when {
        e.message?.contains("timeout", ignoreCase = true) == true ->
            "Время ожидания подключения истекло"
        e.message?.contains("refused", ignoreCase = true) == true ->
            "Сервер отклонил подключение"
        e.message?.contains("host", ignoreCase = true) == true ->
            "Сервер недоступен"
        e.message?.contains("SSL", ignoreCase = true) == true ->
            "Ошибка SSL соединения"
        else -> "Ошибка подключения: ${e.localizedMessage}"
    }

    private fun isRetryableError(e: Exception): Boolean {
        return e.message?.contains("timeout", ignoreCase = true) == true ||
                e.message?.contains("temporary", ignoreCase = true) == true
    }
}
