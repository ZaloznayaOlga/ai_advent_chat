package com.olgaz.aichat.presentation.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.Button
import com.olgaz.aichat.domain.model.AiModel
import com.olgaz.aichat.domain.model.AiProvider
import com.olgaz.aichat.domain.model.ChatSettings
import com.olgaz.aichat.domain.model.CommunicationStyle
import com.olgaz.aichat.domain.model.ResponseFormat
import com.olgaz.aichat.domain.model.SendMessageMode
import com.olgaz.aichat.domain.model.SystemPromptMode

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SettingsDialog(
    settings: ChatSettings,
    onSettingsChange: (ChatSettings) -> Unit,
    onDismiss: () -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    var localSettings by remember { mutableStateOf(settings) }
    val defaultSettings = ChatSettings()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                SettingsHeader(onDismiss = onDismiss)

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    DropdownSettingItem(
                        label = "Провайдер AI",
                        selectedValue = localSettings.provider.displayName,
                        options = AiProvider.entries.map { it.displayName },
                        onOptionSelected = { displayName ->
                            val provider = AiProvider.entries.first { it.displayName == displayName }
                            val defaultModel = AiModel.defaultForProvider(provider)
                            localSettings = localSettings.copy(
                                provider = provider,
                                model = defaultModel
                            )
                        }
                    )

                    DropdownSettingItem(
                        label = "Модель",
                        selectedValue = localSettings.model.displayName,
                        options = AiModel.forProvider(localSettings.provider).map { it.displayName },
                        onOptionSelected = { displayName ->
                            val model = AiModel.entries.first {
                                it.displayName == displayName && it.provider == localSettings.provider
                            }
                            localSettings = localSettings.copy(model = model)
                        }
                    )

                    DropdownSettingItem(
                        label = "Стиль общения",
                        selectedValue = localSettings.communicationStyle.displayName,
                        options = CommunicationStyle.entries.map { it.displayName },
                        onOptionSelected = { displayName ->
                            val style = CommunicationStyle.entries.first { it.displayName == displayName }
                            localSettings = localSettings.copy(communicationStyle = style)
                        }
                    )

                    SwitchSettingItem(
                        label = "Глубокое мышление",
                        description = when (localSettings.provider) {
                            AiProvider.DEEPSEEK -> "Использует deepseek-reasoner"
                            AiProvider.OPENAI -> "Использует o1-preview"
                            AiProvider.HUGGINGFACE -> "Недоступно для HuggingFace"
                        },
                        checked = localSettings.deepThinking,
                        onCheckedChange = { enabled ->
                            localSettings = localSettings.copy(deepThinking = enabled)
                        },
                        enabled = localSettings.provider != AiProvider.HUGGINGFACE
                    )

                    SliderSettingItem(
                        label = "Температура",
                        value = localSettings.temperature,
                        onValueChange = { newTemp ->
                            localSettings = localSettings.copy(temperature = newTemp)
                        },
                        valueRange = 0f..1.5f,
                        steps = 14
                    )

                    if (localSettings.systemPromptMode != SystemPromptMode.CUSTOM) {
                        DropdownSettingItem(
                            label = "Формат ответа",
                            selectedValue = localSettings.responseFormat.displayName,
                            options = ResponseFormat.entries.map { it.displayName },
                            onOptionSelected = { displayName ->
                                val format = ResponseFormat.entries.first { it.displayName == displayName }
                                localSettings = localSettings.copy(responseFormat = format)
                            }
                        )
                    }

                    DropdownSettingItem(
                        label = "Системный промпт",
                        selectedValue = localSettings.systemPromptMode.displayName,
                        options = SystemPromptMode.entries.map { it.displayName },
                        onOptionSelected = { displayName ->
                            val mode = SystemPromptMode.entries.first { it.displayName == displayName }
                            if (mode == SystemPromptMode.CUSTOM) {
                                localSettings = localSettings.copy(
                                    systemPromptMode = mode,
                                    responseFormat = ResponseFormat.TEXT
                                )
                            } else {
                                localSettings = localSettings.copy(systemPromptMode = mode)
                            }
                        }
                    )

                    if (localSettings.systemPromptMode == SystemPromptMode.CUSTOM) {
                        Column {
                            Text(
                                text = "Текст промпта",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            OutlinedTextField(
                                value = localSettings.customSystemPrompt,
                                onValueChange = { newPrompt ->
                                    localSettings = localSettings.copy(customSystemPrompt = newPrompt)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("Введите системный промпт...") },
                                minLines = 3,
                                maxLines = 6
                            )
                        }
                    }

                    SwitchSettingItem(
                        label = "Отправка по Shift+Enter",
                        description = "Если выключено - Enter отправляет сообщение",
                        checked = localSettings.sendMessageMode == SendMessageMode.SHIFT_ENTER,
                        onCheckedChange = { isShiftEnter ->
                            val mode = if (isShiftEnter) SendMessageMode.SHIFT_ENTER else SendMessageMode.ENTER
                            localSettings = localSettings.copy(sendMessageMode = mode)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { localSettings = defaultSettings },
                        enabled = localSettings != defaultSettings,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Сбросить")
                    }

                    Button(
                        onClick = {
                            keyboardController?.hide()
                            onSettingsChange(localSettings)
                            onDismiss()
                        },
                        enabled = localSettings != settings,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Сохранить")
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsHeader(onDismiss: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Настройки",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        IconButton(onClick = onDismiss) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Закрыть"
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownSettingItem(
    label: String,
    selectedValue: String,
    options: List<String>,
    onOptionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(4.dp))

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = selectedValue,
                onValueChange = {},
                readOnly = true,
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onOptionSelected(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SwitchSettingItem(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    description: String? = null,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.5f),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge
            )
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}

@Composable
private fun SliderSettingItem(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = String.format("%.1f", value),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = Color(0xFFB85450),
                inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                activeTickColor = Color.White,
                inactiveTickColor = Color.White
            )
        )
    }
}
