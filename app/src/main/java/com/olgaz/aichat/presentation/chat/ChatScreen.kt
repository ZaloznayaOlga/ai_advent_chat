package com.olgaz.aichat.presentation.chat

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.hilt.navigation.compose.hiltViewModel
import com.olgaz.aichat.domain.model.AiModel
import com.olgaz.aichat.domain.model.AiProvider
import com.olgaz.aichat.domain.model.FileAttachment
import com.olgaz.aichat.domain.model.ConversationTokens
import com.olgaz.aichat.domain.model.Message
import com.olgaz.aichat.domain.model.MessageJsonData
import com.olgaz.aichat.domain.model.MessageMetadata
import com.olgaz.aichat.domain.model.MessageRole
import com.olgaz.aichat.domain.model.ResponseFormat
import com.olgaz.aichat.domain.model.SendMessageMode
import com.olgaz.aichat.domain.model.SummarizationInfo
import com.olgaz.aichat.ui.theme.GradientDarkEnd
import com.olgaz.aichat.ui.theme.GradientDarkStart
import com.olgaz.aichat.ui.theme.GradientLightEnd
import com.olgaz.aichat.ui.theme.GradientLightStart
import com.olgaz.aichat.ui.theme.GreenAssistant
import com.olgaz.aichat.ui.theme.GreenUser
import com.olgaz.aichat.ui.theme.GreyDark
import com.olgaz.aichat.ui.theme.GreyLight
import com.olgaz.aichat.ui.theme.OrangeSummarization
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val isDarkTheme = isSystemInDarkTheme()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.onFileSelected(it) }
    }

    val gradientBrush = Brush.verticalGradient(
        colors = if (isDarkTheme) {
            listOf(GradientDarkStart, GradientDarkEnd)
        } else {
            listOf(GradientLightStart, GradientLightEnd)
        }
    )

    val chatBackgroundColor = if (isDarkTheme) GreyDark else GreyLight

    if (uiState.isSettingsDialogVisible) {
        SettingsDialog(
            settings = uiState.settings,
            onSettingsChange = viewModel::updateSettings,
            onDismiss = viewModel::hideSettingsDialog
        )
    }

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(gradientBrush)
            ) {
                TopAppBar(
                    title = {
                        Text(
                            text = uiState.settings.model.displayName,
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    actions = {
                        val conversationTokens = remember(uiState.messages) {
                            ConversationTokens.fromMessages(uiState.messages)
                        }
                        if (conversationTokens.totalTokens > 0) {
                            val tokensColor = getTokensIndicatorColor(
                                totalTokens = conversationTokens.totalTokens,
                                model = uiState.settings.model
                            )
                            Text(
                                text = "${conversationTokens.formatTotal()} tokens",
                                style = MaterialTheme.typography.labelMedium,
                                color = tokensColor,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                        }
                        val hasMessages = uiState.messages.isNotEmpty()
                        IconButton(
                            onClick = { viewModel.clearChatHistory() },
                            enabled = hasMessages
                        ) {
                            Icon(
                                imageVector = if (hasMessages) Icons.Filled.Delete else Icons.Outlined.Delete,
                                contentDescription = "Очистить историю",
                                tint = if (hasMessages) Color.White else Color.White.copy(alpha = 0.4f)
                            )
                        }
                        IconButton(onClick = { viewModel.showSettingsDialog() }) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Настройки",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = Color.White
                    )
                )
            }
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        },
        modifier = Modifier.imePadding()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(chatBackgroundColor)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (uiState.messages.isEmpty() && !uiState.isLoading) {
                    EmptyState()
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = listState,
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = uiState.messages,
                            key = { it.id }
                        ) { message ->
                            MessageItem(message = message)
                        }

                        if (uiState.isLoading) {
                            item {
                                ThinkingIndicator()
                            }
                        }

                        if (uiState.isSummarizing) {
                            item {
                                SummarizingIndicator()
                            }
                        }
                    }
                }
            }

            MessageInput(
                text = uiState.inputText,
                onTextChange = viewModel::onInputTextChanged,
                onSendClick = viewModel::sendMessage,
                onAttachFileClick = { filePickerLauncher.launch(arrayOf("text/plain")) },
                isLoading = uiState.isLoading || uiState.isSummarizing,
                isReadingFile = uiState.isReadingFile,
                attachedFile = uiState.attachedFile,
                onClearAttachment = viewModel::clearAttachedFile,
                gradientBrush = gradientBrush,
                sendMessageMode = uiState.settings.sendMessageMode
            )
        }
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Еще нет сообщений.\nНачните общение с AI!",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageItem(message: Message) {
    val isUser = message.role == MessageRole.USER
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val formattedTime = remember(message.timestamp) {
        timeFormat.format(Date(message.timestamp))
    }
    val clipboardManager = LocalClipboardManager.current
    var showCopiedHint by remember { mutableStateOf(false) }

    val textToCopy = if (message.jsonData != null) {
        message.jsonData.answer
    } else {
        message.content
    }

    val onCopyText: () -> Unit = {
        clipboardManager.setText(AnnotatedString(textToCopy))
        showCopiedHint = true
    }

    LaunchedEffect(showCopiedHint) {
        if (showCopiedHint) {
            kotlinx.coroutines.delay(1500)
            showCopiedHint = false
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        when {
            message.summarizationInfo != null -> {
                SummarizationMessageCard(
                    content = message.content,
                    summarizationInfo = message.summarizationInfo,
                    onLongClick = onCopyText
                )
            }
            isUser -> {
                UserMessageCard(
                    content = message.displayContent,
                    attachedFile = message.attachedFile,
                    onLongClick = onCopyText
                )
            }
            message.jsonData != null -> {
                AssistantStructuredMessage(jsonData = message.jsonData, onLongClick = onCopyText)
            }
            else -> {
                AssistantSimpleMessageCard(content = message.content, onLongClick = onCopyText)
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Text(
                text = formattedTime,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (showCopiedHint) {
                Text(
                    text = "Скопировано",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF388E3C)
                )
            }
        }

        // Metadata info for assistant messages
        if (!isUser && message.metadata != null) {
            MetadataInfo(metadata = message.metadata)
        }
    }
}

@Composable
private fun MetadataInfo(metadata: MessageMetadata) {
    val timeFormatted = remember(metadata.responseTimeMs) {
        if (metadata.responseTimeMs >= 1000) {
            String.format(Locale.US, "%.1fс", metadata.responseTimeMs / 1000.0)
        } else {
            "${metadata.responseTimeMs}мс"
        }
    }

    val costFormatted = remember(metadata) {
        val cost = metadata.calculateCost()
        if (cost > 0) {
            val formatter = DecimalFormat("0.######")
            "\$${formatter.format(cost)}"
        } else {
            "free"
        }
    }

    val totalTokens = metadata.inputTokens + metadata.outputTokens

    val usagePercentValue = remember(metadata, totalTokens) {
        metadata.model?.maxTokens?.let { maxTokens ->
            totalTokens.toDouble() / maxTokens * 100
        }
    }

    val usagePercentText = remember(usagePercentValue) {
        usagePercentValue?.let { String.format(Locale.US, "%.1f%%", it) }
    }

    val usagePercentColor = remember(usagePercentValue) {
        when {
            usagePercentValue == null -> null
            usagePercentValue <= 70.0 -> Color(0xFF4CAF50) // Green
            usagePercentValue <= 90.0 -> Color(0xFFFF9800) // Orange
            else -> Color(0xFFF44336) // Red
        }
    }

    val baseColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)

    Row(modifier = Modifier.padding(horizontal = 4.dp)) {
        Text(
            text = "⏱$timeFormatted | $totalTokens tokens",
            style = MaterialTheme.typography.labelSmall,
            color = baseColor
        )
        if (usagePercentText != null && usagePercentColor != null) {
            Text(
                text = " ($usagePercentText)",
                style = MaterialTheme.typography.labelSmall,
                color = usagePercentColor
            )
        }
        Text(
            text = " (⬇${metadata.inputTokens} ⬆${metadata.outputTokens}) | $costFormatted",
            style = MaterialTheme.typography.labelSmall,
            color = baseColor
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun UserMessageCard(
    content: String,
    attachedFile: FileAttachment?,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .widthIn(max = 300.dp)
            .combinedClickable(
                onClick = { },
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(
            topStart = 16.dp,
            topEnd = 16.dp,
            bottomStart = 16.dp,
            bottomEnd = 4.dp
        ),
        colors = CardDefaults.cardColors(containerColor = GreenUser)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            if (attachedFile != null) {
                MessageFileChip(
                    fileName = attachedFile.fileName,
                    characterCount = attachedFile.characterCount
                )
                if (content.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            if (content.isNotEmpty()) {
                Text(
                    text = content,
                    color = Color.Black,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun MessageFileChip(
    fileName: String,
    characterCount: Int
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.Black.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = Color.Black.copy(alpha = 0.7f)
            )
            Text(
                text = "$fileName ($characterCount chars)",
                style = MaterialTheme.typography.labelMedium,
                color = Color.Black.copy(alpha = 0.8f)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AssistantSimpleMessageCard(content: String, onLongClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { },
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = GreenAssistant)
    ) {
        Text(
            text = content,
            modifier = Modifier.padding(12.dp),
            color = Color.Black,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SummarizationMessageCard(
    content: String,
    summarizationInfo: SummarizationInfo,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { },
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = OrangeSummarization)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "📋",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Резюме диалога",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE65100)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Суммаризировано: ${summarizationInfo.summarizedMessageCount} сообщений, " +
                       "${summarizationInfo.totalSummarizedTokens} токенов",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF795548)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Black
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
private fun AssistantStructuredMessage(jsonData: MessageJsonData, onLongClick: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    val borderColor = Color.Gray.copy(alpha = 0.4f)
    var showJsonDialog by remember { mutableStateOf(false) }

    if (showJsonDialog) {
        SourceViewerDialog(
            content = jsonData.rawJson,
            responseFormat = jsonData.responseFormat,
            onDismiss = { showJsonDialog = false }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { },
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = GreenAssistant)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Topic
            LabeledSection(
                label = "Тема",
                borderColor = borderColor,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = jsonData.topic,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )
            }

            // Question
            LabeledSection(
                label = "Вопрос",
                borderColor = borderColor,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = jsonData.question,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Black
                )
            }

            // Answer - main content
            LabeledSection(
                label = "Ответ",
                borderColor = borderColor,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = jsonData.answer,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Black
                )
            }

            // Tags
            if (jsonData.tags.isNotEmpty()) {
                LabeledSection(
                    label = "Теги",
                    borderColor = borderColor,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        jsonData.tags.forEach { tag ->
                            TagChip(tag = tag)
                        }
                    }
                }
            }

            // Links
            if (jsonData.links.isNotEmpty()) {
                LabeledSection(
                    label = "Ссылки",
                    borderColor = borderColor,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        jsonData.links.forEach { link ->
                            Text(
                                text = link,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF1976D2),
                                textDecoration = TextDecoration.Underline,
                                modifier = Modifier.clickable {
                                    try {
                                        uriHandler.openUri(link)
                                    } catch (_: Exception) { }
                                }
                            )
                        }
                    }
                }
            }

            // Source data section (JSON/XML)
            val formatLabel = when (jsonData.responseFormat) {
                ResponseFormat.XML -> "XML"
                else -> "JSON"
            }
            val buttonText = when (jsonData.responseFormat) {
                ResponseFormat.XML -> "Показать исходный XML"
                else -> "Показать исходный JSON"
            }
            LabeledSection(
                label = formatLabel,
                borderColor = borderColor,
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = { showJsonDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF81C784)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = buttonText,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.Black
                    )
                }
            }
        }
    }
}

@Composable
private fun LabeledSection(
    label: String,
    borderColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(8.dp)
    ) {
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            content()
        }
    }
}

