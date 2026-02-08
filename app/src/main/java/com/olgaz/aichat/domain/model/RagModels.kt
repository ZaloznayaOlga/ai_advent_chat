package com.olgaz.aichat.domain.model

/**
 * Состояние подключения к RAG серверу
 */
sealed class RagConnectionState {
    data object Disconnected : RagConnectionState()
    data object Connecting : RagConnectionState()
    data object Connected : RagConnectionState()
    data class Error(val message: String) : RagConnectionState()
}

/**
 * Информация о документе в RAG индексе
 */
data class RagDocument(
    val name: String,
    val chunksCount: Int
)

/**
 * Результат поиска в RAG
 */
data class RagSearchResult(
    val documentName: String,
    val chunkIndex: Int,
    val text: String,
    val score: Double
)

/**
 * Ответ на поиск с полным контекстом
 */
data class RagSearchResponse(
    val query: String,
    val results: List<RagSearchResult>
)
