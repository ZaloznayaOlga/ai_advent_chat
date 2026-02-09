package com.olgaz.aichat.domain.usecase.rag

import com.olgaz.aichat.domain.model.RagRerankConfig
import com.olgaz.aichat.domain.repository.RagRepository
import javax.inject.Inject

class ResetRagRerankConfigUseCase @Inject constructor(
    private val repository: RagRepository
) {
    suspend operator fun invoke(): Result<RagRerankConfig> {
        return repository.resetRerankConfig()
    }
}
