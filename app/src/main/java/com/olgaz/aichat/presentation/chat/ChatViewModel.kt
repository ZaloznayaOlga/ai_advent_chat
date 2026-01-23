package com.olgaz.aichat.presentation.chat

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.olgaz.aichat.domain.model.ChatSettings
import com.olgaz.aichat.domain.model.ConversationTokens
import com.olgaz.aichat.domain.model.FileAttachment
import com.olgaz.aichat.domain.model.FileReadResult
import com.olgaz.aichat.domain.model.Message
import com.olgaz.aichat.domain.model.MessageRole
import com.olgaz.aichat.domain.model.SummarizationInfo
import com.olgaz.aichat.domain.repository.ChatRepository
import com.olgaz.aichat.domain.usecase.ReadFileUseCase
import com.olgaz.aichat.domain.usecase.SendMessageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val sendMessageUseCase: SendMessageUseCase,
    private val readFileUseCase: ReadFileUseCase,
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    fun onInputTextChanged(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun sendMessage() {
        val messageText = _uiState.value.inputText.trim()
        val attachedFile = _uiState.value.attachedFile

        if ((messageText.isEmpty() && attachedFile == null) || _uiState.value.isLoading) return

        // Content for API - includes file content
        val apiContent = buildString {
            if (attachedFile != null) {
                append("[Файл: ${attachedFile.fileName}]\n")
                append("---\n")
                append(attachedFile.content)
                if (messageText.isNotEmpty()) {
                    append("\n---\n")
                    append(messageText)
                }
            } else {
                append(messageText)
            }
        }

        // Display content - only user's text (without file content)
        val displayText = messageText.ifEmpty { "" }

        val userMessage = Message(
            content = apiContent,
            role = MessageRole.USER,
            attachedFile = attachedFile?.let {
                FileAttachment(
                    fileName = it.fileName,
                    characterCount = it.characterCount
                )
            },
            displayContent = displayText
        )

        _uiState.update {
            it.copy(
                messages = it.messages + userMessage,
                inputText = "",
                attachedFile = null,
                isLoading = true,
                error = null
            )
        }

        viewModelScope.launch {
            sendMessageUseCase(_uiState.value.messages, _uiState.value.settings).collect { result ->
                result.fold(
                    onSuccess = { assistantMessage ->
                        val updatedMessages = _uiState.value.messages + assistantMessage
                        _uiState.update {
                            it.copy(
                                messages = updatedMessages,
                                isLoading = false
                            )
                        }
                        checkAndTriggerSummarization(updatedMessages)
                    },
                    onFailure = { exception ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = exception.message ?: "Unknown error occurred"
                            )
                        }
                    }
                )
            }
        }
    }

    private fun checkAndTriggerSummarization(messages: List<Message>) {
        val settings = _uiState.value.settings.summarization
        if (!settings.enabled) return

        val relevantMessages = messages.filter {
            it.summarizationInfo == null &&
            (it.role == MessageRole.USER || it.role == MessageRole.ASSISTANT)
        }
        val messageCount = relevantMessages.size

        val tokens = ConversationTokens.fromMessages(messages)
        val tokenCount = tokens.totalTokens

        val shouldSummarize = messageCount >= settings.messageThreshold ||
                              tokenCount >= settings.tokenThreshold

        if (shouldSummarize) {
            performSummarization(messages, messageCount, tokens)
        }
    }

    private fun performSummarization(
        messages: List<Message>,
        messageCount: Int,
        tokens: ConversationTokens
    ) {
        _uiState.update { it.copy(isSummarizing = true) }

        viewModelScope.launch {
            chatRepository.summarizeConversation(messages, _uiState.value.settings)
                .collect { result ->
                    result.fold(
                        onSuccess = { summaryMessage ->
                            val summaryWithInfo = summaryMessage.copy(
                                summarizationInfo = SummarizationInfo(
                                    summarizedMessageCount = messageCount,
                                    summarizedInputTokens = tokens.totalInputTokens,
                                    summarizedOutputTokens = tokens.totalOutputTokens
                                )
                            )
                            _uiState.update {
                                it.copy(
                                    messages = listOf(summaryWithInfo),
                                    isSummarizing = false
                                )
                            }
                        },
                        onFailure = { error ->
                            _uiState.update {
                                it.copy(
                                    isSummarizing = false,
                                    error = "Ошибка суммаризации: ${error.message}"
                                )
                            }
                        }
                    )
                }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun showSettingsDialog() {
        _uiState.update { it.copy(isSettingsDialogVisible = true) }
    }

    fun hideSettingsDialog() {
        _uiState.update { it.copy(isSettingsDialogVisible = false) }
    }

    fun updateSettings(settings: ChatSettings) {
        _uiState.update { it.copy(settings = settings) }
    }

    fun clearChatHistory() {
        _uiState.update { it.copy(messages = emptyList()) }
    }

    fun onFileSelected(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isReadingFile = true, error = null) }

            when (val result = readFileUseCase(uri)) {
                is FileReadResult.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            isReadingFile = false,
                            attachedFile = AttachedFileInfo(
                                fileName = result.fileName,
                                content = result.content,
                                characterCount = result.characterCount
                            )
                        )
                    }
                }
                is FileReadResult.Error -> {
                    _uiState.update { it.copy(isReadingFile = false, error = result.message) }
                }
                FileReadResult.FileTooLarge -> {
                    _uiState.update {
                        it.copy(isReadingFile = false, error = "Файл слишком большой (максимум 10MB)")
                    }
                }
                FileReadResult.EmptyFile -> {
                    _uiState.update {
                        it.copy(isReadingFile = false, error = "Файл пустой")
                    }
                }
            }
        }
    }

    fun clearAttachedFile() {
        _uiState.update { it.copy(attachedFile = null) }
    }
}