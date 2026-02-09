package com.olgaz.aichat.domain.usecase.rag

import com.olgaz.aichat.domain.model.RagRerankConfig
import com.olgaz.aichat.domain.repository.RagRepository
import javax.inject.Inject

class UpdateRagRerankConfigUseCase @Inject constructor(
    private val repository: RagRepository
) {
    suspend operator fun invoke(config: RagRerankConfig): Result<RagRerankConfig> {
        return repository.updateRerankConfig(config)
    }
}