@Composable
private fun TagChip(tag: String) {
    Box(
        modifier = Modifier
            .background(
                color = Color(0xFF81C784).copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = 1.dp,
                color = Color(0xFF388E3C).copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = "#$tag",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF2E7D32),
            fontWeight = FontWeight.Medium
        )
    }
}

private fun formatDateTime(isoDateTime: String): Pair<String, String> {
    return try {
        val months = listOf(
            "января", "февраля", "марта", "апреля", "мая", "июня",
            "июля", "августа", "сентября", "октября", "ноября", "декабря"
        )

        val datePart = isoDateTime.substringBefore("T")
        val timePart = isoDateTime.substringAfter("T").take(5)

        val parts = datePart.split("-")
        if (parts.size == 3) {
            val year = parts[0]
            val month = parts[1].toIntOrNull()?.minus(1) ?: 0
            val day = parts[2].toIntOrNull() ?: 1

            val monthName = months.getOrElse(month) { "???" }
            Pair("$day $monthName $year", timePart)
        } else {
            Pair(isoDateTime, "")
        }
    } catch (e: Exception) {
        Pair(isoDateTime, "")
    }
}

@Composable
private fun SourceViewerDialog(
    content: String,
    responseFormat: ResponseFormat,
    onDismiss: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val scrollState = rememberScrollState()

    val highlightedContent = remember(content, responseFormat) {
        when (responseFormat) {
            ResponseFormat.XML -> highlightXmlSyntax(content)
            else -> highlightJsonSyntax(content)
        }
    }

    val title = when (responseFormat) {
        ResponseFormat.XML -> "Исходный XML"
        else -> "Исходный JSON"
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF2D2D2D)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )

                    TextButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(content))
                        }
                    ) {
                        Text(
                            text = "Копировать",
                            color = Color(0xFF81C784)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Content with scroll and syntax highlighting
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp)
                        .background(
                            color = Color(0xFF1E1E1E),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp)
                ) {
                    Text(
                        text = highlightedContent,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.verticalScroll(scrollState)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Close button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF388E3C)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(text = "Закрыть")
                }
            }
        }
    }
}

