package com.olgaz.aichat.presentation.chat

import com.olgaz.aichat.domain.model.ChatSettings
import com.olgaz.aichat.domain.model.McpConnectionState
import com.olgaz.aichat.domain.model.McpTool
import com.olgaz.aichat.domain.model.Message
import com.olgaz.aichat.domain.model.RagConnectionState
import com.olgaz.aichat.domain.model.RagDocument
import com.olgaz.aichat.domain.model.RagRerankConfig

data class ChatUiState(
    val messages: List<Message> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val isSummarizing: Boolean = false,
    val error: String? = null,
    val settings: ChatSettings = ChatSettings(),
    val attachedFile: AttachedFileInfo? = null,
    val isReadingFile: Boolean = false,
    val showSummaryButton: Boolean = false,
    val mcpConnectionState: McpConnectionState = McpConnectionState.Disconnected,
    val mcpTools: List<McpTool> = emptyList(),
    val ragConnectionState: RagConnectionState = RagConnectionState.Disconnected,
    val ragDocuments: List<RagDocument> = emptyList(),
    val isUploadingRagDocument: Boolean = false,
    val showRagDocumentsDialog: Boolean = false,
    val showRagAddDocumentDialog: Boolean = false,
    val showRagSettingsDialog: Boolean = false,
    val ragError: String? = null,
    val ragRerankConfig: RagRerankConfig = RagRerankConfig.DEFAULT,
    val isLoadingRagConfig: Boolean = false,
    val isSavingRagConfig: Boolean = false
)

data class AttachedFileInfo(
    val fileName: String,
    val content: String,
    val characterCount: Int
)