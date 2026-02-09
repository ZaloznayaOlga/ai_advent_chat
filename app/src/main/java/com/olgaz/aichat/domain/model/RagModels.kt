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

/**
 * Конфигурация реранкинга RAG
 */
data class RagRerankConfig(
    val rerankingEnabled: Boolean,
    val filteringEnabled: Boolean,
    val minScore: Float
) {
    companion object {
        val DEFAULT = RagRerankConfig(
            rerankingEnabled = false,
            filteringEnabled = false,
            minScore = 0.0f
        )
    }
}

/**
 * Источник из RAG для отображения в сообщении.
 * Сохраняется вместе с сообщением для отображения цитат и ссылок.
 */
data class RagSource(
    val documentName: String,
    val chunkIndex: Int,
    val text: String,
    val score: Double
) {
    companion object {
        fun fromSearchResult(result: RagSearchResult): RagSource = RagSource(
            documentName = result.documentName,
            chunkIndex = result.chunkIndex,
            text = result.text,
            score = result.score
        )
    }
}
