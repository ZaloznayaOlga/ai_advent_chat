package com.olgaz.aichat.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChatResponseDto(
    val id: String,
    val `object`: String,
    val created: Long,
    val model: String,
    val choices: List<ChoiceDto>,
    val usage: UsageDto? = null
)

@Serializable
data class ChoiceDto(
    val index: Int,
    val message: MessageDto,
    @SerialName("finish_reason")
    val finishReason: String? = null
)

@Serializable
data class UsageDto(
    @SerialName("prompt_tokens")
    val promptTokens: Int,
    @SerialName("completion_tokens")
    val completionTokens: Int,
    @SerialName("total_tokens")
    val totalTokens: Int
)