package com.olgaz.aichat.data.local.mapper

import com.olgaz.aichat.data.local.entity.SettingsEntity
import com.olgaz.aichat.domain.model.AiModel
import com.olgaz.aichat.domain.model.AiProvider
import com.olgaz.aichat.domain.model.ChatSettings
import com.olgaz.aichat.domain.model.CommunicationStyle
import com.olgaz.aichat.domain.model.ResponseFormat
import com.olgaz.aichat.domain.model.SendMessageMode
import com.olgaz.aichat.domain.model.SummarizationSettings
import com.olgaz.aichat.domain.model.SystemPromptMode

object SettingsMapper {

    fun toEntity(settings: ChatSettings): SettingsEntity {
        return SettingsEntity(
            id = 1,
            provider = settings.provider.name,
            model = settings.model.name,
            communicationStyle = settings.communicationStyle.name,
            deepThinking = settings.deepThinking,
            responseFormat = settings.responseFormat.name,
            sendMessageMode = settings.sendMessageMode.name,
            systemPromptMode = settings.systemPromptMode.name,
            customSystemPrompt = settings.customSystemPrompt,
            temperature = settings.temperature,
            summarizationEnabled = settings.summarization.enabled,
            summarizationMessageThreshold = settings.summarization.messageThreshold,
            summarizationTokenThreshold = settings.summarization.tokenThreshold,
            mcpEnabled = settings.mcpWeatherEnabled,
            mcpWeatherEnabled = settings.mcpWeatherEnabled,
            mcpReminderEnabled = settings.mcpReminderEnabled,
            mcpServerUrl = settings.mcpServerUrl
        )
    }

    fun toDomain(entity: SettingsEntity): ChatSettings {
        val provider = try {
            AiProvider.valueOf(entity.provider)
        } catch (e: Exception) {
            AiProvider.DEEPSEEK
        }

        val model = try {
            AiModel.valueOf(entity.model)
        } catch (e: Exception) {
            AiModel.defaultForProvider(provider)
        }

        val communicationStyle = try {
            CommunicationStyle.valueOf(entity.communicationStyle)
        } catch (e: Exception) {
            CommunicationStyle.GENERAL
        }

        val responseFormat = try {
            ResponseFormat.valueOf(entity.responseFormat)
        } catch (e: Exception) {
            ResponseFormat.TEXT
        }

        val sendMessageMode = try {
            SendMessageMode.valueOf(entity.sendMessageMode)
        } catch (e: Exception) {
            SendMessageMode.ENTER
        }

        val systemPromptMode = try {
            SystemPromptMode.valueOf(entity.systemPromptMode)
        } catch (e: Exception) {
            SystemPromptMode.DEFAULT
        }

        return ChatSettings(
            provider = provider,
            model = model,
            communicationStyle = communicationStyle,
            deepThinking = entity.deepThinking,
            responseFormat = responseFormat,
            sendMessageMode = sendMessageMode,
            systemPromptMode = systemPromptMode,
            customSystemPrompt = entity.customSystemPrompt,
            temperature = entity.temperature,
            summarization = SummarizationSettings(
                enabled = entity.summarizationEnabled,
                messageThreshold = entity.summarizationMessageThreshold,
                tokenThreshold = entity.summarizationTokenThreshold
            ),
            mcpWeatherEnabled = entity.mcpWeatherEnabled,
            mcpReminderEnabled = entity.mcpReminderEnabled,
            mcpServerUrl = entity.mcpServerUrl
        )
    }
}
