package com.rudra.swiggymind.ai

import com.rudra.swiggymind.domain.model.RecommendationResult
import com.rudra.swiggymind.domain.model.Restaurant
import com.rudra.swiggymind.domain.model.UserIntent

interface LLMClient {
    suspend fun parseIntent(
        userMessage: String,
        conversationContext: List<LlmMessage> = emptyList()
    ): UserIntent
    
    suspend fun generateRecommendation(
        intent: UserIntent,
        candidates: List<Restaurant>,
        conversationContext: List<LlmMessage> = emptyList()
    ): com.rudra.swiggymind.domain.model.RecommendationResponse?

    suspend fun generateGeneric(
        prompt: String,
        systemPrompt: String? = null
    ): String?
}
