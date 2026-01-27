package com.olgaz.aichat.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class ChatRequestDto(
    @SerialName("model")
    val model: String = "deepseek-chat",
    @SerialName("messages")
    val messages: List<MessageDto>,
    @SerialName("stream")
    val stream: Boolean = false,
    @SerialName("temperature")
    val temperature: Float? = null,
    @SerialName("tools")
    val tools: List<ToolDto>? = null,
    @SerialName("tool_choice")
    val toolChoice: String? = null
)

@Serializable
data class MessageDto(
    @SerialName("role")
    val role: String,
    @SerialName("content")
    val content: String? = null,
    @SerialName("tool_calls")
    val toolCalls: List<ToolCallDto>? = null,
    @SerialName("tool_call_id")
    val toolCallId: String? = null
)

/**
 * Определение инструмента для AI
 */
@Serializable
data class ToolDto(
    val type: String = "function",
    val function: FunctionDto
)

/**
 * Описание функции инструмента
 */
@Serializable
data class FunctionDto(
    val name: String,
    val description: String? = null,
    val parameters: JsonElement
)

/**
 * Вызов инструмента от AI
 */
@Serializable
data class ToolCallDto(
    val id: String,
    val type: String = "function",
    val function: ToolCallFunctionDto
)

/**
 * Данные вызова функции
 */
@Serializable
data class ToolCallFunctionDto(
    val name: String,
    val arguments: String
)