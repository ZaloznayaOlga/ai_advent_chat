package com.olgaz.aichat.data.repository

import com.olgaz.aichat.data.local.dao.MessageDao
import com.olgaz.aichat.data.local.dao.SettingsDao
import com.olgaz.aichat.data.local.mapper.MessageMapper
import com.olgaz.aichat.data.local.mapper.SettingsMapper
import com.olgaz.aichat.domain.model.ChatSettings
import com.olgaz.aichat.domain.model.Message
import com.olgaz.aichat.domain.model.MessageRole
import com.olgaz.aichat.domain.repository.ChatHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ChatHistoryRepositoryImpl @Inject constructor(
    private val messageDao: MessageDao,
    private val settingsDao: SettingsDao
) : ChatHistoryRepository {

    override fun getAllMessages(): Flow<List<Message>> {
        return messageDao.getAllMessages().map { entities ->
            entities.map { MessageMapper.toDomain(it) }
        }
    }

    override suspend fun getAllMessagesOnce(): List<Message> {
        return messageDao.getAllMessagesOnce().map { MessageMapper.toDomain(it) }
    }

    override suspend fun getLastMessage(): Message? {
        return messageDao.getLastMessage()?.let { MessageMapper.toDomain(it) }
    }

    override suspend fun saveMessage(message: Message) {
        messageDao.insertMessage(MessageMapper.toEntity(message))
    }

    override suspend fun saveMessages(messages: List<Message>) {
        val entities = messages.map { MessageMapper.toEntity(it) }
        messageDao.insertMessages(entities)
    }

    override suspend fun getLastSummary(): Message? {
        return messageDao.getLastSummary()?.let { MessageMapper.toDomain(it) }
    }

    override suspend fun getMessagesForApiContext(): List<Message> {
        val lastSummary = getLastSummary()
        val messagesAfterSummary = messageDao.getMessagesAfterLastSummary()
            .map { MessageMapper.toDomain(it) }

        return if (lastSummary != null) {
            listOf(lastSummary) + messagesAfterSummary
        } else {
            messagesAfterSummary
        }
    }

    override suspend fun saveSummaryAndMarkCovered(summary: Message, messageIds: List<String>) {
        messageDao.insertMessage(MessageMapper.toEntity(summary))
        if (messageIds.isNotEmpty()) {
            messageDao.markMessagesCoveredBySummary(summary.id, messageIds)
        }
    }

    override suspend fun hasUnansweredUserMessage(): Boolean {
        val lastMessage = messageDao.getLastMessage() ?: return false
        return lastMessage.role == MessageRole.USER.name
    }

    override suspend fun clearAllHistory() {
        messageDao.deleteAllMessages()
    }

    override suspend fun getSettings(): ChatSettings? {
        return settingsDao.getSettings()?.let { SettingsMapper.toDomain(it) }
    }

    override suspend fun saveSettings(settings: ChatSettings) {
        settingsDao.saveSettings(SettingsMapper.toEntity(settings))
    }
}
