package com.olgaz.aichat.domain.repository

import com.olgaz.aichat.domain.model.McpConnectionState
import com.olgaz.aichat.domain.model.McpServerConfig
import com.olgaz.aichat.domain.model.McpTool
import com.olgaz.aichat.domain.model.McpToolCallResult
import kotlinx.coroutines.flow.StateFlow

interface McpRepository {

    /**
     * Текущее состояние подключения (StateFlow для UI)
     */
    val connectionState: StateFlow<McpConnectionState>

    /**
     * Список доступных инструментов (обновляется после подключения)
     */
    val availableTools: StateFlow<List<McpTool>>

    /**
     * Подключиться к MCP серверу
     */
    suspend fun connect(config: McpServerConfig): Result<Unit>

    /**
     * Отключиться от сервера
     */
    suspend fun disconnect()

    /**
     * Обновить список инструментов
     */
    suspend fun refreshTools(): Result<List<McpTool>>

    /**
     * Вызвать инструмент
     */
    suspend fun callTool(
        toolName: String,
        arguments: Map<String, Any?>
    ): Result<McpToolCallResult>

    /**
     * Переподключиться к MCP серверу используя последнюю конфигурацию
     */
    suspend fun reconnect(): Result<Unit>

    /**
     * Проверить доступность сервера
     */
    suspend fun ping(): Result<Boolean>
}
