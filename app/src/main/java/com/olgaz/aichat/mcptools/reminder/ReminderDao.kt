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

    @Query("DELETE FROM reminders")
    suspend fun deleteAll(): Int

    @Query("SELECT * FROM reminders WHERE is_completed = 0 AND reminder_time IS NOT NULL AND reminder_time <= :thresholdTime AND is_notified = 0 ORDER BY reminder_time ASC")
    suspend fun getUpcomingReminders(thresholdTime: Long): List<ReminderEntity>

    @Query("SELECT COUNT(*) FROM reminders WHERE is_completed = 0")
    suspend fun getActiveReminderCount(): Int

    @Query("UPDATE reminders SET is_notified = 1 WHERE id = :id")
    suspend fun markNotified(id: Long): Int

    @Query("SELECT * FROM reminders WHERE is_completed = 0 ORDER BY reminder_time ASC")
    suspend fun getActiveRemindersSortedByTime(): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getReminderById(id: Long): ReminderEntity?

    @Query("SELECT * FROM reminders WHERE is_completed = 0 AND reminder_time IS NOT NULL AND reminder_time > :currentTime AND is_notified = 0 ORDER BY reminder_time ASC")
    suspend fun getPendingTimedReminders(currentTime: Long): List<ReminderEntity>

    @Query("SELECT id FROM reminders WHERE is_completed = 0 AND reminder_time IS NOT NULL AND is_notified = 0")
    suspend fun getPendingTimedReminderIds(): List<Long>
}
