package com.olgaz.aichat.data.repository

import com.olgaz.aichat.data.remote.api.ChatApi
import com.olgaz.aichat.data.remote.dto.AiResponseJsonDto
import com.olgaz.aichat.data.remote.dto.ChatRequestDto
import com.olgaz.aichat.data.remote.dto.FunctionDto
import com.olgaz.aichat.data.remote.dto.MessageDto
import com.olgaz.aichat.data.remote.dto.ToolDto
import com.olgaz.aichat.di.DeepSeekApi
import com.olgaz.aichat.di.HuggingFaceApi
import com.olgaz.aichat.di.OpenAiApi
import com.olgaz.aichat.domain.model.AiModel
import com.olgaz.aichat.domain.model.AiProvider
import com.olgaz.aichat.domain.model.ChatSettings
import com.olgaz.aichat.domain.model.McpContent
import com.olgaz.aichat.domain.model.McpTool
import com.olgaz.aichat.domain.model.McpToolCallResult
import com.olgaz.aichat.domain.model.Message
import com.olgaz.aichat.domain.model.MessageJsonData
import com.olgaz.aichat.domain.model.MessageMetadata
import com.olgaz.aichat.domain.model.MessageRole
import com.olgaz.aichat.domain.model.ResponseFormat
import com.olgaz.aichat.domain.provider.SystemPromptProvider
import com.olgaz.aichat.domain.repository.ChatRepository
import com.olgaz.aichat.domain.repository.McpRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import android.util.Log
import retrofit2.HttpException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject

private const val TAG = "ChatRepository"

