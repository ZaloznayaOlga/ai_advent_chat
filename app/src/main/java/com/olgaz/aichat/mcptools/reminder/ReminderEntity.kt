package com.olgaz.aichat.mcptools.reminder

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    @ColumnInfo(name = "is_completed") val isCompleted: Boolean = false,
    @ColumnInfo(name = "interval_minutes") val intervalMinutes: Int? = null,
    @ColumnInfo(name = "reminder_time") val reminderTime: Long? = null,
    @ColumnInfo(name = "is_notified") val isNotified: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "completed_at") val completedAt: Long? = null
)
