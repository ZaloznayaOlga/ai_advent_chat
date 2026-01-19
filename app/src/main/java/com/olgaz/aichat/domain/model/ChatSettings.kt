package com.olgaz.aichat.domain.model

/**
 * Провайдеры AI API
 */
enum class AiProvider(val displayName: String) {
    DEEPSEEK("DeepSeek"),
    OPENAI("OpenAI")
}

/**
 * Доступные модели AI для выбора
 */
enum class AiModel(val displayName: String, val apiName: String, val provider: AiProvider) {
    // DeepSeek модели
    DEEPSEEK_CHAT("DeepSeek Chat", "deepseek-chat", AiProvider.DEEPSEEK),
    DEEPSEEK_REASONER("DeepSeek Reasoner", "deepseek-reasoner", AiProvider.DEEPSEEK),
    // OpenAI модели
    GPT_4O("GPT-4o", "gpt-4o", AiProvider.OPENAI),
    GPT_4O_MINI("GPT-4o Mini", "gpt-4o-mini", AiProvider.OPENAI),
    O1_PREVIEW("o1-preview", "o1-preview", AiProvider.OPENAI),
    O1_MINI("o1-mini", "o1-mini", AiProvider.OPENAI);

    companion object {
        fun defaultForProvider(provider: AiProvider): AiModel = when (provider) {
            AiProvider.DEEPSEEK -> DEEPSEEK_CHAT
            AiProvider.OPENAI -> GPT_4O
        }

        fun forProvider(provider: AiProvider): List<AiModel> =
            entries.filter { it.provider == provider }
    }
}

/**
 * Стиль общения AI
 */
enum class CommunicationStyle(val displayName: String) {
    GENERAL("Общие ответы"),
    WITH_QUESTIONS("С уточняющими вопросами")
}

/**
 * Формат ответа от AI
 */
enum class ResponseFormat(val displayName: String) {
    TEXT("Текст"),
    JSON("JSON"),
    XML("XML")
}

/**
 * Способ отправки сообщения
 */
enum class SendMessageMode(val displayName: String) {
    ENTER("Enter"),
    SHIFT_ENTER("Shift+Enter")
}

/**
 * Режим системного промпта
 */
enum class SystemPromptMode(val displayName: String) {
    DEFAULT("По умолчанию"),
    CUSTOM("Пользовательский")
}

/**
 * Настройки чата для текущей сессии
 */
data class ChatSettings(
    val provider: AiProvider = AiProvider.DEEPSEEK,
    val model: AiModel = AiModel.DEEPSEEK_CHAT,
    val communicationStyle: CommunicationStyle = CommunicationStyle.GENERAL,
    val deepThinking: Boolean = false,
    val responseFormat: ResponseFormat = ResponseFormat.TEXT,
    val sendMessageMode: SendMessageMode = SendMessageMode.ENTER,
    val systemPromptMode: SystemPromptMode = SystemPromptMode.DEFAULT,
    val customSystemPrompt: String = ""
)
