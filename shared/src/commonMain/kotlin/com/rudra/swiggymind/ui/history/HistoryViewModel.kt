package com.rudra.swiggymind.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import com.rudra.swiggymind.data.local.ChatConversation
import com.rudra.swiggymind.data.local.ChatHistoryDao
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import com.rudra.swiggymind.data.local.ChatMessageEntity
import kotlinx.datetime.Clock
import com.rudra.swiggymind.util.currentTimeMillis

class HistoryViewModel(
    private val chatHistoryDao: ChatHistoryDao,
    private val shouldSeedDefaults: Boolean = true
) : ViewModel() {

    init {
        seedInitialData()
    }

    val conversations: StateFlow<List<ChatConversation>> = chatHistoryDao.getAllConversations()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private fun seedInitialData() {
        if (!shouldSeedDefaults) return
        
        viewModelScope.launch {
            val isEmpty = chatHistoryDao.getAllConversations().map { it.isEmpty() }.first()
            if (isEmpty) {
                val now = currentTimeMillis()
                val samples = listOf(
                    ChatConversation("c1", "Friday Night Pizza", "Found top-rated spicy pizza spots within your budget.", now - 86400000),
                    ChatConversation("c2", "Healthy Dinner Prep", "Parsed ingredients for Grilled Chicken Salad & Quinoa.", now - 172800000),
                    ChatConversation("c3", "Budget Street Food", "Recommended local favorites under ₹150.", now - 259200000)
                )
                samples.forEach { chatHistoryDao.insertConversation(it) }
                
                // Add one message each so they aren't empty if clicked
                chatHistoryDao.insertMessage(ChatMessageEntity(conversationId = "c1", text = "Found some great pizza spots for you!", isFromUser = false, timestamp = now - 86400000))
                chatHistoryDao.insertMessage(ChatMessageEntity(conversationId = "c2", text = "Here's your grocery list for the salad.", isFromUser = false, timestamp = now - 172800000))
                chatHistoryDao.insertMessage(ChatMessageEntity(conversationId = "c3", text = "Here are the best budget street food options nearby.", isFromUser = false, timestamp = now - 259200000))
            }
        }
    }

    fun deleteConversation(id: String) {
        viewModelScope.launch {
            chatHistoryDao.deleteConversation(id)
        }
    }
}
