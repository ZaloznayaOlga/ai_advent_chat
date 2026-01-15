package com.olgaz.aichat.presentation.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.olgaz.aichat.domain.model.ChatSettings
import com.olgaz.aichat.domain.model.Message
import com.olgaz.aichat.domain.model.MessageRole
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
    private val sendMessageUseCase: SendMessageUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    fun onInputTextChanged(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun sendMessage() {
        val messageText = _uiState.value.inputText.trim()
        if (messageText.isEmpty() || _uiState.value.isLoading) return

        val userMessage = Message(
            content = messageText,
            role = MessageRole.USER
        )

        _uiState.update {
            it.copy(
                messages = it.messages + userMessage,
                inputText = "",
                isLoading = true,
                error = null
            )
        }

        viewModelScope.launch {
            sendMessageUseCase(_uiState.value.messages, _uiState.value.settings).collect { result ->
                result.fold(
                    onSuccess = { assistantMessage ->
                        _uiState.update {
                            it.copy(
                                messages = it.messages + assistantMessage,
                                isLoading = false
                            )
                        }
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
}