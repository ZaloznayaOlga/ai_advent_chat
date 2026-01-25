package com.olgaz.aichat.data.local.mapper

import com.olgaz.aichat.data.local.converter.MessageJsonDataSerializable
import com.olgaz.aichat.data.local.converter.MessageMetadataSerializable
import com.olgaz.aichat.data.local.converter.toDomain
import com.olgaz.aichat.data.local.converter.toSerializable
import com.olgaz.aichat.data.local.entity.FileAttachmentEmbedded
import com.olgaz.aichat.data.local.entity.MessageEntity
import com.olgaz.aichat.data.local.entity.SummarizationInfoEmbedded
import com.olgaz.aichat.domain.model.FileAttachment
import com.olgaz.aichat.domain.model.Message
import com.olgaz.aichat.domain.model.MessageRole
import com.olgaz.aichat.domain.model.SummarizationInfo
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object MessageMapper {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun toEntity(message: Message): MessageEntity {
        return MessageEntity(
            id = message.id,
            content = message.content,
            role = message.role.name,
            timestamp = message.timestamp,
            displayContent = message.displayContent,
            jsonData = message.jsonData?.let { json.encodeToString(it.toSerializable()) },
            metadata = message.metadata?.let { json.encodeToString(it.toSerializable()) },
            attachedFile = message.attachedFile?.let {
                FileAttachmentEmbedded(it.fileName, it.characterCount)
            },
            summarizationInfo = message.summarizationInfo?.let {
                SummarizationInfoEmbedded(
                    summarizedMessageCount = it.summarizedMessageCount,
                    summarizedInputTokens = it.summarizedInputTokens,
                    summarizedOutputTokens = it.summarizedOutputTokens
                )
            },
            isSummary = message.summarizationInfo != null
        )
    }

    fun toDomain(entity: MessageEntity): Message {
        return Message(
            id = entity.id,
            content = entity.content,
            role = MessageRole.valueOf(entity.role),
            timestamp = entity.timestamp,
            displayContent = entity.displayContent,
            jsonData = entity.jsonData?.let {
                json.decodeFromString<MessageJsonDataSerializable>(it).toDomain()
            },
            metadata = entity.metadata?.let {
                json.decodeFromString<MessageMetadataSerializable>(it).toDomain()
            },
            attachedFile = entity.attachedFile?.let {
                FileAttachment(it.fileName, it.characterCount)
            },
            summarizationInfo = entity.summarizationInfo?.let {
                SummarizationInfo(
                    summarizedMessageCount = it.summarizedMessageCount,
                    summarizedInputTokens = it.summarizedInputTokens,
                    summarizedOutputTokens = it.summarizedOutputTokens
                )
            }
        )
    }
}
