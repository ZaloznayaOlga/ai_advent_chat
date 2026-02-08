package com.olgaz.aichat.domain.usecase.rag

import com.olgaz.aichat.domain.repository.RagRepository
import javax.inject.Inject

class UploadRagDocumentUseCase @Inject constructor(
    private val repository: RagRepository
) {
    suspend operator fun invoke(name: String, content: String): Result<Int> {
        return repository.uploadDocument(name, content)
    }
}
