package com.olgaz.aichat.data.provider

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.olgaz.aichat.domain.model.FileReadResult
import com.olgaz.aichat.domain.provider.FileContentReader
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class FileContentReaderImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : FileContentReader {

    companion object {
        private const val MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024L // 10MB
        private const val MAX_TEXT_LENGTH = 100_000 // ~25K tokens
    }

    override suspend fun readFileContent(uri: Uri): FileReadResult = withContext(Dispatchers.IO) {
        try {
            val (fileName, fileSize) = getFileMetadata(uri)

            if (fileSize > MAX_FILE_SIZE_BYTES) {
                return@withContext FileReadResult.FileTooLarge
            }

            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext FileReadResult.Error("Не удалось открыть файл")

            val content = inputStream.use { stream ->
                stream.bufferedReader().readText()
            }

            if (content.isBlank()) {
                return@withContext FileReadResult.EmptyFile
            }

            val truncatedContent = if (content.length > MAX_TEXT_LENGTH) {
                content.take(MAX_TEXT_LENGTH) + "\n\n[Содержимое обрезано из-за размера файла]"
            } else {
                content
            }

            FileReadResult.Success(
                content = truncatedContent,
                fileName = fileName ?: "unknown.txt",
                characterCount = truncatedContent.length
            )
        } catch (e: Exception) {
            FileReadResult.Error("Ошибка чтения файла: ${e.localizedMessage}")
        }
    }

    private fun getFileMetadata(uri: Uri): Pair<String?, Long> {
        var fileName: String? = null
        var fileSize: Long = 0

        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)

                if (nameIndex >= 0) fileName = cursor.getString(nameIndex)
                if (sizeIndex >= 0) fileSize = cursor.getLong(sizeIndex)
            }
        }

        return fileName to fileSize
    }
}
