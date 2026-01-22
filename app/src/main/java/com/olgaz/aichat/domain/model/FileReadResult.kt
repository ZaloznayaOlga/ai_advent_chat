package com.olgaz.aichat.domain.model

sealed class FileReadResult {
    data class Success(
        val content: String,
        val fileName: String,
        val characterCount: Int
    ) : FileReadResult()

    data class Error(val message: String) : FileReadResult()

    data object FileTooLarge : FileReadResult()

    data object EmptyFile : FileReadResult()
}
