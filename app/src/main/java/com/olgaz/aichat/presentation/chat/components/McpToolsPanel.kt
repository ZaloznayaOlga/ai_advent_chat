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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.olgaz.aichat.domain.model.McpConnectionState
import com.olgaz.aichat.domain.model.McpTool

@Composable
fun McpToolsPanel(
    connectionState: McpConnectionState,
    tools: List<McpTool>,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Connection status header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    McpStatusIndicator(connectionState)
                    Text(
                        text = "MCP Tools",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = getStatusText(connectionState, tools.size),
                        style = MaterialTheme.typography.labelSmall,
                        color = getStatusColor(connectionState).copy(alpha = 0.8f)
                    )
                }

                when (connectionState) {
                    is McpConnectionState.Connected -> {
                        TextButton(onClick = onDisconnect) {
                            Text(
                                text = "Отключить",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                    is McpConnectionState.Disconnected,
                    is McpConnectionState.Error -> {
                        TextButton(onClick = onConnect) {
                            Text(
                                text = "Подключить",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                    is McpConnectionState.Connecting -> {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(20.dp)
                                .padding(end = 8.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }


            // Error message
            if (connectionState is McpConnectionState.Error) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = connectionState.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun McpStatusIndicator(state: McpConnectionState) {
    val color = getStatusColor(state)

    Box(
        modifier = Modifier
            .size(8.dp)
            .background(color, CircleShape)
    )
}

@Composable
private fun getStatusColor(state: McpConnectionState): Color {
    return when (state) {
        is McpConnectionState.Connected -> Color(0xFF4CAF50) // Green
        is McpConnectionState.Connecting -> Color(0xFFFF9800) // Orange
        is McpConnectionState.Disconnected -> Color.Gray
        is McpConnectionState.Error -> Color(0xFFF44336) // Red
    }
}

private fun getStatusText(state: McpConnectionState, toolsCount: Int): String {
    return when (state) {
        is McpConnectionState.Connected -> "подключено ($toolsCount tools)"
        is McpConnectionState.Connecting -> "подключение..."
        is McpConnectionState.Disconnected -> "отключено"
        is McpConnectionState.Error -> "ошибка"
    }
}

