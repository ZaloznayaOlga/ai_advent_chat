package com.olgaz.aichat.domain.usecase.rag

import com.olgaz.aichat.domain.model.RagDocument
import com.olgaz.aichat.domain.repository.RagRepository
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class GetRagDocumentsUseCase @Inject constructor(
    private val repository: RagRepository
) {
    operator fun invoke(): StateFlow<List<RagDocument>> = repository.documents

    suspend fun refresh(): Result<List<RagDocument>> = repository.getDocuments()
}
