package com.olgaz.aichat.domain.repository

import com.olgaz.aichat.domain.model.ChatSettings
import com.olgaz.aichat.domain.model.McpTool
import com.olgaz.aichat.domain.model.Message
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun sendMessage(messages: List<Message>): Flow<Result<Message>>
    fun sendMessage(messages: List<Message>, settings: ChatSettings): Flow<Result<Message>>
    fun sendMessage(
        messages: List<Message>,
        settings: ChatSettings,
        mcpTools: List<McpTool>
    ): Flow<Result<Message>>
    fun summarizeConversation(messages: List<Message>, settings: ChatSettings): Flow<Result<Message>>
}