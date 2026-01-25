package com.olgaz.aichat.data.local.converter

import androidx.room.TypeConverter
import com.olgaz.aichat.domain.model.AiModel
import com.olgaz.aichat.domain.model.AiProvider
import com.olgaz.aichat.domain.model.MessageJsonData
import com.olgaz.aichat.domain.model.MessageMetadata
import com.olgaz.aichat.domain.model.ResponseFormat
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Converters {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @TypeConverter
    fun fromMessageJsonData(data: MessageJsonData?): String? {
        return data?.let { json.encodeToString(it.toSerializable()) }
    }

    @TypeConverter
    fun toMessageJsonData(value: String?): MessageJsonData? {
        return value?.let {
            json.decodeFromString<MessageJsonDataSerializable>(it).toDomain()
        }
    }

    @TypeConverter
    fun fromMessageMetadata(data: MessageMetadata?): String? {
        return data?.let { json.encodeToString(it.toSerializable()) }
    }

    @TypeConverter
    fun toMessageMetadata(value: String?): MessageMetadata? {
        return value?.let {
            json.decodeFromString<MessageMetadataSerializable>(it).toDomain()
        }
    }
}

@Serializable
data class MessageJsonDataSerializable(
    val datetime: String,
    val topic: String,
    val question: String,
    val answer: String,
    val tags: List<String>,
    val links: List<String>,
    val language: String,
    val rawJson: String,
    val responseFormat: String
)

@Serializable
data class MessageMetadataSerializable(
    val responseTimeMs: Long,
    val inputTokens: Int,
    val outputTokens: Int,
    val provider: String,
    val model: String?
)

fun MessageJsonData.toSerializable() = MessageJsonDataSerializable(
    datetime = datetime,
    topic = topic,
    question = question,
    answer = answer,
    tags = tags,
    links = links,
    language = language,
    rawJson = rawJson,
    responseFormat = responseFormat.name
)

fun MessageJsonDataSerializable.toDomain() = MessageJsonData(
    datetime = datetime,
    topic = topic,
    question = question,
    answer = answer,
    tags = tags,
    links = links,
    language = language,
    rawJson = rawJson,
    responseFormat = ResponseFormat.valueOf(responseFormat)
)

fun MessageMetadata.toSerializable() = MessageMetadataSerializable(
    responseTimeMs = responseTimeMs,
    inputTokens = inputTokens,
    outputTokens = outputTokens,
    provider = provider.name,
    model = model?.name
)

fun MessageMetadataSerializable.toDomain() = MessageMetadata(
    responseTimeMs = responseTimeMs,
    inputTokens = inputTokens,
    outputTokens = outputTokens,
    provider = AiProvider.valueOf(provider),
    model = model?.let { AiModel.valueOf(it) }
)
