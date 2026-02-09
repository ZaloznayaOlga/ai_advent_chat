package com.olgaz.aichat.presentation.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.olgaz.aichat.domain.model.RagConnectionState

@Composable
fun RagSection(
    ragEnabled: Boolean,
    onRagEnabledChange: (Boolean) -> Unit,
    ragConnectionState: RagConnectionState,
    ragDocumentsCount: Int,
    onOpenSettings: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {

        RagSwitchItem(
            label = "RAG",
            checked = ragEnabled,
            onCheckedChange = onRagEnabledChange
        )

        if (ragEnabled) {
            RagConnectionStatus(
                connectionState = ragConnectionState,
                documentsCount = ragDocumentsCount
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onOpenSettings,
                modifier = Modifier.padding(start = 16.dp)
            ) {
                Text("Настройка")
            }
        }
    }
}

@Composable
private fun RagSwitchItem(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.5f),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge
        )

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}

@Composable
private fun RagConnectionStatus(
    connectionState: RagConnectionState,
    documentsCount: Int
) {
    val statusColor = when (connectionState) {
        is RagConnectionState.Connected -> Color(0xFF4CAF50)
        is RagConnectionState.Connecting -> Color(0xFFFF9800)
        is RagConnectionState.Disconnected -> Color.Gray
        is RagConnectionState.Error -> Color(0xFFF44336)
    }

    val statusText = when (connectionState) {
        is RagConnectionState.Connected -> "подключено ($documentsCount docs)"
        is RagConnectionState.Connecting -> "подключение..."
        is RagConnectionState.Disconnected -> "отключено"
        is RagConnectionState.Error -> "ошибка"
    }

    Column(modifier = Modifier.padding(start = 16.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(statusColor, CircleShape)
            )
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodySmall,
                color = statusColor
            )
        }

        if (connectionState is RagConnectionState.Error) {
            Text(
                text = connectionState.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 16.dp, top = 2.dp)
            )
        }
    }
}
