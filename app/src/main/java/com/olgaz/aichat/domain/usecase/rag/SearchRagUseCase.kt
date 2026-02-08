package com.olgaz.aichat.domain.usecase.rag

import com.olgaz.aichat.domain.model.RagSearchResponse
import com.olgaz.aichat.domain.repository.RagRepository
import javax.inject.Inject

class SearchRagUseCase @Inject constructor(
    private val repository: RagRepository
) {
    suspend operator fun invoke(query: String, topK: Int = 5): Result<RagSearchResponse> {
        return repository.search(query, topK)
    }
}
