package com.olgaz.aichat.domain.model

import java.util.UUID

data class Message(
    val id: String = UUID.randomUUID().toString(),
    val content: String,
    val role: MessageRole,
    val timestamp: Long = System.currentTimeMillis()
)

enum class MessageRole {
    USER,
    ASSISTANT,
    SYSTEM
}