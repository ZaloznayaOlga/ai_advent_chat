package com.olgaz.aichat.mcptools.reminder

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ReminderDao {

    @Query("SELECT * FROM reminders WHERE is_completed = 0 ORDER BY created_at DESC")
    suspend fun getActiveReminders(): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE is_completed = 1 ORDER BY completed_at DESC")
    suspend fun getCompletedReminders(): List<ReminderEntity>

    @Query("SELECT * FROM reminders ORDER BY created_at DESC")
    suspend fun getAllReminders(): List<ReminderEntity>

    @Insert
    suspend fun insert(reminder: ReminderEntity): Long

    @Query("UPDATE reminders SET is_completed = 1, completed_at = :completedAt WHERE id = :id")
    suspend fun markCompleted(id: Long, completedAt: Long): Int

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun delete(id: Long): Int
}
