package com.olgaz.aichat.data.repository

import com.olgaz.aichat.data.remote.api.DeepSeekApi
import com.olgaz.aichat.data.remote.dto.AiResponseJsonDto
import com.olgaz.aichat.data.remote.dto.ChatRequestDto
import com.olgaz.aichat.data.remote.dto.MessageDto
import com.olgaz.aichat.domain.model.Message
import com.olgaz.aichat.domain.model.MessageJsonData
import com.olgaz.aichat.domain.model.MessageRole
import com.olgaz.aichat.domain.provider.SystemPromptProvider
import com.olgaz.aichat.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject

class ChatRepositoryImpl @Inject constructor(
    private val api: DeepSeekApi,
    private val systemPromptProvider: SystemPromptProvider
) : ChatRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    override fun sendMessage(messages: List<Message>): Flow<Result<Message>> = flow {
        try {
            val systemMessage = MessageDto(
                role = "system",
                content = systemPromptProvider.getSystemPrompt()
            )

            val userMessages = messages.map { message ->
                MessageDto(
                    role = when (message.role) {
                        MessageRole.USER -> "user"
                        MessageRole.ASSISTANT -> "assistant"
                        MessageRole.SYSTEM -> "system"
                    },
                    content = message.content
                )
            }

            val allMessages = listOf(systemMessage) + userMessages

            val request = ChatRequestDto(messages = allMessages)
            val response = api.sendMessage(request)

            val rawContent = response.choices.firstOrNull()?.message?.content

            if (rawContent.isNullOrBlank()) {
                emit(Result.failure(ApiException("Пустой ответ от сервера")))
                return@flow
            }

            val assistantMessage = try {
                val cleanedJson = cleanJsonResponse(rawContent)
                val aiResponse = json.decodeFromString<AiResponseJsonDto>(cleanedJson)

                Message(
                    content = aiResponse.answer,
                    role = MessageRole.ASSISTANT,
                    timestamp = System.currentTimeMillis(),
                    jsonData = MessageJsonData(
                        datetime = aiResponse.datetime,
                        topic = aiResponse.topic,
                        question = aiResponse.question,
                        answer = aiResponse.answer,
                        tags = aiResponse.tags,
                        links = aiResponse.links,
                        language = aiResponse.language,
                        rawJson = cleanedJson
                    )
                )
            } catch (e: Exception) {
                Message(
                    content = rawContent,
                    role = MessageRole.ASSISTANT,
                    timestamp = System.currentTimeMillis()
                )
            }

            emit(Result.success(assistantMessage))
        } catch (e: HttpException) {
            val errorMessage = when (e.code()) {
                400 -> "Неверный формат запроса."
                401 -> "Ошибка доступа к API. Проверьте корректность API ключа!"
                403 -> "Доступ запрещён. Проверьте права доступа API ключа."
                429 -> "Слишком много запросов. Подождите немного и попробуйте снова."
                500, 502, 503 -> "Сервер временно недоступен. Попробуйте позже."
                else -> "Ошибка сервера: ${e.code()}"
            }
            emit(Result.failure(ApiException(errorMessage)))
        } catch (e: UnknownHostException) {
            emit(Result.failure(ApiException("Нет подключения к интернету")))
        } catch (e: SocketTimeoutException) {
            emit(Result.failure(ApiException("Превышено время ожидания ответа. Попробуйте снова.")))
        } catch (e: Exception) {
            emit(Result.failure(ApiException("Произошла ошибка: ${e.localizedMessage}")))
        }
    }

    private fun cleanJsonResponse(raw: String): String {
        var cleaned = raw.trim()

        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.removePrefix("```json")
        }
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.removePrefix("```")
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.removeSuffix("```")
        }

        return cleaned.trim()
    }
}

class ApiException(message: String) : Exception(message)