private fun highlightJsonSyntax(json: String): AnnotatedString {
    val bracketColor = Color.White
    val keyColor = Color(0xFF9CDCFE)      // Light blue
    val valueStringColor = Color(0xFF6A9955)  // Green
    val valueNumberColor = Color(0xFFB5CEA8)  // Light green for numbers
    val valueBoolNullColor = Color(0xFF569CD6) // Blue for true/false/null

    return buildAnnotatedString {
        var i = 0
        var inString = false
        var isKey = false
        var afterColon = false

        while (i < json.length) {
            val char = json[i]

            when {
                // Handle escape sequences in strings
                char == '\\' && inString && i + 1 < json.length -> {
                    val color = if (isKey) keyColor else valueStringColor
                    withStyle(SpanStyle(color = color)) {
                        append(char)
                        append(json[i + 1])
                    }
                    i += 2
                    continue
                }

                // String start/end
                char == '"' -> {
                    if (!inString) {
                        inString = true
                        isKey = !afterColon
                        val color = if (isKey) keyColor else valueStringColor
                        withStyle(SpanStyle(color = color)) {
                            append(char)
                        }
                    } else {
                        val color = if (isKey) keyColor else valueStringColor
                        withStyle(SpanStyle(color = color)) {
                            append(char)
                        }
                        inString = false
                        if (isKey) {
                            isKey = false
                        }
                    }
                }

                // Inside string
                inString -> {
                    val color = if (isKey) keyColor else valueStringColor
                    withStyle(SpanStyle(color = color)) {
                        append(char)
                    }
                }

                // Brackets and braces
                char in "{}[]" -> {
                    withStyle(SpanStyle(color = bracketColor)) {
                        append(char)
                    }
                    if (char == '{' || char == '[') {
                        afterColon = false
                    }
                }

                // Colon
                char == ':' -> {
                    withStyle(SpanStyle(color = bracketColor)) {
                        append(char)
                    }
                    afterColon = true
                }

                // Comma
                char == ',' -> {
                    withStyle(SpanStyle(color = bracketColor)) {
                        append(char)
                    }
                    afterColon = false
                }

                // Numbers
                char.isDigit() || (char == '-' && i + 1 < json.length && json[i + 1].isDigit()) -> {
                    val numberBuilder = StringBuilder()
                    while (i < json.length && (json[i].isDigit() || json[i] in ".-+eE")) {
                        numberBuilder.append(json[i])
                        i++
                    }
                    withStyle(SpanStyle(color = valueNumberColor)) {
                        append(numberBuilder.toString())
                    }
                    continue
                }

                // true, false, null
                char == 't' || char == 'f' || char == 'n' -> {
                    val keyword = when {
                        json.substring(i).startsWith("true") -> "true"
                        json.substring(i).startsWith("false") -> "false"
                        json.substring(i).startsWith("null") -> "null"
                        else -> null
                    }
                    if (keyword != null) {
                        withStyle(SpanStyle(color = valueBoolNullColor)) {
                            append(keyword)
                        }
                        i += keyword.length
                        continue
                    } else {
                        append(char)
                    }
                }

                // Whitespace and other characters
                else -> {
                    append(char)
                }
            }
            i++
        }
    }
}

