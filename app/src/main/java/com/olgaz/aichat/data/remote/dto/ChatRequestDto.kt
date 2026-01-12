package com.olgaz.aichat.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChatRequestDto(
    @SerialName("model")
    val model: String = "deepseek-chat",
    @SerialName("messages")
    val messages: List<MessageDto>,
    @SerialName("stream")
    val stream: Boolean = false
)

@Serializable
data class MessageDto(
    @SerialName("role")
    val role: String,
    @SerialName("content")
    val content: String
)