package com.olgaz.aichat.domain.provider

import android.net.Uri
import com.olgaz.aichat.domain.model.FileReadResult

interface FileContentReader {
    suspend fun readFileContent(uri: Uri): FileReadResult
}
