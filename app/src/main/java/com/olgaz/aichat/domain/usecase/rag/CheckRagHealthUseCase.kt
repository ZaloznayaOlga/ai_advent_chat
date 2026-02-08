package com.olgaz.aichat.domain.usecase.rag

import com.olgaz.aichat.domain.repository.RagRepository
import javax.inject.Inject

class CheckRagHealthUseCase @Inject constructor(
    private val repository: RagRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        return repository.checkHealth()
    }
}
