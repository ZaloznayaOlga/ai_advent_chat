package com.olgaz.aichat.domain.usecase

import com.olgaz.aichat.domain.model.ChatSettings
import com.olgaz.aichat.domain.model.Message
import com.olgaz.aichat.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SendMessageUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    operator fun invoke(messages: List<Message>): Flow<Result<Message>> {
        return repository.sendMessage(messages)
    }

    operator fun invoke(messages: List<Message>, settings: ChatSettings): Flow<Result<Message>> {
        return repository.sendMessage(messages, settings)
    }
}