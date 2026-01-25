package com.olgaz.aichat.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey
    val id: String,

    val content: String,

    val role: String,

    val timestamp: Long,

    @ColumnInfo(name = "display_content")
    val displayContent: String,

    @ColumnInfo(name = "json_data")
    val jsonData: String? = null,

    @ColumnInfo(name = "metadata")
    val metadata: String? = null,

    @Embedded(prefix = "file_")
    val attachedFile: FileAttachmentEmbedded? = null,

    @Embedded(prefix = "summary_")
    val summarizationInfo: SummarizationInfoEmbedded? = null,

    @ColumnInfo(name = "is_summary")
    val isSummary: Boolean = false,

    @ColumnInfo(name = "covered_by_summary_id")
    val coveredBySummaryId: String? = null
)

data class FileAttachmentEmbedded(
    @ColumnInfo(name = "name")
    val fileName: String,
    @ColumnInfo(name = "char_count")
    val characterCount: Int
)

data class SummarizationInfoEmbedded(
    @ColumnInfo(name = "msg_count")
    val summarizedMessageCount: Int,
    @ColumnInfo(name = "input_tokens")
    val summarizedInputTokens: Int,
    @ColumnInfo(name = "output_tokens")
    val summarizedOutputTokens: Int
)
