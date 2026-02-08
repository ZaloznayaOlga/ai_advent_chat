package com.olgaz.aichat.domain.usecase.rag

import com.olgaz.aichat.domain.model.RagConnectionState
import com.olgaz.aichat.domain.repository.RagRepository
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class ObserveRagConnectionUseCase @Inject constructor(
    private val repository: RagRepository
) {
    operator fun invoke(): StateFlow<RagConnectionState> = repository.connectionState
}
