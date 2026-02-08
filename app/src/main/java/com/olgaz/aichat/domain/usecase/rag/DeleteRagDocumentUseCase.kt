package com.olgaz.aichat.domain.usecase.rag

import com.olgaz.aichat.domain.repository.RagRepository
import javax.inject.Inject

class DeleteRagDocumentUseCase @Inject constructor(
    private val repository: RagRepository
) {
    suspend operator fun invoke(name: String): Result<Int> {
        return repository.deleteDocument(name)
    }
}