private fun highlightXmlSyntax(xml: String): AnnotatedString {
    val tagColor = Color(0xFF569CD6)        // Blue for tags
    val attrNameColor = Color(0xFF9CDCFE)   // Light blue for attribute names
    val attrValueColor = Color(0xFFCE9178)  // Orange for attribute values
    val contentColor = Color(0xFFD4D4D4)    // Light gray for content
    val commentColor = Color(0xFF6A9955)    // Green for comments

    return buildAnnotatedString {
        var i = 0
        while (i < xml.length) {
            when {
                // XML comment
                xml.substring(i).startsWith("<!--") -> {
                    val endIndex = xml.indexOf("-->", i)
                    val comment = if (endIndex != -1) {
                        xml.substring(i, endIndex + 3)
                    } else {
                        xml.substring(i)
                    }
                    withStyle(SpanStyle(color = commentColor)) {
                        append(comment)
                    }
                    i += comment.length
                }
                // XML declaration or tag
                xml[i] == '<' -> {
                    val endIndex = xml.indexOf('>', i)
                    if (endIndex != -1) {
                        val tag = xml.substring(i, endIndex + 1)
                        withStyle(SpanStyle(color = tagColor)) {
                            append(tag)
                        }
                        i = endIndex + 1
                    } else {
                        withStyle(SpanStyle(color = tagColor)) {
                            append(xml[i])
                        }
                        i++
                    }
                }
                // Content between tags
                else -> {
                    val nextTagIndex = xml.indexOf('<', i)
                    val content = if (nextTagIndex != -1) {
                        xml.substring(i, nextTagIndex)
                    } else {
                        xml.substring(i)
                    }
                    withStyle(SpanStyle(color = contentColor)) {
                        append(content)
                    }
                    i += content.length
                }
            }
        }
    }
}

