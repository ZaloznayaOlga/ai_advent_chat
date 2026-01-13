package com.olgaz.aichat.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Data class for parsing structured JSON responses from DeepSeek API.
 * The AI is instructed via system prompt to respond in this exact format.
 */
@Serializable
data class AiResponseJsonDto(
    @SerialName("datetime")
    val datetime: String,

    @SerialName("topic")
    val topic: String,

    @SerialName("question")
    val question: String,

    @SerialName("answer")
    val answer: String,

    @SerialName("tags")
    val tags: List<String>,

    @SerialName("links")
    val links: List<String>,

    @SerialName("language")
    val language: String
)
