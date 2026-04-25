package com.rudra.swiggymind.domain.usecase

import com.rudra.swiggymind.AppConstants
import com.rudra.swiggymind.ai.ConversationContext
import com.rudra.swiggymind.ai.LlmMessage
import com.rudra.swiggymind.ai.OpenRouterClient
import com.rudra.swiggymind.data.repository.RestaurantRepository
import com.rudra.swiggymind.domain.model.Restaurant
import com.rudra.swiggymind.domain.model.UserIntent
import com.rudra.swiggymind.domain.repository.SettingsRepository
import kotlinx.coroutines.withTimeoutOrNull

class ParseIntentUseCase(private val settingsRepository: SettingsRepository) {
    suspend operator fun invoke(
        message: String,
        conversationHistory: List<LlmMessage> = emptyList()
    ): UserIntent {
        val trimmedContext = ConversationContext.trim(conversationHistory)
        val apiKey = settingsRepository.openRouterApiKey.value
        val client = if (apiKey.isNotBlank() && !apiKey.contains("dummy")) {
            OpenRouterClient(apiKey)
        } else null

        val aiIntent = client?.let {
            withTimeoutOrNull(AppConstants.OPENROUTER_TIMEOUT_MS) { it.parseIntent(message, trimmedContext) }
        } ?: UserIntent(rawQuery = message)

        return if (aiIntent.isMeaningful()) {
            aiIntent
        } else {
            val heuristicIntent = HeuristicIntentParser.parse(message)
            if (HeuristicIntentParser.looksUseful(heuristicIntent)) heuristicIntent else aiIntent
        }
    }
}

class GetAIRecommendationsUseCase(
    private val settingsRepository: SettingsRepository,
    private val restaurantRepository: RestaurantRepository
) {
    suspend operator fun invoke(
        intent: UserIntent,
        conversationHistory: List<LlmMessage> = emptyList()
    ): Pair<String, List<Pair<Restaurant, String>>> {
        val candidates = restaurantRepository.getRestaurants(intent)
        if (candidates.isEmpty()) return "I couldn't find any restaurants matching your request." to emptyList()

        val trimmedContext = ConversationContext.trim(conversationHistory)
        val apiKey = settingsRepository.openRouterApiKey.value
        val client = if (apiKey.isNotBlank() && !apiKey.contains("dummy")) {
            OpenRouterClient(apiKey)
        } else null

        val aiResponse = client?.let {
            withTimeoutOrNull(AppConstants.OPENROUTER_TIMEOUT_MS) { it.generateRecommendation(intent, candidates, trimmedContext) }
        } ?: return buildHeuristicFallback(intent, candidates)

        val recommendations = aiResponse.picks.mapNotNull { rec ->
            val restaurant = candidates.find { it.id == rec.restaurantId }
            restaurant?.let { it to rec.reason }
        }

        return aiResponse.summary to recommendations.ifEmpty {
            candidates.take(AppConstants.MAX_RECOMMENDATIONS).map { it to "A strong match based on your budget and craving." }
        }
    }

    private fun buildHeuristicFallback(
        intent: UserIntent,
        candidates: List<Restaurant>
    ): Pair<String, List<Pair<Restaurant, String>>> {
        val ranked = candidates
            .sortedByDescending { restaurant -> scoreRestaurant(intent, restaurant) }
            .take(AppConstants.MAX_RECOMMENDATIONS)
            .map { restaurant -> restaurant to buildFallbackReason(intent, restaurant) }

        val summary = if (ranked.isEmpty()) {
            "I could not rank the restaurants just now."
        } else {
            "I picked these based on your filters, ratings, delivery time, and budget."
        }

        return summary to ranked
    }

    private fun scoreRestaurant(intent: UserIntent, restaurant: Restaurant): Int {
        var score = 0

        score += (restaurant.rating * 20).toInt()
        score += (60 - restaurant.deliveryTimeMinutes).coerceAtLeast(0)

        intent.budget?.let { budget ->
            if (restaurant.costForTwo <= budget) {
                score += 80
            } else {
                score -= (restaurant.costForTwo - budget) / 10
            }
        }

        if (intent.dietaryPreference?.contains("veg", ignoreCase = true) == true && restaurant.isVeg) {
            score += 50
        }

        if (intent.specificCravings.isNotEmpty()) {
            intent.specificCravings.forEach { craving ->
                val matchesCuisine = restaurant.cuisine.any { it.contains(craving, ignoreCase = true) }
                val matchesName = restaurant.name.contains(craving, ignoreCase = true)
                val matchesTag = restaurant.tags.any { it.contains(craving, ignoreCase = true) }
                if (matchesCuisine || matchesName || matchesTag) {
                    score += 40
                }
            }
        }

        return score
    }

    private fun buildFallbackReason(intent: UserIntent, restaurant: Restaurant): String {
        val parts = mutableListOf<String>()
        parts += "${restaurant.rating} rated"
        parts += "${restaurant.deliveryTimeMinutes} mins"

        intent.budget?.let {
            if (restaurant.costForTwo <= it) {
                parts += "fits your budget"
            }
        }

        if (intent.dietaryPreference?.contains("veg", ignoreCase = true) == true && restaurant.isVeg) {
            parts += "veg-friendly"
        }

        val cravingMatch = intent.specificCravings.firstOrNull { craving ->
            restaurant.cuisine.any { it.contains(craving, ignoreCase = true) } ||
                restaurant.name.contains(craving, ignoreCase = true) ||
                restaurant.tags.any { it.contains(craving, ignoreCase = true) }
        }
        if (cravingMatch != null) {
            parts += "matches '$cravingMatch'"
        }

        return parts.joinToString(", ")
    }
}

private fun UserIntent.isMeaningful(): Boolean {
    return specificCravings.isNotEmpty() ||
        budget != null ||
        minBudget != null ||
        dietaryPreference != null ||
        mealType != null ||
        occasion != null ||
        mood != null ||
        location != null ||
        spiceLevel != null ||
        excludes.isNotEmpty() ||
        partySize != null
}