@Composable
private fun ThinkingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "dots")
    val dotsCount by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dotsAnimation"
    )

    val dots = ".".repeat(dotsCount.toInt().coerceIn(0, 3))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(GreenAssistant)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = "🤔  Думаю$dots",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Black
            )
        }
    }
}

@Composable
private fun SummarizingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "summarizing")
    val dotsCount by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dotsAnimation"
    )

    val dots = ".".repeat(dotsCount.toInt().coerceIn(0, 3))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(OrangeSummarization)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = Color(0xFFE65100)
                )
                Text(
                    text = "Суммаризация диалога$dots",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFE65100)
                )
            }
        }
    }
}

@Composable
private fun MessageInput(
    text: String,
    onTextChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onAttachFileClick: () -> Unit,
    isLoading: Boolean,
    isReadingFile: Boolean,
    attachedFile: AttachedFileInfo?,
    onClearAttachment: () -> Unit,
    gradientBrush: Brush,
    sendMessageMode: SendMessageMode
) {
    val canSend = (text.isNotBlank() || attachedFile != null) && !isLoading

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(gradientBrush)
    ) {
        if (attachedFile != null) {
            AttachedFileChip(
                fileName = attachedFile.fileName,
                characterCount = attachedFile.characterCount,
                onRemove = onClearAttachment,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = onAttachFileClick,
                enabled = !isLoading && !isReadingFile
            ) {
                if (isReadingFile) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.AttachFile,
                        contentDescription = "Прикрепить файл",
                        tint = if (isLoading) Color.White.copy(alpha = 0.38f) else Color.White
                    )
                }
            }

            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier
                    .weight(1f)
                    .onPreviewKeyEvent { keyEvent ->
                        if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.Enter) {
                            when (sendMessageMode) {
                                SendMessageMode.ENTER -> {
                                    if (keyEvent.isShiftPressed) {
                                        false
                                    } else {
                                        if (canSend) {
                                            onSendClick()
                                        }
                                        true
                                    }
                                }
                                SendMessageMode.SHIFT_ENTER -> {
                                    if (keyEvent.isShiftPressed) {
                                        if (canSend) {
                                            onSendClick()
                                        }
                                        true
                                    } else {
                                        false
                                    }
                                }
                            }
                        } else {
                            false
                        }
                    },
                placeholder = {
                    Text(
                        if (attachedFile != null) "Добавьте комментарий..."
                        else "Введите сообщение..."
                    )
                },
                shape = RoundedCornerShape(24.dp),
                maxLines = 4,
                enabled = !isLoading,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default)
            )

            IconButton(
                onClick = onSendClick,
                enabled = canSend
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Отправить",
                    tint = if (canSend) Color.White else Color.White.copy(alpha = 0.38f)
                )
            }
        }

        if (text.isNotEmpty() || attachedFile != null) {
            val charCount = text.length + (attachedFile?.characterCount ?: 0)
            val estimatedTokens = estimateTokenCount(text) +
                (attachedFile?.let { estimateTokenCount(it.content) } ?: 0)
            Text(
                text = "$charCount chars · ~$estimatedTokens tokens",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
            )
        }
    }
}

