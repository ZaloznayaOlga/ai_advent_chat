package com.olgaz.aichat.data.provider

import android.content.Context
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

    override fun getSystemPrompt(): String = cachedPrompt
}
