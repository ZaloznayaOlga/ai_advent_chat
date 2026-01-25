package com.olgaz.aichat.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.olgaz.aichat.data.local.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {

    @Query("SELECT * FROM messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages ORDER BY timestamp ASC")
    suspend fun getAllMessagesOnce(): List<MessageEntity>

    @Query("SELECT * FROM messages ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastMessage(): MessageEntity?

    @Query("SELECT * FROM messages WHERE is_summary = 1 ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastSummary(): MessageEntity?

    @Query("""
        SELECT * FROM messages
        WHERE timestamp > COALESCE(
            (SELECT timestamp FROM messages WHERE is_summary = 1 ORDER BY timestamp DESC LIMIT 1),
            0
        )
        AND is_summary = 0
        ORDER BY timestamp ASC
    """)
    suspend fun getMessagesAfterLastSummary(): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE covered_by_summary_id IS NULL AND is_summary = 0 ORDER BY timestamp ASC")
    suspend fun getUnsummarizedMessages(): List<MessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)

    @Query("UPDATE messages SET covered_by_summary_id = :summaryId WHERE id IN (:messageIds)")
    suspend fun markMessagesCoveredBySummary(summaryId: String, messageIds: List<String>)

    @Query("DELETE FROM messages")
    suspend fun deleteAllMessages()

    @Query("SELECT COUNT(*) FROM messages")
    suspend fun getMessageCount(): Int

    @Query("SELECT COUNT(*) FROM messages WHERE role = :role")
    suspend fun getMessageCountByRole(role: String): Int
}
