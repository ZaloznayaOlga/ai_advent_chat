package com.olgaz.aichat.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey
    val id: Int = 1,

    @ColumnInfo(name = "provider")
    val provider: String,

    @ColumnInfo(name = "model")
    val model: String,

    @ColumnInfo(name = "communication_style")
    val communicationStyle: String,

    @ColumnInfo(name = "deep_thinking")
    val deepThinking: Boolean,

    @ColumnInfo(name = "response_format")
    val responseFormat: String,

    @ColumnInfo(name = "send_message_mode")
    val sendMessageMode: String,

    @ColumnInfo(name = "system_prompt_mode")
    val systemPromptMode: String,

    @ColumnInfo(name = "custom_system_prompt")
    val customSystemPrompt: String,

    @ColumnInfo(name = "temperature")
    val temperature: Float,

    @ColumnInfo(name = "summarization_enabled")
    val summarizationEnabled: Boolean,

    @ColumnInfo(name = "summarization_message_threshold")
    val summarizationMessageThreshold: Int,

    @ColumnInfo(name = "summarization_token_threshold")
    val summarizationTokenThreshold: Int,

    @ColumnInfo(name = "mcp_enabled", defaultValue = "0")
    val mcpEnabled: Boolean = false,

    @ColumnInfo(name = "mcp_weather_enabled", defaultValue = "0")
    val mcpWeatherEnabled: Boolean = false,

    @ColumnInfo(name = "mcp_reminder_enabled", defaultValue = "0")
    val mcpReminderEnabled: Boolean = false,

    @ColumnInfo(name = "mcp_server_url", defaultValue = "")
    val mcpServerUrl: String = ""
)