@Composable
private fun AttachedFileChip(
    fileName: String,
    characterCount: Int,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.2f)
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = Color.White
            )
            Text(
                text = "$fileName ($characterCount chars)",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White
            )
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Удалить файл",
                    modifier = Modifier.size(16.dp),
                    tint = Color.White
                )
            }
        }
    }
}

/**
 * Estimate token count for input text.
 * Uses ~4 characters per token for Latin text, ~2 for Cyrillic/CJK.
 */
private fun estimateTokenCount(text: String): Int {
    if (text.isEmpty()) return 0
    var latinChars = 0
    var otherChars = 0
    text.forEach { char ->
        if (char.code < 128) latinChars++ else otherChars++
    }
    val latinTokens = latinChars / 4.0
    val otherTokens = otherChars / 2.0
    return maxOf(1, (latinTokens + otherTokens).toInt())
}

/**
 * Get color indicator for tokens based on model limit.
 * - Green: 0-70% of limit
 * - Orange: 71-90% of limit
 * - Red: >90% of limit
 * - White (default): no limit defined for model
 */
private fun getTokensIndicatorColor(totalTokens: Int, model: AiModel): Color {
    val maxTokens = model.maxTokens ?: return Color.White.copy(alpha = 0.8f)
    val percentage = (totalTokens.toFloat() / maxTokens) * 100
    return when {
        percentage <= 70f -> Color(0xFF4CAF50) // Green
        percentage <= 90f -> Color(0xFFFF9800) // Orange
        else -> Color(0xFFF44336) // Red
    }
}