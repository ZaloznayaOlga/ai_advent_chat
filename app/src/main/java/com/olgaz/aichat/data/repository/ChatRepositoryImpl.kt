package com.olgaz.aichat.data.repository

import com.olgaz.aichat.data.remote.api.ChatApi
import com.olgaz.aichat.data.remote.dto.AiResponseJsonDto
import com.olgaz.aichat.data.remote.dto.ChatRequestDto
import com.olgaz.aichat.data.remote.dto.MessageDto
import com.olgaz.aichat.di.DeepSeekApi
import com.olgaz.aichat.di.OpenAiApi
import com.olgaz.aichat.domain.model.AiProvider
import com.olgaz.aichat.domain.model.ChatSettings
import com.olgaz.aichat.domain.model.Message
import com.olgaz.aichat.domain.model.MessageJsonData
import com.olgaz.aichat.domain.model.MessageRole
import com.olgaz.aichat.domain.model.ResponseFormat
import com.olgaz.aichat.domain.provider.SystemPromptProvider
import com.olgaz.aichat.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import android.util.Log
import retrofit2.HttpException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject

private const val TAG = "ChatRepository"

class ChatRepositoryImpl @Inject constructor(
    @DeepSeekApi private val deepSeekApi: ChatApi,
    @OpenAiApi private val openAiApi: ChatApi,
    private val systemPromptProvider: SystemPromptProvider
) : ChatRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    override fun sendMessage(messages: List<Message>): Flow<Result<Message>> =
        sendMessage(messages, ChatSettings())

    override fun sendMessage(messages: List<Message>, settings: ChatSettings): Flow<Result<Message>> = flow {
        try {
            val systemPrompt = systemPromptProvider.getSystemPrompt(
                settings.communicationStyle,
                settings.responseFormat
            )

            val systemMessage = MessageDto(
                role = "system",
                content = systemPrompt
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

            val api = selectApi(settings.provider)
            val modelName = getModelName(settings)
            val request = ChatRequestDto(model = modelName, messages = allMessages)
            Log.d(TAG, "modelName: $modelName Base url = ${settings.provider}")
            val response = api.sendMessage(request)

            val rawContent = response.choices.firstOrNull()?.message?.content

            if (rawContent.isNullOrBlank()) {
                emit(Result.failure(ApiException("Пустой ответ от сервера")))
                return@flow
            }

            val assistantMessage = when (settings.responseFormat) {
                ResponseFormat.JSON -> parseJsonResponse(rawContent)
                ResponseFormat.XML -> parseXmlResponse(rawContent)
                ResponseFormat.TEXT -> Message(
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

    private fun selectApi(provider: AiProvider): ChatApi = when (provider) {
        AiProvider.DEEPSEEK -> deepSeekApi
        AiProvider.OPENAI -> openAiApi
    }

    private fun getModelName(settings: ChatSettings): String {
        return if (settings.deepThinking) {
            when (settings.provider) {
                AiProvider.DEEPSEEK -> "deepseek-reasoner"
                AiProvider.OPENAI -> "o1-preview"
            }
        } else {
            settings.model.apiName
        }
    }

    private fun parseJsonResponse(rawContent: String): Message {
        return try {
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
                    rawJson = cleanedJson,
                    responseFormat = ResponseFormat.JSON
                )
            )
        } catch (e: Exception) {
            Log.w(TAG, "JSON parsing failed, showing raw content", e)
            Log.d(TAG, "Raw response: $rawContent")
            Message(
                content = rawContent,
                role = MessageRole.ASSISTANT,
                timestamp = System.currentTimeMillis()
            )
        }
    }

    private fun parseXmlResponse(rawContent: String): Message {
        return try {
            val cleanedXml = cleanXmlResponse(rawContent)
            val answer = extractXmlTag(cleanedXml, "answer") ?: rawContent
            val topic = extractXmlTag(cleanedXml, "topic") ?: ""
            val question = extractXmlTag(cleanedXml, "question") ?: ""
            val datetime = extractXmlTag(cleanedXml, "datetime") ?: ""
            val language = extractXmlTag(cleanedXml, "language") ?: "ru"
            val tags = extractXmlTags(cleanedXml, "tag")
            val links = extractXmlTags(cleanedXml, "link")

            Message(
                content = answer,
                role = MessageRole.ASSISTANT,
                timestamp = System.currentTimeMillis(),
                jsonData = MessageJsonData(
                    datetime = datetime,
                    topic = topic,
                    question = question,
                    answer = answer,
                    tags = tags,
                    links = links,
                    language = language,
                    rawJson = cleanedXml,
                    responseFormat = ResponseFormat.XML
                )
            )
        } catch (e: Exception) {
            Log.w(TAG, "XML parsing failed, showing raw content", e)
            Log.d(TAG, "Raw response: $rawContent")
            Message(
                content = rawContent,
                role = MessageRole.ASSISTANT,
                timestamp = System.currentTimeMillis()
            )
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

    private fun cleanXmlResponse(raw: String): String {
        var cleaned = raw.trim()

        if (cleaned.startsWith("```xml")) {
            cleaned = cleaned.removePrefix("```xml")
        }
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.removePrefix("```")
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.removeSuffix("```")
        }

        return cleaned.trim()
    }

    private fun extractXmlTag(xml: String, tagName: String): String? {
        val regex = Regex("<$tagName>(.*?)</$tagName>", RegexOption.DOT_MATCHES_ALL)
        return regex.find(xml)?.groupValues?.get(1)?.trim()
    }

    private fun extractXmlTags(xml: String, tagName: String): List<String> {
        val regex = Regex("<$tagName>(.*?)</$tagName>", RegexOption.DOT_MATCHES_ALL)
        return regex.findAll(xml).map { it.groupValues[1].trim() }.toList()
    }
}

class ApiException(message: String) : Exception(message)
