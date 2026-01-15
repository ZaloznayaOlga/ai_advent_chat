package com.olgaz.aichat.data.provider

import android.content.Context
import com.olgaz.aichat.domain.model.CommunicationStyle
import com.olgaz.aichat.domain.model.ResponseFormat
import com.olgaz.aichat.domain.provider.SystemPromptProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class SystemPromptProviderImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SystemPromptProvider {

    private val cachedPrompt: String by lazy {
        context.assets.open("system-prompt.txt")
            .bufferedReader()
            .use { it.readText() }
    }

    private val promptCache = mutableMapOf<String, String>()

    override fun getSystemPrompt(): String = cachedPrompt

    override fun getSystemPrompt(
        communicationStyle: CommunicationStyle,
        responseFormat: ResponseFormat
    ): String {
        val styleFile = when (communicationStyle) {
            CommunicationStyle.GENERAL -> "system-prompt-default.txt"
            CommunicationStyle.WITH_QUESTIONS -> "system-prompt-with-questions.txt"
        }

        val formatFile = when (responseFormat) {
            ResponseFormat.TEXT -> "system-prompt-text.txt"
            ResponseFormat.JSON -> "system-prompt-json.txt"
            ResponseFormat.XML -> "system-prompt-xml.txt"
        }

        val cacheKey = "$styleFile:$formatFile"

        return promptCache.getOrPut(cacheKey) {
            val formatPrompt = loadAsset(formatFile)
            val stylePrompt = loadAsset(styleFile)
            "$formatPrompt\n\n$stylePrompt"
        }
    }

    private fun loadAsset(fileName: String): String {
        return context.assets.open(fileName)
            .bufferedReader()
            .use { it.readText() }
    }
}
