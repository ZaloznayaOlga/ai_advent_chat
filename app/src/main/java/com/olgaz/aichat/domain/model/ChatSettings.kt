package com.olgaz.aichat.domain.model

/**
 * Провайдеры AI API
 */
enum class AiProvider(val displayName: String) {
    DEEPSEEK("DeepSeek"),
    OPENAI("OpenAI"),
    HUGGINGFACE("HuggingFace")
}

/**
 * Доступные модели AI для выбора
 * @param maxTokens лимит токенов (null = не показывать % использования)
 */
enum class AiModel(
    val displayName: String,
    val apiName: String,
    val provider: AiProvider,
    val maxTokens: Int? = null
) {
    // DeepSeek модели
    DEEPSEEK_CHAT("DeepSeek Chat", "deepseek-chat", AiProvider.DEEPSEEK, 128_000),
    DEEPSEEK_REASONER("DeepSeek Reasoner", "deepseek-reasoner", AiProvider.DEEPSEEK, 128_000),
    // OpenAI модели
    GPT_4O_MINI("GPT-4o Mini", "gpt-4o-mini", AiProvider.OPENAI, 16_384),
    // HuggingFace модели
    QWEN_72B("Qwen 2.5 72B", "Qwen/Qwen2.5-72B-Instruct", AiProvider.HUGGINGFACE, 32_000),
    LLAMA_3B("Llama 3.2 3B", "meta-llama/Llama-3.2-3B-Instruct", AiProvider.HUGGINGFACE, 128_000);

    companion object {
        fun defaultForProvider(provider: AiProvider): AiModel = when (provider) {
            AiProvider.DEEPSEEK -> DEEPSEEK_CHAT
            AiProvider.OPENAI -> GPT_4O_MINI
            AiProvider.HUGGINGFACE -> QWEN_72B
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
    val customSystemPrompt: String = "",
    val temperature: Float = 1.0f
)
