package com.olgaz.aichat.domain.repository

import com.olgaz.aichat.domain.model.Message
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun sendMessage(messages: List<Message>): Flow<Result<Message>>
}