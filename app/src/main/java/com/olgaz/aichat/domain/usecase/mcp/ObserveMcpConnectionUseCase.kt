package com.olgaz.aichat.domain.usecase.mcp

import com.olgaz.aichat.domain.model.McpConnectionState
import com.olgaz.aichat.domain.repository.McpRepository
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class ObserveMcpConnectionUseCase @Inject constructor(
    private val repository: McpRepository
) {
    operator fun invoke(): StateFlow<McpConnectionState> {
        return repository.connectionState
    }
}
