package com.olgaz.aichat.domain.usecase.mcp

import com.olgaz.aichat.domain.model.McpToolCallResult
import com.olgaz.aichat.domain.repository.McpRepository
import javax.inject.Inject

class CallMcpToolUseCase @Inject constructor(
    private val repository: McpRepository
) {
    suspend operator fun invoke(
        toolName: String,
        arguments: Map<String, Any?>
    ): Result<McpToolCallResult> {
        return repository.callTool(toolName, arguments)
    }
}