class ChatRepositoryImpl @Inject constructor(
    @DeepSeekApi private val deepSeekApi: ChatApi,
    @OpenAiApi private val openAiApi: ChatApi,
    @HuggingFaceApi private val huggingFaceApi: ChatApi,
    private val systemPromptProvider: SystemPromptProvider,
    private val mcpRepository: McpRepository
) : ChatRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    override fun sendMessage(messages: List<Message>): Flow<Result<Message>> =
        sendMessage(messages, ChatSettings())

    override fun sendMessage(messages: List<Message>, settings: ChatSettings): Flow<Result<Message>> = flow {
        try {
            val systemPrompt = systemPromptProvider.getSystemPrompt(settings)

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
            val request = ChatRequestDto(
                model = modelName,
                messages = allMessages,
                temperature = settings.temperature
            )

            val startTime = System.currentTimeMillis()
            val response = api.sendMessage(request)
            val responseTimeMs = System.currentTimeMillis() - startTime

            val rawContent = response.choices.firstOrNull()?.message?.content

            if (rawContent.isNullOrBlank()) {
                emit(Result.failure(ApiException("Пустой ответ от сервера")))
                return@flow
            }

            val actualModel = getActualModel(settings)
            val metadata = MessageMetadata(
                responseTimeMs = responseTimeMs,
                inputTokens = response.usage?.promptTokens ?: 0,
                outputTokens = response.usage?.completionTokens ?: 0,
                provider = settings.provider,
                model = actualModel
            )

            val assistantMessage = when (settings.responseFormat) {
                ResponseFormat.JSON -> parseJsonResponse(rawContent, metadata)
                ResponseFormat.XML -> parseXmlResponse(rawContent, metadata)
                ResponseFormat.TEXT -> Message(
                    content = rawContent,
                    role = MessageRole.ASSISTANT,
                    timestamp = System.currentTimeMillis(),
                    metadata = metadata
                )
            }

            emit(Result.success(assistantMessage))
        } catch (e: HttpException) {
            val errorBody = try {
                e.response()?.errorBody()?.string()
            } catch (_: Exception) {
                null
            }

            val baseMessage = when (e.code()) {
                400 -> "Неверный формат запроса."
                401 -> "Ошибка доступа к API. Проверьте корректность API ключа!"
                403 -> "Доступ запрещён. Проверьте права доступа API ключа."
                429 -> "Слишком много запросов. Подождите немного и попробуйте снова."
                500, 502, 503 -> "Сервер временно недоступен. Попробуйте позже."
                else -> "Ошибка сервера: ${e.code()}"
            }

            val errorMessage = if (!errorBody.isNullOrBlank()) {
                "$baseMessage\n\nОтвет сервера: $errorBody"
            } else {
                baseMessage
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

    override fun sendMessage(
        messages: List<Message>,
        settings: ChatSettings,
        mcpTools: List<McpTool>
    ): Flow<Result<Message>> = flow {
        // Если нет MCP tools, используем обычную отправку
        if (mcpTools.isEmpty()) {
            sendMessage(messages, settings).collect { emit(it) }
            return@flow
        }

        try {
            val systemPrompt = systemPromptProvider.getSystemPrompt(settings)
            val systemMessage = MessageDto(role = "system", content = systemPrompt)

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

            // Конвертируем MCP tools в формат API
            val toolDtos = mcpTools.map { it.toToolDto() }

            val api = selectApi(settings.provider)
            val modelName = getModelName(settings)
            val startTime = System.currentTimeMillis()

            // Список сообщений для API (будет расти с tool results)
            val allMessages = mutableListOf(systemMessage)
            allMessages.addAll(userMessages)

            // Список использованных tools
            val usedToolNames = mutableListOf<String>()

            // Цикл tool calling (максимум 5 итераций для безопасности)
            var iterations = 0
            val maxIterations = 5

            while (iterations < maxIterations) {
                iterations++

                val request = ChatRequestDto(
                    model = modelName,
                    messages = allMessages.toList(),
                    temperature = settings.temperature,
                    tools = toolDtos,
                    toolChoice = "auto"
                )

                Log.d(TAG, "Sending request with ${toolDtos.size} tools (iteration $iterations)")
                val response = api.sendMessage(request)
                val choice = response.choices.firstOrNull()
                val assistantMsg = choice?.message

                if (assistantMsg == null) {
                    emit(Result.failure(ApiException("Пустой ответ от сервера")))
                    return@flow
                }

                // Проверяем есть ли tool_calls
                val toolCalls = assistantMsg.toolCalls
                if (toolCalls.isNullOrEmpty()) {
                    // Нет tool calls - это финальный ответ
                    val rawContent = assistantMsg.content
                    if (rawContent.isNullOrBlank()) {
                        emit(Result.failure(ApiException("Пустой ответ от сервера")))
                        return@flow
                    }

                    val responseTimeMs = System.currentTimeMillis() - startTime
                    val actualModel = getActualModel(settings)
                    val metadata = MessageMetadata(
                        responseTimeMs = responseTimeMs,
                        inputTokens = response.usage?.promptTokens ?: 0,
                        outputTokens = response.usage?.completionTokens ?: 0,
                        provider = settings.provider,
                        model = actualModel
                    )

                    val assistantMessage = when (settings.responseFormat) {
                        ResponseFormat.JSON -> parseJsonResponse(rawContent, metadata, usedToolNames)
                        ResponseFormat.XML -> parseXmlResponse(rawContent, metadata, usedToolNames)
                        ResponseFormat.TEXT -> Message(
                            content = rawContent,
                            role = MessageRole.ASSISTANT,
                            timestamp = System.currentTimeMillis(),
                            metadata = metadata,
                            usedTools = usedToolNames.toList()
                        )
                    }

                    emit(Result.success(assistantMessage))
                    return@flow
                }

                // Есть tool calls - добавляем assistant message и вызываем tools
                allMessages.add(
                    MessageDto(
                        role = "assistant",
                        content = assistantMsg.content,
                        toolCalls = toolCalls
                    )
                )

                // Вызываем каждый tool
                for (toolCall in toolCalls) {
                    val toolName = toolCall.function.name
                    val argsJson = toolCall.function.arguments

                    Log.d(TAG, "Calling MCP tool: $toolName with args: $argsJson")
                    usedToolNames.add(toolName)

                    // Парсим аргументы
                    val arguments = try {
                        val parsed = json.parseToJsonElement(argsJson)
                        if (parsed is JsonObject) {
                            parsed.entries.associate { (k, v) ->
                                k to when (v) {
                                    is JsonPrimitive -> when {
                                        v.isString -> v.content
                                        else -> v.content
                                    }
                                    else -> v.toString()
                                }
                            }
                        } else {
                            emptyMap()
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to parse tool arguments", e)
                        emptyMap<String, Any?>()
                    }

                    // Вызываем MCP tool
                    val toolResult = mcpRepository.callTool(toolName, arguments)
                    val toolContent = when (val result = toolResult.getOrNull()) {
                        is McpToolCallResult.Success -> {
                            result.content.joinToString("\n") { content ->
                                when (content) {
                                    is McpContent.Text -> content.text
                                    is McpContent.Image -> "[Image: ${content.mimeType}]"
                                    is McpContent.Resource -> content.text ?: "[Resource: ${content.uri}]"
                                }
                            }
                        }
                        is McpToolCallResult.Error -> {
                            "Error: ${result.message}"
                        }
                        null -> {
                            val error = toolResult.exceptionOrNull()
                            "Error calling tool: ${error?.message ?: "Unknown error"}"
                        }
                    }

                    // Добавляем результат tool в сообщения
                    allMessages.add(
                        MessageDto(
                            role = "tool",
                            content = toolContent,
                            toolCallId = toolCall.id
                        )
                    )

                    Log.d(TAG, "Tool $toolName result: ${toolContent.take(100)}...")
                }
            }

            // Достигли максимума итераций
            emit(Result.failure(ApiException("Превышено максимальное количество вызовов инструментов")))

        } catch (e: HttpException) {
            val errorBody = try {
                e.response()?.errorBody()?.string()
            } catch (_: Exception) {
                null
            }

            val baseMessage = when (e.code()) {
                400 -> "Неверный формат запроса."
                401 -> "Ошибка доступа к API. Проверьте корректность API ключа!"
                403 -> "Доступ запрещён. Проверьте права доступа API ключа."
                429 -> "Слишком много запросов. Подождите немного и попробуйте снова."
                500, 502, 503 -> "Сервер временно недоступен. Попробуйте позже."
                else -> "Ошибка сервера: ${e.code()}"
            }

            val errorMessage = if (!errorBody.isNullOrBlank()) {
                "$baseMessage\n\nОтвет сервера: $errorBody"
            } else {
                baseMessage
            }
            emit(Result.failure(ApiException(errorMessage)))
        } catch (e: UnknownHostException) {
            emit(Result.failure(ApiException("Нет подключения к интернету")))
        } catch (e: SocketTimeoutException) {
            emit(Result.failure(ApiException("Превышено время ожидания ответа. Попробуйте снова.")))
        } catch (e: Exception) {
            Log.e(TAG, "Error in sendMessage with tools", e)
            emit(Result.failure(ApiException("Произошла ошибка: ${e.localizedMessage}")))
        }
    }

    /**
     * Конвертирует McpTool в ToolDto для API
     */
    private fun McpTool.toToolDto(): ToolDto {
        val parameters = buildJsonObject {
            put("type", inputSchema.type)
            if (inputSchema.properties.isNotEmpty()) {
                putJsonObject("properties") {
                    inputSchema.properties.forEach { (propName, propSchema) ->
                        putJsonObject(propName) {
                            put("type", propSchema.type)
                            propSchema.description?.let { put("description", it) }
                            propSchema.enum?.let { enumList ->
                                putJsonArray("enum") {
                                    enumList.forEach { add(JsonPrimitive(it)) }
                                }
                            }
                        }
                    }
                }
            }
            if (inputSchema.required.isNotEmpty()) {
                putJsonArray("required") {
                    inputSchema.required.forEach { add(JsonPrimitive(it)) }
                }
            }
        }

        return ToolDto(
            type = "function",
            function = FunctionDto(
                name = name,
                description = description,
                parameters = parameters
            )
        )
    }

    override fun summarizeConversation(
        messages: List<Message>,
        settings: ChatSettings
    ): Flow<Result<Message>> = flow {
        try {
            val summarizationPrompt = """
Ты — помощник, который создает краткие, информативные резюме диалогов. Проанализируй предоставленную историю сообщений и создай сводку, которая захватит:

1. **Контекст и цель:** С чего начался разговор и какова была изначальная цель пользователя?
2. **Ключевые моменты и решения:** Какие основные вопросы были подняты, какие решения приняты или информация получена?
3. **Текущий статус:** На чем диалог остановился? Какие открытые вопросы, следующие шаги или запланированные действия остались?
4. **Тон и особенности:** Были ли особые эмоции, срочность или важные детали (например, имена, даты, числа), которые нужно сохранить?

Создай сводку в виде короткого структурированного текста (до 7 предложений), понятного для ИИ-агента, который продолжит диалог. Будь точен и используй ключевые слова.
Предоставь в виде:

Тема:
Ключевые моменты:

Текст сводки сообщений.
""".trimIndent()

            val conversationContext = messages
                .filter { it.role == MessageRole.USER || it.role == MessageRole.ASSISTANT }
                .filter { it.summarizationInfo == null }
                .joinToString("\n\n") { msg ->
                    val role = if (msg.role == MessageRole.USER) "Пользователь" else "Ассистент"
                    val content = msg.displayContent.ifEmpty { msg.content }
                    "$role: $content"
                }

            val allMessages = listOf(
                MessageDto(role = "system", content = summarizationPrompt),
                MessageDto(role = "user", content = "Проанализируй и суммаризируй следующий диалог:\n\n$conversationContext")
            )

            val api = selectApi(settings.provider)
            val modelName = getModelName(settings)
            val request = ChatRequestDto(
                model = modelName,
                messages = allMessages,
                temperature = 0.3f
            )

            val startTime = System.currentTimeMillis()
            val response = api.sendMessage(request)
            val responseTimeMs = System.currentTimeMillis() - startTime

            val rawContent = response.choices.firstOrNull()?.message?.content

            if (rawContent.isNullOrBlank()) {
                emit(Result.failure(ApiException("Пустой ответ при суммаризации")))
                return@flow
            }

            val actualModel = getActualModel(settings)
            val metadata = MessageMetadata(
                responseTimeMs = responseTimeMs,
                inputTokens = response.usage?.promptTokens ?: 0,
                outputTokens = response.usage?.completionTokens ?: 0,
                provider = settings.provider,
                model = actualModel
            )

            emit(Result.success(Message(
                content = rawContent,
                role = MessageRole.ASSISTANT,
                metadata = metadata
            )))
        } catch (e: HttpException) {
            val errorBody = try {
                e.response()?.errorBody()?.string()
            } catch (_: Exception) {
                null
            }

            val baseMessage = when (e.code()) {
                400 -> "Неверный формат запроса."
                401 -> "Ошибка доступа к API."
                403 -> "Доступ запрещён."
                429 -> "Слишком много запросов."
                500, 502, 503 -> "Сервер временно недоступен."
                else -> "Ошибка сервера: ${e.code()}"
            }

            val errorMessage = if (!errorBody.isNullOrBlank()) {
                "$baseMessage\n\nОтвет сервера: $errorBody"
            } else {
                baseMessage
            }
            emit(Result.failure(ApiException(errorMessage)))
        } catch (e: UnknownHostException) {
            emit(Result.failure(ApiException("Нет подключения к интернету")))
        } catch (e: SocketTimeoutException) {
            emit(Result.failure(ApiException("Превышено время ожидания")))
        } catch (e: Exception) {
            emit(Result.failure(ApiException("Ошибка суммаризации: ${e.localizedMessage}")))
        }
    }

    private fun selectApi(provider: AiProvider): ChatApi = when (provider) {
        AiProvider.DEEPSEEK -> deepSeekApi
        AiProvider.OPENAI -> openAiApi
        AiProvider.HUGGINGFACE -> huggingFaceApi
    }

    private fun getModelName(settings: ChatSettings): String {
        return getActualModel(settings).apiName
    }

    private fun getActualModel(settings: ChatSettings): AiModel {
        return if (settings.deepThinking) {
            when (settings.provider) {
                AiProvider.DEEPSEEK -> AiModel.DEEPSEEK_REASONER
                AiProvider.OPENAI -> AiModel.GPT_4O_MINI
                AiProvider.HUGGINGFACE -> settings.model // HuggingFace не имеет reasoning модели
            }
        } else {
            settings.model
        }
    }

    private fun parseJsonResponse(rawContent: String, metadata: MessageMetadata): Message =
        parseJsonResponse(rawContent, metadata, emptyList())

    private fun parseJsonResponse(
        rawContent: String,
        metadata: MessageMetadata,
        usedTools: List<String>
    ): Message {
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
                ),
                metadata = metadata,
                usedTools = usedTools
            )
        } catch (e: Exception) {
            Log.w(TAG, "JSON parsing failed, showing raw content", e)
            Log.d(TAG, "Raw response: $rawContent")
            Message(
                content = rawContent,
                role = MessageRole.ASSISTANT,
                timestamp = System.currentTimeMillis(),
                metadata = metadata,
                usedTools = usedTools
            )
        }
    }

    private fun parseXmlResponse(rawContent: String, metadata: MessageMetadata): Message =
        parseXmlResponse(rawContent, metadata, emptyList())

    private fun parseXmlResponse(
        rawContent: String,
        metadata: MessageMetadata,
        usedTools: List<String>
    ): Message {
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
                ),
                metadata = metadata,
                usedTools = usedTools
            )
        } catch (e: Exception) {
            Log.w(TAG, "XML parsing failed, showing raw content", e)
            Log.d(TAG, "Raw response: $rawContent")
            Message(
                content = rawContent,
                role = MessageRole.ASSISTANT,
                timestamp = System.currentTimeMillis(),
                metadata = metadata,
                usedTools = usedTools
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
