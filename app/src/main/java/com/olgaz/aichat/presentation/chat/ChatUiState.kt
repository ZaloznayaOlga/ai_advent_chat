package com.olgaz.aichat.presentation.chat

import com.olgaz.aichat.domain.model.ChatSettings
import com.olgaz.aichat.domain.model.Message

data class ChatUiState(
    val messages: List<Message> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val settings: ChatSettings = ChatSettings(),
    val isSettingsDialogVisible: Boolean = false
)