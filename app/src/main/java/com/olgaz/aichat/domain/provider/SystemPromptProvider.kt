package com.olgaz.aichat.domain.provider

import com.olgaz.aichat.domain.model.ChatSettings
import com.olgaz.aichat.domain.model.CommunicationStyle
import com.olgaz.aichat.domain.model.ResponseFormat

interface SystemPromptProvider {
    fun getSystemPrompt(): String
    fun getSystemPrompt(communicationStyle: CommunicationStyle, responseFormat: ResponseFormat): String
    fun getSystemPrompt(settings: ChatSettings): String
}
