package com.olgaz.aichat.presentation.chat.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import com.olgaz.aichat.domain.model.RagDocument
import com.olgaz.aichat.domain.model.RagRerankConfig
import com.olgaz.aichat.presentation.utils.roundTo

@Composable
fun RagSettingsDialog(
    rerankConfig: RagRerankConfig,
    isLoading: Boolean,
    isSaving: Boolean,
    documents: List<RagDocument>,
    onConfigChange: (RagRerankConfig) -> Unit,
    onResetConfig: () -> Unit,
    onAddDocuments: () -> Unit,
    onDeleteDocument: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var localConfig by remember { mutableStateOf(rerankConfig) }

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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Настройки RAG",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Закрыть"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Документы",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (documents.isEmpty()) {
                    Text(
                        text = "Нет загруженных документов",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 200.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(documents, key = { it.name }) { doc ->
                            DocumentItem(
                                document = doc,
                                onDelete = { onDeleteDocument(doc.name) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onAddDocuments()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Добавить документ")
                }

                Spacer(modifier = Modifier.height(16.dp))

                RagSwitchItem(
                    label = "Реранкинг",
                    description = "Переупорядочивание результатов поиска",
                    checked = localConfig.rerankingEnabled,
                    onCheckedChange = { enabled ->
                        localConfig = localConfig.copy(rerankingEnabled = enabled)
                    },
                    enabled = !isLoading && !isSaving
                )

                Spacer(modifier = Modifier.height(8.dp))

                RagSwitchItem(
                    label = "Фильтрация",
                    description = "Отсеивание нерелевантных результатов",
                    checked = localConfig.filteringEnabled,
                    onCheckedChange = { enabled ->
                        localConfig = localConfig.copy(filteringEnabled = enabled)
                    },
                    enabled = !isLoading && !isSaving
                )

                Spacer(modifier = Modifier.height(8.dp))

                MinScoreSlider(
                    value = localConfig.minScore,
                    onValueChange = { score ->
                        localConfig = localConfig.copy(minScore = score.roundTo(1))
                    },
                    enabled = !isLoading && !isSaving
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            localConfig = RagRerankConfig.DEFAULT
                            onResetConfig()
                        },
                        enabled = !isLoading && !isSaving,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Сбросить")
                    }

                    Button(
                        onClick = {
                            onConfigChange(localConfig)
                            onDismiss()
                        },
                        enabled = !isLoading && !isSaving,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isSaving) "Сохранение..." else "Сохранить")
                    }
                }
            }
        }
    }
}

@Composable
private fun DocumentItem(
    document: RagDocument,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = document.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${document.chunksCount} чанков",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Удалить",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun RagSwitchItem(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
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
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}

@Composable
private fun MinScoreSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    enabled: Boolean = true
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Мин. порог схожести",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "Минимальный score для результатов",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = String.format("%.1f", value),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..1f,
            steps = 9,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            )
        )
    }
}
