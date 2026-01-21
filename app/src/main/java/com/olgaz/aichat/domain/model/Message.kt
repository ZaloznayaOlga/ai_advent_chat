package com.olgaz.aichat.domain.model

import java.util.UUID

data class Message(
    val id: String = UUID.randomUUID().toString(),
    val content: String,
    val role: MessageRole,
    val timestamp: Long = System.currentTimeMillis(),
    val jsonData: MessageJsonData? = null,
    val metadata: MessageMetadata? = null
)

/**
 * Metadata for assistant messages: response time, tokens, cost calculation.
 */
data class MessageMetadata(
    val responseTimeMs: Long,
    val inputTokens: Int,
    val outputTokens: Int,
    val provider: AiProvider
) {
    /**
     * Calculate cost in USD.
     * DeepSeek: INPUT $0.28/1M tokens, OUTPUT $0.42/1M tokens
     * Other providers: free (0.0)
     */
    fun calculateCost(): Double {
        return when (provider) {
            AiProvider.DEEPSEEK -> {
                val inputCost = inputTokens * 0.28 / 1_000_000
                val outputCost = outputTokens * 0.42 / 1_000_000
                inputCost + outputCost
            }
            else -> 0.0
        }
    }
}

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