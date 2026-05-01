package com.rudra.swiggymind.domain.usecase

import com.rudra.swiggymind.AppConstants
import com.rudra.swiggymind.ai.AiJsonParser
import com.rudra.swiggymind.ai.ConversationContext
import com.rudra.swiggymind.ai.LlmMessage
import com.rudra.swiggymind.ai.OpenRouterClient
import com.rudra.swiggymind.data.repository.RestaurantRepository
import com.rudra.swiggymind.domain.model.OrchestratedResponse
import com.rudra.swiggymind.domain.model.RecommendationResponse
import com.rudra.swiggymind.domain.model.Restaurant
import com.rudra.swiggymind.domain.model.RestaurantRecommendation
import com.rudra.swiggymind.domain.model.UserIntent
import com.rudra.swiggymind.domain.repository.SettingsRepository
import kotlinx.coroutines.withTimeoutOrNull

class ResponseOrchestrator(
    private val settingsRepository: SettingsRepository,
    private val restaurantRepository: RestaurantRepository,
    private val isMcpEnabled: Boolean = false
) {
    suspend operator fun invoke(
        userMessage: String,
        conversationHistory: List<LlmMessage> = emptyList()
    ): OrchestratedResponse {
        return try {
            val intent = HeuristicIntentParser.parse(userMessage).copy(rawQuery = userMessage)
            if (intent.mealType == "grocery") {
                val ingredients = extractIngredients(userMessage)
                val instamartItems = restaurantRepository.getInstamartItems(intent)
                
                // If user didn't specify items but we have trending items, show some
                val finalItems = if (ingredients.isEmpty()) {
                    instamartItems.take(4).map { it.name }
                } else {
                    ingredients
                }

                val summary = when {
                    ingredients.isNotEmpty() -> "I've found these items for you on Instamart:"
                    instamartItems.isNotEmpty() -> "I'm opening Instamart for you. Here are some trending items you might need:"
                    else -> "Opening Instamart for you. What else do you need?"
                }
                
                return OrchestratedResponse(
                    summary = summary,
                    recommendations = emptyList(),
                    isGrocery = true,
                    ingredients = finalItems.ifEmpty { listOf("Milk", "Fresh Bread", "Amul Butter", "Farm Eggs") },
                    isMcp = isMcpEnabled
                )
            }

            val filteredCandidates = restaurantRepository.getRestaurants(intent)
            val allRestaurants = restaurantRepository.getRestaurants()
            val trimmedContext = ConversationContext.trim(conversationHistory)

            val rawResponses = mutableListOf<String>()
            val llmCandidates = if (filteredCandidates.isEmpty()) allRestaurants else filteredCandidates
            var llmAttempted = false

            providerAttempts().forEach { provider ->
                llmAttempted = true
                val raw = withTimeoutOrNull(AppConstants.OPENROUTER_TIMEOUT_MS) {
                    provider.generateRecommendationRaw(intent, llmCandidates, trimmedContext)
                }

                if (!raw.isNullOrBlank()) {
                    rawResponses += raw

                    val parsed = AiJsonParser.decodeOrNull<RecommendationResponse>(raw)
                    val resolved = parsed?.let { resolveStructuredRecommendations(it, llmCandidates, allRestaurants) }
                    if (resolved != null && resolved.recommendations.isNotEmpty()) {
                        return resolved
                    }
                }
            }

            val llmOffline = llmAttempted && rawResponses.isEmpty()

            val partialRecovery = recoverFromRawResponses(rawResponses, llmCandidates, allRestaurants)
            if (partialRecovery.recommendations.isNotEmpty()) {
                return partialRecovery.copy(isLlmOffline = llmOffline)
            }

            val ruleBased = buildRuleBasedFallback(filteredCandidates, intent)
            if (ruleBased.recommendations.isNotEmpty()) {
                return ruleBased.copy(isLlmOffline = llmOffline)
            }

            buildRelaxedFallback(allRestaurants, intent).copy(isLlmOffline = llmOffline)
        } catch (e: Exception) {
            val allRestaurants = restaurantRepository.getRestaurants()
            val dummyIntent = UserIntent(rawQuery = userMessage)
            buildRelaxedFallback(allRestaurants, dummyIntent).copy(isLlmOffline = true)
        }
    }

    private fun providerAttempts(): List<RawRecommendationProvider> {
        val providers = mutableListOf<RawRecommendationProvider>()
        val apiKey = settingsRepository.openRouterApiKey.value.trim()
        if (apiKey.isNotBlank() && !apiKey.contains("dummy")) {
            providers += OpenRouterProvider(OpenRouterClient(apiKey))
        }
        return providers
    }

    private fun resolveStructuredRecommendations(
        response: RecommendationResponse,
        filteredCandidates: List<Restaurant>,
        allRestaurants: List<Restaurant>
    ): OrchestratedResponse? {
        val mapped = response.picks.mapNotNull { pick ->
            val restaurant = filteredCandidates.find { it.id == pick.restaurantId }
                ?: allRestaurants.find { it.id == pick.restaurantId }
            restaurant?.let { RestaurantRecommendation(it, pick.reason) }
        }

        if (mapped.isEmpty()) return null
        return OrchestratedResponse(
            summary = response.summary.ifBlank { "Here's what I found based on your request" },
            recommendations = mapped,
            isMcp = isMcpEnabled
        )
    }

    private fun recoverFromRawResponses(
        rawResponses: List<String>,
        filteredCandidates: List<Restaurant>,
        allRestaurants: List<Restaurant>
    ): OrchestratedResponse {
        if (rawResponses.isEmpty()) return OrchestratedResponse(summary = "", recommendations = emptyList())

        val pool = (filteredCandidates + allRestaurants).distinctBy { it.id }
        val matches = linkedMapOf<String, RestaurantRecommendation>()

        rawResponses.forEach { raw ->
            val cleaned = raw
                .replace("```json", "", ignoreCase = true)
                .replace("```", "")
                .trim()

            Regex("""r\d{3}""", RegexOption.IGNORE_CASE).findAll(cleaned).forEach { match ->
                val restaurant = pool.find { it.id.equals(match.value, ignoreCase = true) }
                if (restaurant != null && matches[restaurant.id] == null) {
                    matches[restaurant.id] = RestaurantRecommendation(restaurant, "Mentioned in the AI response.")
                }
            }

            pool.forEach { restaurant ->
                if (cleaned.contains(restaurant.name, ignoreCase = true) && matches[restaurant.id] == null) {
                    matches[restaurant.id] = RestaurantRecommendation(restaurant, "Referenced in the AI response.")
                }
            }

            if (matches.size < AppConstants.MAX_RECOMMENDATIONS) {
                val fuzzy = pool.mapNotNull { restaurant ->
                    val score = fuzzyNameScore(cleaned, restaurant.name)
                    if (score >= 2) restaurant to score else null
                }.sortedByDescending { it.second }

                fuzzy.forEach { (restaurant, _) ->
                    if (matches.size < AppConstants.MAX_RECOMMENDATIONS && matches[restaurant.id] == null) {
                        matches[restaurant.id] = RestaurantRecommendation(restaurant, "Looks like a close match from the AI response.")
                    }
                }
            }
        }

        return OrchestratedResponse(
            summary = "Here's what I found based on your request",
            recommendations = matches.values.take(AppConstants.MAX_RECOMMENDATIONS),
            isMcp = isMcpEnabled
        )
    }

    private fun buildRuleBasedFallback(candidates: List<Restaurant>, intent: UserIntent): OrchestratedResponse {
        val top = candidates
            .sortedByDescending { it.rating }
            .take(AppConstants.MAX_RECOMMENDATIONS)
            .map {
                RestaurantRecommendation(
                    restaurant = it,
                    reason = generateWhyMatch(it, intent)
                )
            }

        return OrchestratedResponse(
            summary = "I found these highly rated options for you",
            recommendations = top,
            isAiFallback = !isMcpEnabled,
            isMcp = isMcpEnabled
        )
    }

    fun generateWhyMatch(restaurant: Restaurant, intent: UserIntent): String {
        val reasons = mutableListOf<String>()
        if (restaurant.rating >= 4.5)
            reasons.add("One of the highest rated in this category")
        if (restaurant.deliveryTimeMinutes <= 25)
            reasons.add("Delivers in under 25 minutes")
        if (intent.budget != null &&
            restaurant.costForTwo <= intent.budget * 2)
            reasons.add("Within your \u20b9${intent.budget} budget")
        if (restaurant.isVeg && intent.dietaryPreference == "veg")
            reasons.add("Pure veg kitchen")
        if (restaurant.tags.any { it.contains("spicy", ignoreCase = true) }
            && intent.spiceLevel == "spicy")
            reasons.add("Known for spicy dishes")

        return if (reasons.isEmpty())
            "Highly rated with fast delivery"
        else reasons.take(2).joinToString(" · ")
    }

    private fun buildRelaxedFallback(allRestaurants: List<Restaurant>, intent: UserIntent): OrchestratedResponse {
        var filtered = allRestaurants

        intent.budget?.let { budget ->
            filtered = filtered.filter { it.costForTwo <= budget * 2 }
        }

        if (intent.mood == "quick" && intent.mealType != "dineout") {
            filtered = filtered.filter { it.deliveryTimeMinutes <= AppConstants.QUICK_DELIVERY_MAX_MINS }
        }

        val finalPool = if (filtered.isNotEmpty()) filtered else allRestaurants

        val top = finalPool
            .sortedByDescending { it.rating }
            .take(AppConstants.MAX_RECOMMENDATIONS)
            .map {
                RestaurantRecommendation(
                    restaurant = it,
                    reason = generateWhyMatch(it, intent)
                )
            }

        return OrchestratedResponse(
            summary = if (filtered.isNotEmpty())
                "I couldn't find an exact match, but here are some top picks that fit your filters"
                else "I couldn't find an exact match, but here are our top picks",
            recommendations = top,
            isAiFallback = !isMcpEnabled,
            isRelaxed = true,
            isMcp = isMcpEnabled
        )
    }

    private fun extractIngredients(message: String): List<String> {
        val commonIngredients = listOf(
            "milk", "egg", "bread", "butter", "onion", "tomato", "potato", "garlic", "ginger",
            "oil", "salt", "sugar", "flour", "rice", "dal", "chicken", "paneer", "cheese",
            "curd", "yogurt", "apple", "banana", "orange", "vegetable", "fruit", "chilli",
            "turmeric", "cumin", "coriander", "mustard", "honey", "coffee", "tea", "soap",
            "shampoo", "detergent", "toothpaste", "biscuit", "chips", "juice", "water"
        )

        val normalized = message.lowercase()
        val found = commonIngredients.filter { normalized.contains(it) }

        // Improved extraction to catch items after "need", "buy", "order"
        val explicitItems = mutableListOf<String>()
        val prefixes = listOf("need", "buy", "order", "want", "get", "bring")
        prefixes.forEach { prefix ->
            val regex = Regex("$prefix\\s+(.*?)(?:from|on|in|and|for|$)")
            regex.findAll(normalized).forEach { match ->
                val items = match.groupValues[1].split(Regex(""",|and"""))
                    .map { it.trim() }
                    .filter { it.length in 3..25 }
                    .filter { !it.contains("grocery") && !it.contains("instamart") }
                explicitItems.addAll(items)
            }
        }

        return (found + explicitItems).distinct()
            .map { it.replaceFirstChar { char -> char.uppercase() } }
    }

    private fun fuzzyNameScore(raw: String, restaurantName: String): Int {
        val tokens = restaurantName.lowercase()
            .split(Regex("""[^a-z0-9]+"""))
            .filter { it.length > 2 }
        if (tokens.isEmpty()) return 0
        return tokens.count { raw.contains(it, ignoreCase = true) }
    }
}

private interface RawRecommendationProvider {
    suspend fun generateRecommendationRaw(
        intent: UserIntent,
        candidates: List<Restaurant>,
        conversationContext: List<LlmMessage>
    ): String?
}

private class OpenRouterProvider(
    private val client: OpenRouterClient
) : RawRecommendationProvider {
    override suspend fun generateRecommendationRaw(
        intent: UserIntent,
        candidates: List<Restaurant>,
        conversationContext: List<LlmMessage>
    ): String? = client.generateRecommendationRaw(intent, candidates, conversationContext)
}
