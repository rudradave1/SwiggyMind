package com.rudra.swiggymind.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rudra.swiggymind.AppConstants
import com.rudra.swiggymind.ai.AiJsonParser
import com.rudra.swiggymind.ai.LlmMessage
import com.rudra.swiggymind.data.local.ChatConversation
import com.rudra.swiggymind.data.local.ChatHistoryDao
import com.rudra.swiggymind.data.local.ChatMessageEntity
import com.rudra.swiggymind.data.repository.RestaurantRepository
import com.rudra.swiggymind.domain.model.RecommendationResult
import com.rudra.swiggymind.domain.model.RestaurantRecommendation
import com.rudra.swiggymind.domain.repository.SettingsRepository
import com.rudra.swiggymind.domain.usecase.ResponseOrchestrator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.datetime.Clock
import com.rudra.swiggymind.util.currentTimeMillis

class ChatViewModel(
    private val responseOrchestrator: ResponseOrchestrator,
    private val chatHistoryDao: ChatHistoryDao,
    private val restaurantRepository: RestaurantRepository,
    private val settingsRepository: SettingsRepository,
    private val isMcpEnabled: Boolean = false
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ChatUiState(
            isMcpEnabled = isMcpEnabled,
            aiStatus = if (isMcpEnabled) AiStatus.MCP else AiStatus.CLOUD
        )
    )
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    val currentCity = settingsRepository.currentCity
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppConstants.DEFAULT_CITY)

    private var currentConversationId = randomId()

    val totalConversations = chatHistoryDao.getConversationCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalRecommendations = chatHistoryDao.getTotalRecommendationsCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val hasDismissedOfflineBanner = settingsRepository.hasDismissedOfflineBanner
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private fun randomId() = (0..Int.MAX_VALUE).random().toString()

    fun dismissOfflineBanner() {
        viewModelScope.launch {
            settingsRepository.setOfflineBannerDismissed(true)
        }
    }

    fun loadConversation(conversationId: String) {
        currentConversationId = conversationId
        viewModelScope.launch {
            chatHistoryDao.getMessagesForConversation(conversationId).collect { entities ->
                val resolvedMessages = mutableListOf<ChatMessage>()

                for (entity in entities) {
                    val recommendations = entity.recommendationJson?.let { json ->
                        AiJsonParser.decodeOrNull<List<RecommendationResult>>(json)?.let { results ->
                            val resolvedRecs = mutableListOf<RestaurantRecommendation>()
                            for (result in results) {
                                val restaurant = restaurantRepository.getRestaurantById(result.restaurantId)
                                if (restaurant != null) {
                                    resolvedRecs.add(RestaurantRecommendation(restaurant, result.reason))
                                }
                            }
                            resolvedRecs
                        }
                    } ?: emptyList<RestaurantRecommendation>()

                    val ingredients = entity.ingredientsJson?.let { json ->
                        AiJsonParser.decodeOrNull<List<String>>(json)
                    } ?: emptyList()

                    resolvedMessages.add(
                        ChatMessage(
                            text = entity.text,
                            isFromUser = entity.isFromUser,
                            recommendations = recommendations,
                            isAiFallback = entity.isAiFallback,
                            isRelaxed = entity.isRelaxed,
                            isGrocery = entity.isGrocery,
                            ingredients = ingredients
                        )
                    )
                }

                _uiState.value = _uiState.value.copy(messages = resolvedMessages)
            }
        }
    }

    fun startNewChat() {
        currentConversationId = randomId()
        _uiState.value = ChatUiState(
            currentCity = currentCity.value,
            isMcpEnabled = _uiState.value.isMcpEnabled,
            aiStatus = if (_uiState.value.isMcpEnabled) AiStatus.MCP else AiStatus.CLOUD
        )
    }

    fun clearAllSessions() {
        viewModelScope.launch {
            chatHistoryDao.clearAllConversations()
            chatHistoryDao.clearAllMessages()
            startNewChat()
        }
    }

    fun sendMessage(message: String) {
        if (message.isBlank()) return

        val trimmedMessage = message.trim()
        val existingMessages = _uiState.value.messages
        val userMessage = ChatMessage(text = message, isFromUser = true)
        _uiState.value = _uiState.value.copy(
            messages = existingMessages + userMessage,
            isLoading = true,
            currentCity = currentCity.value
        )

        viewModelScope.launch {
            try {
                val conversationHistory = (existingMessages + userMessage).map {
                    LlmMessage(
                        role = if (it.isFromUser) "user" else "assistant",
                        content = it.text
                    )
                }

                val response = responseOrchestrator(trimmedMessage, conversationHistory)

                val assistantMessage = ChatMessage(
                    text = response.summary,
                    isFromUser = false,
                    recommendations = response.recommendations,
                    isAiFallback = response.isAiFallback,
                    isRelaxed = response.isRelaxed,
                    isGrocery = response.isGrocery,
                    ingredients = response.ingredients,
                    isMcp = response.isMcp
                )

                val newAiStatus = when {
                    isMcpEnabled -> AiStatus.MCP
                    response.isLlmOffline -> AiStatus.OFFLINE
                    response.isAiFallback -> AiStatus.FALLBACK
                    else -> AiStatus.CLOUD
                }

                _uiState.value = _uiState.value.copy(
                    messages = _uiState.value.messages + assistantMessage,
                    isLoading = false,
                    aiStatus = newAiStatus,
                    isLlmOffline = response.isLlmOffline
                )
                val now = currentTimeMillis()

                if (existingMessages.isEmpty()) {
                    val title = if (trimmedMessage.length > 28) trimmedMessage.take(28) + "..." else trimmedMessage
                    val dedupWindowMs = AppConstants.DEDUP_WINDOW_SECONDS * 1000L
                    val recent = chatHistoryDao.findRecentConversation(title, now - dedupWindowMs)

                    if (recent != null) {
                        currentConversationId = recent.id
                    } else {
                        chatHistoryDao.insertConversation(
                            ChatConversation(
                                id = currentConversationId,
                                title = title,
                                summary = response.summary.take(80),
                                timestamp = now
                            )
                        )
                    }
                }

                chatHistoryDao.insertMessage(
                    ChatMessageEntity(
                        conversationId = currentConversationId,
                        text = message,
                        isFromUser = true,
                        timestamp = now
                    )
                )

                val recommendationJson = if (response.recommendations.isNotEmpty()) {
                    val results = response.recommendations.map {
                        RecommendationResult(it.restaurant.id, it.reason)
                    }
                    AiJsonParser.json.encodeToString(results)
                } else null

                val ingredientsJson = if (response.isGrocery) {
                    AiJsonParser.json.encodeToString(response.ingredients)
                } else null

                chatHistoryDao.insertMessage(
                    ChatMessageEntity(
                        conversationId = currentConversationId,
                        text = response.summary,
                        isFromUser = false,
                        recommendationJson = recommendationJson,
                        ingredientsJson = ingredientsJson,
                        isGrocery = response.isGrocery,
                        isAiFallback = response.isAiFallback,
                        isRelaxed = response.isRelaxed,
                        timestamp = now
                    )
                )

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }
}

enum class AiStatus {
    CLOUD, FALLBACK, OFFLINE, MCP
}

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val aiStatus: AiStatus = AiStatus.CLOUD,
    val isLlmOffline: Boolean = false,
    val currentCity: String = AppConstants.DEFAULT_CITY,
    val isMcpEnabled: Boolean = false
)

data class ChatMessage(
    val text: String,
    val isFromUser: Boolean,
    val recommendations: List<RestaurantRecommendation> = emptyList(),
    val isAiFallback: Boolean = false,
    val isRelaxed: Boolean = false,
    val isGrocery: Boolean = false,
    val ingredients: List<String> = emptyList(),
    val isMcp: Boolean = false
)
