package com.olgaz.aichat.domain.model

import java.util.UUID

data class Message(
    val id: String = UUID.randomUUID().toString(),
    val content: String,
    val role: MessageRole,
    val timestamp: Long = System.currentTimeMillis(),
    val jsonData: MessageJsonData? = null
)

/**
 * Structured data parsed from AI response (JSON or XML).
 * Only populated for ASSISTANT messages.
 */
data class MessageJsonData(
    val datetime: String,
    val topic: String,
    val question: String,
    val answer: String,
    val tags: List<String>,
    val links: List<String>,
    val language: String,
    val rawJson: String,
    val responseFormat: ResponseFormat = ResponseFormat.JSON
)

enum class MessageRole {
    USER,
    ASSISTANT,
    SYSTEM
}