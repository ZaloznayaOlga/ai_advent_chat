package com.olgaz.aichat.domain.usecase.mcp

import com.olgaz.aichat.domain.repository.McpRepository
import javax.inject.Inject

class DisconnectMcpUseCase @Inject constructor(
    private val repository: McpRepository
) {
    suspend operator fun invoke() {
        repository.disconnect()
    }
}
