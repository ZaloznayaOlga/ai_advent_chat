package com.olgaz.aichat.domain.usecase.mcp

import com.olgaz.aichat.domain.model.McpServerConfig
import com.olgaz.aichat.domain.repository.McpRepository
import javax.inject.Inject

class ConnectMcpUseCase @Inject constructor(
    private val repository: McpRepository
) {
    suspend operator fun invoke(config: McpServerConfig): Result<Unit> {
        return repository.connect(config)
    }
}
