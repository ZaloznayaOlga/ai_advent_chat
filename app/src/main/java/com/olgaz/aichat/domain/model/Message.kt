package com.olgaz.aichat.domain.model

import java.util.Locale
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
    val provider: AiProvider,
    val model: AiModel? = null
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

/**
 * Token usage statistics for the entire conversation.
 */
data class ConversationTokens(
    val totalInputTokens: Int,
    val totalOutputTokens: Int
) {
    val totalTokens: Int get() = totalInputTokens + totalOutputTokens

    /**
     * Format token count: 1500 -> "1.5K", 150 -> "150"
     */
    fun formatTotal(): String = formatTokenCount(totalTokens)

    companion object {
        fun fromMessages(messages: List<Message>): ConversationTokens {
            var inputTokens = 0
            var outputTokens = 0
            messages.forEach { message ->
                message.metadata?.let {
                    inputTokens += it.inputTokens
                    outputTokens += it.outputTokens
                }
            }
            return ConversationTokens(inputTokens, outputTokens)
        }

        private fun formatTokenCount(count: Int): String {
            return when {
                count >= 1_000_000 -> String.format(Locale.US, "%.1fM", count / 1_000_000.0)
                count >= 1_000 -> String.format(Locale.US, "%.1fK", count / 1_000.0)
                else -> count.toString()
            }
        }
    }
}