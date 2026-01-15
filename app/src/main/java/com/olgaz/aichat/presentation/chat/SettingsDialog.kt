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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.olgaz.aichat.domain.model.AiModel
import com.olgaz.aichat.domain.model.AiProvider
import com.olgaz.aichat.domain.model.ChatSettings
import com.olgaz.aichat.domain.model.CommunicationStyle
import com.olgaz.aichat.domain.model.ResponseFormat
import com.olgaz.aichat.domain.model.SendMessageMode

@Composable
fun SettingsDialog(
    settings: ChatSettings,
    onSettingsChange: (ChatSettings) -> Unit,
    onDismiss: () -> Unit
) {
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
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    DropdownSettingItem(
                        label = "Провайдер",
                        selectedValue = settings.provider.displayName,
                        options = AiProvider.entries.map { it.displayName },
                        onOptionSelected = { displayName ->
                            val provider = AiProvider.entries.first { it.displayName == displayName }
                            val newModel = AiModel.defaultForProvider(provider)
                            onSettingsChange(settings.copy(provider = provider, model = newModel))
                        }
                    )

                    val availableModels = AiModel.forProvider(settings.provider)
                    DropdownSettingItem(
                        label = "Модель",
                        selectedValue = settings.model.displayName,
                        options = availableModels.map { it.displayName },
                        onOptionSelected = { displayName ->
                            val model = availableModels.first { it.displayName == displayName }
                            onSettingsChange(settings.copy(model = model))
                        }
                    )

                    DropdownSettingItem(
                        label = "Стиль общения",
                        selectedValue = settings.communicationStyle.displayName,
                        options = CommunicationStyle.entries.map { it.displayName },
                        onOptionSelected = { displayName ->
                            val style = CommunicationStyle.entries.first { it.displayName == displayName }
                            onSettingsChange(settings.copy(communicationStyle = style))
                        }
                    )

                    SwitchSettingItem(
                        label = "Глубокое мышление",
                        description = when (settings.provider) {
                            AiProvider.DEEPSEEK -> "Использует deepseek-reasoner"
                            AiProvider.OPENAI -> "Использует o1-preview"
                        },
                        checked = settings.deepThinking,
                        onCheckedChange = { enabled ->
                            onSettingsChange(settings.copy(deepThinking = enabled))
                        }
                    )

                    DropdownSettingItem(
                        label = "Формат ответа",
                        selectedValue = settings.responseFormat.displayName,
                        options = ResponseFormat.entries.map { it.displayName },
                        onOptionSelected = { displayName ->
                            val format = ResponseFormat.entries.first { it.displayName == displayName }
                            onSettingsChange(settings.copy(responseFormat = format))
                        }
                    )

                    SwitchSettingItem(
                        label = "Отправка по Shift+Enter",
                        description = "Если выключено - Enter отправляет сообщение",
                        checked = settings.sendMessageMode == SendMessageMode.SHIFT_ENTER,
                        onCheckedChange = { isShiftEnter ->
                            val mode = if (isShiftEnter) SendMessageMode.SHIFT_ENTER else SendMessageMode.ENTER
                            onSettingsChange(settings.copy(sendMessageMode = mode))
                        }
                    )
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
    description: String? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
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
            onCheckedChange = onCheckedChange
        )
    }
}
