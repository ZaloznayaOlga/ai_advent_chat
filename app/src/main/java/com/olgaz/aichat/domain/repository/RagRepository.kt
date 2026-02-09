package com.olgaz.aichat.domain.repository

import com.olgaz.aichat.domain.model.RagConnectionState
import com.olgaz.aichat.domain.model.RagDocument
import com.olgaz.aichat.domain.model.RagRerankConfig
import com.olgaz.aichat.domain.model.RagSearchResponse
import kotlinx.coroutines.flow.StateFlow

interface RagRepository {

    val connectionState: StateFlow<RagConnectionState>
    val documents: StateFlow<List<RagDocument>>

    /**
     * Проверить здоровье сервера и обновить состояние подключения
     */
    suspend fun checkHealth(): Result<Unit>

    /**
     * Поиск релевантных чанков
     */
    suspend fun search(query: String, topK: Int = 5): Result<RagSearchResponse>

    /**
     * Загрузить документ
     * @return количество созданных чанков
     */
    suspend fun uploadDocument(name: String, content: String): Result<Int>

    /**
     * Получить список документов
     */
    suspend fun getDocuments(): Result<List<RagDocument>>

    /**
     * Удалить документ
     * @return количество удаленных чанков
     */
    suspend fun deleteDocument(name: String): Result<Int>

    /**
     * Получить конфигурацию реранкинга
     */
    suspend fun getRerankConfig(): Result<RagRerankConfig>

    /**
     * Обновить конфигурацию реранкинга
     */
    suspend fun updateRerankConfig(config: RagRerankConfig): Result<RagRerankConfig>

    /**
     * Сбросить конфигурацию реранкинга к значениям по умолчанию
     */
    suspend fun resetRerankConfig(): Result<RagRerankConfig>
}
