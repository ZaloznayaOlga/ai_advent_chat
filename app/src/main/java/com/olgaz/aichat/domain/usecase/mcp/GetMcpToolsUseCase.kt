package com.olgaz.aichat.domain.usecase.mcp

import com.olgaz.aichat.domain.model.McpTool
import com.olgaz.aichat.domain.repository.McpRepository
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class GetMcpToolsUseCase @Inject constructor(
    private val repository: McpRepository
) {
    operator fun invoke(): StateFlow<List<McpTool>> {
        return repository.availableTools
    }
}
