package com.olgaz.aichat.domain.repository

import com.olgaz.aichat.domain.model.McpTool
import com.olgaz.aichat.domain.model.McpToolCallResult

/**
 * Интерфейс для локальных инструментов (обрабатываются внутри приложения, без внешнего MCP сервера)
 */
interface LocalToolHandler {
    fun getTools(): List<McpTool>
    fun canHandle(toolName: String): Boolean
    suspend fun handleToolCall(toolName: String, arguments: Map<String, Any?>): McpToolCallResult
}
