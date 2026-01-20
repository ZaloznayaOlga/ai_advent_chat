package com.olgaz.aichat.data.remote.logging

import android.util.Log
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener

class PrettyJsonLogger : HttpLoggingInterceptor.Logger {

    companion object {
        private const val TAG = "API"
        private const val JSON_INDENT = 2
        private const val MAX_LOG_LENGTH = 4000
    }

    override fun log(message: String) {
        val trimmedMessage = message.trim()

        if (trimmedMessage.isEmpty()) return

        // Пробуем отформатировать как JSON
        val formattedMessage = tryFormatJson(trimmedMessage) ?: trimmedMessage

        // Логируем с разбивкой на части (Android ограничивает длину лога)
        logLongMessage(formattedMessage)
    }

    private fun tryFormatJson(message: String): String? {
        if (!message.startsWith("{") && !message.startsWith("[")) {
            return null
        }

        return try {
            when (val json = JSONTokener(message).nextValue()) {
                is JSONObject -> json.toString(JSON_INDENT)
                is JSONArray -> json.toString(JSON_INDENT)
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun logLongMessage(message: String) {
        if (message.length <= MAX_LOG_LENGTH) {
            Log.d(TAG, message)
            return
        }

        // Разбиваем длинное сообщение на части
        var startIndex = 0
        var partNumber = 1
        while (startIndex < message.length) {
            val endIndex = minOf(startIndex + MAX_LOG_LENGTH, message.length)
            val part = message.substring(startIndex, endIndex)
            Log.d(TAG, "[$partNumber] $part")
            startIndex = endIndex
            partNumber++
        }
    }
}