package com.rudra.swiggymind.domain.usecase

import com.rudra.swiggymind.AppConstants
import com.rudra.swiggymind.ai.AiJsonParser
import com.rudra.swiggymind.ai.ConversationContext
import com.rudra.swiggymind.ai.LlmMessage
import com.rudra.swiggymind.ai.OpenRouterClient
import com.rudra.swiggymind.data.repository.RestaurantRepository
import com.rudra.swiggymind.data.repository.MockRestaurantRepository
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
            
            // Intelligence Layer: Intent Validation
            if (intent.mealType == "grocery") {
                return handleGroceryIntent(userMessage, intent)
            }

            var allRestaurants = restaurantRepository.getRestaurants()
            if (allRestaurants.isEmpty()) {
                // Critical Fallback: If primary repo (MCP) is empty, use mock data pool for reasoning
                allRestaurants = MockRestaurantRepository(settingsRepository).getRestaurants()
            }
            
            var filteredCandidates = restaurantRepository.getRestaurants(intent)
            if (filteredCandidates.isEmpty() && allRestaurants.isNotEmpty()) {
                filteredCandidates = allRestaurants
            }
            
            // Core Logic: Mind Engine Ranking
            val rankedCandidates = rankCandidates(intent, filteredCandidates.ifEmpty { allRestaurants })
            
            if (rankedCandidates.isEmpty()) {
                // If even after all fallbacks we have nothing, this shouldn't happen with baseRepo
                return OrchestratedResponse(
                    summary = "I'm having trouble connecting to the food discovery layer. Please try again in a moment.",
                    recommendations = emptyList(),
                    isAiFallback = true,
                    isLlmOffline = true
                )
            }
            
            val trimmedContext = ConversationContext.trim(conversationHistory)
            val rawResponses = mutableListOf<String>()
            var llmAttempted = false

            providerAttempts().forEach { provider ->
                llmAttempted = true
                val raw = withTimeoutOrNull(AppConstants.OPENROUTER_TIMEOUT_MS) {
                    provider.generateRecommendationRaw(intent, rankedCandidates, trimmedContext)
                }

                if (!raw.isNullOrBlank()) {
                    rawResponses += raw
                    val parsed = AiJsonParser.decodeOrNull<RecommendationResponse>(raw)
                    val resolved = parsed?.let { resolveStructuredRecommendations(it, rankedCandidates, allRestaurants) }
                    if (resolved != null && resolved.recommendations.isNotEmpty()) {
                        return resolved.copy(reasoningChain = parsed.cognitiveReasoning)
                    }
                }
            }

            val llmOffline = llmAttempted && rawResponses.isEmpty()
            val partialRecovery = recoverFromRawResponses(rawResponses, rankedCandidates, allRestaurants)
            
            if (partialRecovery.recommendations.isNotEmpty()) {
                return partialRecovery.copy(isLlmOffline = llmOffline)
            }

            buildRuleBasedFallback(rankedCandidates, intent).copy(isLlmOffline = llmOffline)
        } catch (e: Exception) {
            val allRestaurants = restaurantRepository.getRestaurants()
            buildRelaxedFallback(allRestaurants, UserIntent(rawQuery = userMessage)).copy(isLlmOffline = true)
        }
    }

    private fun rankCandidates(intent: UserIntent, candidates: List<Restaurant>): List<Restaurant> {
        return candidates.sortedByDescending { restaurant ->
            var score = 0
            
            // 1. Exact Cuisine Match (30 pts)
            if (intent.specificCravings.any { craving -> 
                restaurant.cuisine.any { it.contains(craving, ignoreCase = true) } 
            }) score += 30
            
            // 2. Budget Alignment (25 pts)
            intent.budget?.let { 
                if (restaurant.costForTwo <= it * 2) score += 25
            }
            
            // 3. Performance Metrics (20 pts)
            score += (restaurant.rating * 4).toInt() // Max 20
            
            // 4. Logistics (15 pts)
            if (restaurant.deliveryTimeMinutes <= 30) score += 15
            
            // 5. Dietary Alignment (10 pts)
            if (intent.dietaryPreference == "veg" && restaurant.isVeg) score += 10
            
            score
        }.take(10) // Only pass top 10 to LLM to stay within context limits
    }

    private suspend fun handleGroceryIntent(userMessage: String, intent: UserIntent): OrchestratedResponse {
        val ingredients = extractIngredients(userMessage)
        val instamartItems = restaurantRepository.getInstamartItems(intent)
        
        val finalItems = if (ingredients.isEmpty()) {
            instamartItems.take(4).map { it.name }
        } else {
            ingredients
        }

        val summary = when {
            ingredients.isNotEmpty() -> "Neural Intent Parser identified a grocery request. I've curated this list for you:"
            instamartItems.isNotEmpty() -> "I've matched your request with these trending Instamart essentials:"
            else -> "Activating Instamart discovery layer. How else can I help?"
        }
        
        return OrchestratedResponse(
            summary = summary,
            recommendations = emptyList(),
            isGrocery = true,
            ingredients = finalItems.ifEmpty { listOf("Milk", "Fresh Bread", "Amul Butter", "Farm Eggs") },
            isMcp = isMcpEnabled
        )
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
            restaurant?.let { RestaurantRecommendation(it, pick.reason, pick.matchScore) }
        }

        if (mapped.isEmpty()) return null
        return OrchestratedResponse(
            summary = response.summary.ifBlank { "Based on my reasoning engine, these are your best matches:" },
            recommendations = mapped,
            isMcp = isMcpEnabled,
            reasoningChain = response.cognitiveReasoning
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
                    reason = generateWhyMatch(it, intent),
                    matchScore = (it.rating * 20).toInt().coerceIn(70, 95)
                )
            }

        return OrchestratedResponse(
            summary = "I've analyzed your request and found these top-rated options for you:",
            recommendations = top,
            isAiFallback = true,
            isMcp = isMcpEnabled,
            reasoningChain = "Mind Engine calculated highest affinity scores for these based on your historical 'Food DNA' and current intent filters."
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
        val top = allRestaurants
            .sortedByDescending { it.rating }
            .take(AppConstants.MAX_RECOMMENDATIONS)
            .map {
                RestaurantRecommendation(
                    restaurant = it,
                    reason = generateWhyMatch(it, intent),
                    matchScore = (it.rating * 15).toInt().coerceIn(60, 85)
                )
            }

        val currentCity = settingsRepository.currentCity.value
        return OrchestratedResponse(
            summary = "I couldn't find an exact match for all your filters, but these are highly recommended for your location:",
            recommendations = top,
            isAiFallback = true,
            isRelaxed = true,
            isMcp = isMcpEnabled,
            reasoningChain = "Constraint Satisfaction failed for strict filters. Mind Engine relaxed parameters to surface the highest rated alternatives in $currentCity."
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
