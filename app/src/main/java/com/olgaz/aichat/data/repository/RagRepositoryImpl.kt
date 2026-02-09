package com.olgaz.aichat.data.repository

import android.util.Log
import com.olgaz.aichat.data.remote.api.RagApi
import com.olgaz.aichat.data.remote.dto.AddDocumentRequestDto
import com.olgaz.aichat.data.remote.dto.RerankConfigUpdateDto
import com.olgaz.aichat.data.remote.dto.SearchRequestDto
import com.olgaz.aichat.data.remote.dto.UpdateRerankConfigRequestDto
import com.olgaz.aichat.domain.model.RagConnectionState
import com.olgaz.aichat.domain.model.RagDocument
import com.olgaz.aichat.domain.model.RagRerankConfig
import com.olgaz.aichat.domain.model.RagSearchResponse
import com.olgaz.aichat.domain.model.RagSearchResult
import com.olgaz.aichat.domain.repository.RagRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "RagRepository"

@Singleton
class RagRepositoryImpl @Inject constructor(
    private val ragApi: RagApi
) : RagRepository {

    private val _connectionState = MutableStateFlow<RagConnectionState>(RagConnectionState.Disconnected)
    override val connectionState: StateFlow<RagConnectionState> = _connectionState.asStateFlow()

    private val _documents = MutableStateFlow<List<RagDocument>>(emptyList())
    override val documents: StateFlow<List<RagDocument>> = _documents.asStateFlow()

    override suspend fun checkHealth(): Result<Unit> = withContext(Dispatchers.IO) {
        _connectionState.value = RagConnectionState.Connecting
        try {
            val response = ragApi.getHealth()
            Log.i(TAG, "RAG server healthy: ${response.documentsCount} docs, ${response.chunksCount} chunks")
            _connectionState.value = RagConnectionState.Connected
            // Refresh documents list on successful health check
            getDocuments()
            Result.success(Unit)
        } catch (e: Exception) {
            val errorMsg = mapError(e)
            Log.e(TAG, "RAG health check failed", e)
            _connectionState.value = RagConnectionState.Error(errorMsg)
            Result.failure(Exception(errorMsg))
        }
    }

    override suspend fun search(query: String, topK: Int): Result<RagSearchResponse> =
        withContext(Dispatchers.IO) {
            try {
                val response = ragApi.search(SearchRequestDto(query, topK))
                val results = response.results.map { item ->
                    RagSearchResult(
                        documentName = item.documentName,
                        chunkIndex = item.chunkIndex,
                        text = item.text,
                        score = item.score
                    )
                }
                Log.d(TAG, "RAG search for '$query' returned ${results.size} results")
                Result.success(RagSearchResponse(response.query, results))
            } catch (e: Exception) {
                Log.e(TAG, "RAG search failed", e)
                Result.failure(Exception(mapError(e)))
            }
        }

    override suspend fun uploadDocument(name: String, content: String): Result<Int> =
        withContext(Dispatchers.IO) {
            try {
                val response = ragApi.uploadDocument(
                    AddDocumentRequestDto(name, content, "text")
                )
                if (response.success) {
                    Log.i(TAG, "Document '$name' uploaded: ${response.chunksCount} chunks")
                    getDocuments() // Refresh documents list
                    Result.success(response.chunksCount)
                } else {
                    Result.failure(Exception(response.message))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Document upload failed", e)
                Result.failure(Exception(mapError(e)))
            }
        }

    override suspend fun getDocuments(): Result<List<RagDocument>> = withContext(Dispatchers.IO) {
        try {
            val response = ragApi.getDocuments()
            val docs = response.documents.map { dto ->
                RagDocument(name = dto.name, chunksCount = dto.chunksCount)
            }
            _documents.value = docs
            Log.i(TAG, "Loaded ${docs.size} documents")
            Result.success(docs)
        } catch (e: Exception) {
            Log.e(TAG, "Get documents failed", e)
            Result.failure(Exception(mapError(e)))
        }
    }

    override suspend fun deleteDocument(name: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val response = ragApi.deleteDocument(name)
            if (response.success) {
                Log.i(TAG, "Document '$name' deleted: ${response.deletedChunks} chunks")
                getDocuments() // Refresh documents list
                Result.success(response.deletedChunks)
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Delete document failed", e)
            Result.failure(Exception(mapError(e)))
        }
    }

    override suspend fun getRerankConfig(): Result<RagRerankConfig> = withContext(Dispatchers.IO) {
        try {
            val response = ragApi.getRerankConfig()
            if (response.success) {
                val config = RagRerankConfig(
                    rerankingEnabled = response.config.rerankingEnabled ?: false,
                    filteringEnabled = response.config.filteringEnabled ?: false,
                    minScore = response.config.minScore ?: 0.0f
                )
                Log.d(TAG, "Loaded rerank config: $config")
                Result.success(config)
            } else {
                Result.failure(Exception("Не удалось получить конфигурацию"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Get rerank config failed", e)
            Result.failure(Exception(mapError(e)))
        }
    }

    override suspend fun updateRerankConfig(config: RagRerankConfig): Result<RagRerankConfig> =
        withContext(Dispatchers.IO) {
            try {
                val request = UpdateRerankConfigRequestDto(
                    config = RerankConfigUpdateDto(
                        rerankingEnabled = config.rerankingEnabled,
                        filteringEnabled = config.filteringEnabled,
                        minScore = config.minScore
                    )
                )
                val response = ragApi.updateRerankConfig(request)
                if (response.success) {
                    val updatedConfig = RagRerankConfig(
                        rerankingEnabled = response.config.rerankingEnabled ?: false,
                        filteringEnabled = response.config.filteringEnabled ?: false,
                        minScore = response.config.minScore ?: 0.0f
                    )
                    Log.i(TAG, "Rerank config updated: $updatedConfig")
                    Result.success(updatedConfig)
                } else {
                    Result.failure(Exception("Не удалось обновить конфигурацию"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Update rerank config failed", e)
                Result.failure(Exception(mapError(e)))
            }
        }

    override suspend fun resetRerankConfig(): Result<RagRerankConfig> = withContext(Dispatchers.IO) {
        try {
            val response = ragApi.resetRerankConfig()
            if (response.success && response.config != null) {
                val config = RagRerankConfig(
                    rerankingEnabled = response.config.rerankingEnabled ?: false,
                    filteringEnabled = response.config.filteringEnabled ?: false,
                    minScore = response.config.minScore ?: 0.0f
                )
                Log.i(TAG, "Rerank config reset to defaults: $config")
                Result.success(config)
            } else {
                Result.failure(Exception("Не удалось сбросить конфигурацию"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Reset rerank config failed", e)
            Result.failure(Exception(mapError(e)))
        }
    }

    private fun mapError(e: Exception): String = when (e) {
        is UnknownHostException -> "RAG сервер недоступен"
        is SocketTimeoutException -> "Превышено время ожидания RAG сервера"
        is HttpException -> when (e.code()) {
            404 -> "Документ не найден"
            500 -> "Ошибка RAG сервера"
            else -> "Ошибка: ${e.code()}"
        }
        else -> e.message ?: "Неизвестная ошибка"
    }
}
