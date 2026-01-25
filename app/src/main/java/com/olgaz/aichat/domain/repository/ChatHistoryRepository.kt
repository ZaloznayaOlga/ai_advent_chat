package com.olgaz.aichat.domain.repository

import com.olgaz.aichat.domain.model.ChatSettings
import com.olgaz.aichat.domain.model.Message
import kotlinx.coroutines.flow.Flow

interface ChatHistoryRepository {

    fun getAllMessages(): Flow<List<Message>>

    suspend fun getAllMessagesOnce(): List<Message>

    suspend fun getLastMessage(): Message?

    suspend fun saveMessage(message: Message)

    suspend fun saveMessages(messages: List<Message>)

    suspend fun getLastSummary(): Message?

    suspend fun getMessagesForApiContext(): List<Message>

    suspend fun saveSummaryAndMarkCovered(summary: Message, messageIds: List<String>)

    suspend fun hasUnansweredUserMessage(): Boolean

    suspend fun clearAllHistory()

    suspend fun getSettings(): ChatSettings?

    suspend fun saveSettings(settings: ChatSettings)
}
