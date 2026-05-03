package com.rudra.swiggymind.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "conversations")
data class ChatConversation(
    @PrimaryKey val id: String,
    val title: String,
    val summary: String,
    val timestamp: Long
)

@Entity(
    tableName = "messages",
    indices = [Index("conversationId")],
    foreignKeys = [
        ForeignKey(
            entity = ChatConversation::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val conversationId: String,
    val text: String,
    val isFromUser: Boolean,
    val recommendationJson: String? = null,
    val ingredientsJson: String? = null,
    val isGrocery: Boolean = false,
    val isAiFallback: Boolean = false,
    val isRelaxed: Boolean = false,
    val isMcp: Boolean = false,
    val isRefinement: Boolean = false,
    val timestamp: Long,
    val reasoningChain: String? = null
)

@Dao
interface ChatHistoryDao {
    @Query("SELECT * FROM conversations ORDER BY timestamp DESC")
    fun getAllConversations(): Flow<List<ChatConversation>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: ChatConversation)

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    fun getMessagesForConversation(conversationId: String): Flow<List<ChatMessageEntity>>

    @Insert
    suspend fun insertMessage(message: ChatMessageEntity)

    @Query("DELETE FROM conversations WHERE id = :conversationId")
    suspend fun deleteConversation(conversationId: String)

    @Query("DELETE FROM conversations")
    suspend fun clearAllConversations()

    @Query("DELETE FROM messages")
    suspend fun clearAllMessages()

    @Query("SELECT COUNT(*) FROM conversations")
    fun getConversationCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM messages WHERE recommendationJson IS NOT NULL")
    fun getTotalRecommendationsCount(): Flow<Int>

    @Query("SELECT * FROM conversations WHERE title = :title AND timestamp > :sinceTime LIMIT 1")
    suspend fun findRecentConversation(title: String, sinceTime: Long): ChatConversation?

    @Query("SELECT * FROM messages WHERE isFromUser = 1")
    fun getAllUserMessages(): Flow<List<ChatMessageEntity>>
}
