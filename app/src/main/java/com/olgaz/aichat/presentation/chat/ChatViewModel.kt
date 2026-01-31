package com.olgaz.aichat.presentation.chat

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.olgaz.aichat.BuildConfig
import com.olgaz.aichat.domain.model.ChatSettings
import com.olgaz.aichat.domain.model.ConversationTokens
import com.olgaz.aichat.domain.model.FileAttachment
import com.olgaz.aichat.domain.model.FileReadResult
import com.olgaz.aichat.domain.model.McpServerConfig
import com.olgaz.aichat.domain.model.Message
import com.olgaz.aichat.domain.model.MessageRole
import com.olgaz.aichat.domain.model.SummarizationInfo
import com.olgaz.aichat.domain.repository.ChatHistoryRepository
import com.olgaz.aichat.domain.repository.ChatRepository
import com.olgaz.aichat.domain.usecase.ReadFileUseCase
import com.olgaz.aichat.domain.usecase.SendMessageUseCase
import com.olgaz.aichat.domain.usecase.mcp.CallMcpToolUseCase
import com.olgaz.aichat.domain.usecase.mcp.ConnectMcpUseCase
import com.olgaz.aichat.domain.usecase.mcp.DisconnectMcpUseCase
import com.olgaz.aichat.domain.usecase.mcp.GetMcpToolsUseCase
import com.olgaz.aichat.domain.usecase.mcp.ObserveMcpConnectionUseCase
import com.olgaz.aichat.notification.ReminderScheduler
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
    private val chatRepository: ChatRepository,
    private val chatHistoryRepository: ChatHistoryRepository,
    private val connectMcpUseCase: ConnectMcpUseCase,
    private val disconnectMcpUseCase: DisconnectMcpUseCase,
    private val getMcpToolsUseCase: GetMcpToolsUseCase,
    private val callMcpToolUseCase: CallMcpToolUseCase,
    private val observeMcpConnectionUseCase: ObserveMcpConnectionUseCase,
    private val reminderScheduler: ReminderScheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        loadSavedData()
        observeMcpState()
    }

    private fun observeMcpState() {
        viewModelScope.launch {
            observeMcpConnectionUseCase().collect { state ->
                _uiState.update { it.copy(mcpConnectionState = state) }
            }
        }
        viewModelScope.launch {
            getMcpToolsUseCase().collect { tools ->
                _uiState.update { it.copy(mcpTools = tools) }
            }
        }
    }

    fun connectToMcp() {
        val settings = _uiState.value.settings
        val url = settings.mcpServerUrl.ifEmpty { BuildConfig.MCP_SERVER_URL }

        if (url.isBlank()) {
            _uiState.update { it.copy(error = "MCP Server URL не настроен") }
            return
        }

        viewModelScope.launch {
            connectMcpUseCase(McpServerConfig(url = url))
        }
    }

    fun disconnectMcp() {
        viewModelScope.launch {
            disconnectMcpUseCase()
        }
    }

    private fun loadSavedData() {
        viewModelScope.launch {
            try {
                val savedSettings = chatHistoryRepository.getSettings()
                val settings = savedSettings ?: ChatSettings()
                _uiState.update { it.copy(settings = settings) }
                if (settings.mcpWeatherEnabled) {
                    connectToMcp()
                }

                val messages = chatHistoryRepository.getAllMessagesOnce()
                val shouldShowSummaryButton = messages.isNotEmpty() &&
                    messages.lastOrNull()?.summarizationInfo == null
                _uiState.update {
                    it.copy(
                        messages = messages,
                        showSummaryButton = shouldShowSummaryButton
                    )
                }

                if (chatHistoryRepository.hasUnansweredUserMessage()) {
                    retryLastMessage()
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = "Ошибка загрузки данных: ${e.message}")
                }
            }
        }
    }

    private fun retryLastMessage() {
        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            try {
                val apiContext = chatHistoryRepository.getMessagesForApiContext()
                val mcpTools = _uiState.value.mcpTools

                sendMessageUseCase(apiContext, _uiState.value.settings, mcpTools).collect { result ->
                    result.fold(
                        onSuccess = { assistantMessage ->
                            chatHistoryRepository.saveMessage(assistantMessage)

                            val updatedMessages = _uiState.value.messages + assistantMessage
                            _uiState.update {
                                it.copy(messages = updatedMessages, isLoading = false)
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
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Ошибка повторной отправки: ${e.message}"
                    )
                }
            }
        }
    }

    fun onInputTextChanged(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun sendMessage() {
        val messageText = _uiState.value.inputText.trim()
        val attachedFile = _uiState.value.attachedFile

        if ((messageText.isEmpty() && attachedFile == null) || _uiState.value.isLoading) return

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
                error = null,
                showSummaryButton = false
            )
        }

        viewModelScope.launch {
            chatHistoryRepository.saveMessage(userMessage)

            val apiContext = chatHistoryRepository.getMessagesForApiContext()
            val mcpTools = _uiState.value.mcpTools

            sendMessageUseCase(apiContext, _uiState.value.settings, mcpTools).collect { result ->
                result.fold(
                    onSuccess = { assistantMessage ->
                        chatHistoryRepository.saveMessage(assistantMessage)

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

        // Find messages after the last summary
        val lastSummaryIndex = messages.indexOfLast { it.summarizationInfo != null }
        val messagesAfterSummary = if (lastSummaryIndex >= 0) {
            messages.subList(lastSummaryIndex + 1, messages.size)
        } else {
            messages
        }

        val relevantMessages = messagesAfterSummary.filter {
            it.role == MessageRole.USER || it.role == MessageRole.ASSISTANT
        }
        val messageCount = relevantMessages.size

        val tokens = ConversationTokens.fromMessages(relevantMessages)
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
            // Only summarize messages after the last summary
            val lastSummaryIndex = messages.indexOfLast { it.summarizationInfo != null }
            val messagesAfterSummary = if (lastSummaryIndex >= 0) {
                messages.subList(lastSummaryIndex + 1, messages.size)
            } else {
                messages
            }

            val messagesToSummarize = messagesAfterSummary.filter {
                it.role == MessageRole.USER || it.role == MessageRole.ASSISTANT
            }
            val messageIds = messagesToSummarize.map { it.id }

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

                            chatHistoryRepository.saveSummaryAndMarkCovered(
                                summaryWithInfo,
                                messageIds
                            )

                            _uiState.update {
                                it.copy(
                                    messages = it.messages + summaryWithInfo,
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
        val oldSettings = _uiState.value.settings
        _uiState.update { it.copy(settings = settings) }
        viewModelScope.launch {
            try {
                chatHistoryRepository.saveSettings(settings)

                // Re-schedule worker if reminder interval changed
                if (oldSettings.reminderCheckIntervalMinutes != settings.reminderCheckIntervalMinutes) {
                    reminderScheduler.schedulePeriodicCheck(settings.reminderCheckIntervalMinutes)
                }

                // If reminders were just disabled, cancel the worker
                if (oldSettings.mcpReminderEnabled && !settings.mcpReminderEnabled) {
                    reminderScheduler.cancelPeriodicCheck()
                }

                // If reminders were just enabled, schedule the worker
                if (!oldSettings.mcpReminderEnabled && settings.mcpReminderEnabled) {
                    reminderScheduler.schedulePeriodicCheck(settings.reminderCheckIntervalMinutes)
                }
            } catch (e: Exception) {
                // Ignore save errors - settings are still in memory
            }
        }
    }

    fun clearChatHistory() {
        viewModelScope.launch {
            chatHistoryRepository.clearAllHistory()
            _uiState.update { it.copy(messages = emptyList(), showSummaryButton = false) }
        }
    }

    fun triggerManualSummarization() {
        val messages = _uiState.value.messages
        if (messages.isEmpty()) return

        _uiState.update { it.copy(showSummaryButton = false) }

        val lastSummaryIndex = messages.indexOfLast { it.summarizationInfo != null }
        val messagesAfterSummary = if (lastSummaryIndex >= 0) {
            messages.subList(lastSummaryIndex + 1, messages.size)
        } else {
            messages
        }

        val relevantMessages = messagesAfterSummary.filter {
            it.role == MessageRole.USER || it.role == MessageRole.ASSISTANT
        }

        if (relevantMessages.isEmpty()) return

        val tokens = ConversationTokens.fromMessages(relevantMessages)
        performSummarization(messages, relevantMessages.size, tokens)
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

    fun reloadMessages() {
        viewModelScope.launch {
            try {
                val messages = chatHistoryRepository.getAllMessagesOnce()
                _uiState.update {
                    it.copy(
                        messages = messages,
                        showSummaryButton = messages.isNotEmpty() &&
                            messages.lastOrNull()?.summarizationInfo == null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = "Ошибка загрузки сообщений: ${e.message}")
                }
            }
        }
    }
}
