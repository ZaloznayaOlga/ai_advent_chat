package com.olgaz.aichat.data.repository

import com.olgaz.aichat.data.remote.api.DeepSeekApi
import com.olgaz.aichat.data.remote.dto.ChatRequestDto
import com.olgaz.aichat.data.remote.dto.MessageDto
import com.olgaz.aichat.domain.model.Message
import com.olgaz.aichat.domain.model.MessageRole
import com.olgaz.aichat.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject

class ChatRepositoryImpl @Inject constructor(
    private val api: DeepSeekApi
) : ChatRepository {

    override fun sendMessage(messages: List<Message>): Flow<Result<Message>> = flow {
        try {
            val systemMessage = MessageDto(
                role = "system",
                content = "You are a helpful assistant."
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

            val assistantMessage = response.choices.firstOrNull()?.message?.let {
                Message(
                    content = it.content,
                    role = MessageRole.ASSISTANT,
                    timestamp = System.currentTimeMillis()
                )
            }

            if (assistantMessage != null) {
                emit(Result.success(assistantMessage))
            } else {
                emit(Result.failure(ApiException("Пустой ответ от сервера")))
            }
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
}

class ApiException(message: String) : Exception(message)