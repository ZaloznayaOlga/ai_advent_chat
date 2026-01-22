package com.olgaz.aichat.domain.usecase

import android.net.Uri
import com.olgaz.aichat.domain.model.FileReadResult
import com.olgaz.aichat.domain.provider.FileContentReader
import javax.inject.Inject

class ReadFileUseCase @Inject constructor(
    private val fileContentReader: FileContentReader
) {
    suspend operator fun invoke(uri: Uri): FileReadResult {
        return fileContentReader.readFileContent(uri)
    }
}
