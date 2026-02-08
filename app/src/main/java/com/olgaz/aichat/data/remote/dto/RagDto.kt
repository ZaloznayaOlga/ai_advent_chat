package com.olgaz.aichat.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Request DTOs

@Serializable
data class AddDocumentRequestDto(
    val name: String,
    val content: String,
    val format: String = "text"
)

@Serializable
data class SearchRequestDto(
    val query: String,
    val topK: Int = 5
)

// Response DTOs

@Serializable
data class AddDocumentResponseDto(
    val success: Boolean,
    val message: String,
    val chunksCount: Int
)

@Serializable
data class DocumentInfoDto(
    val name: String,
    val chunksCount: Int
)

@Serializable
data class DocumentListResponseDto(
    val documents: List<DocumentInfoDto>,
    val totalDocuments: Int,
    val totalChunks: Int
)

@Serializable
data class SearchResultItemDto(
    val documentName: String,
    val chunkIndex: Int,
    val text: String,
    val score: Double
)

@Serializable
data class SearchResponseDto(
    val query: String,
    val results: List<SearchResultItemDto>
)

@Serializable
data class DeleteResponseDto(
    val success: Boolean,
    val message: String,
    val deletedChunks: Int = 0
)

@Serializable
data class HealthResponseDto(
    val status: String,
    val ollamaConnected: Boolean,
    val documentsCount: Int,
    val chunksCount: Int
)
