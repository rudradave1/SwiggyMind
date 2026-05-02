package com.rudra.swiggymind.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Restaurant(
    val id: String,
    val name: String,
    val cuisine: List<String>,
    val rating: Double,
    val deliveryTimeMinutes: Int,
    val costForTwo: Int,
    val imageUrl: String,
    val isVeg: Boolean = true,
    val isDineout: Boolean = false,
    val tags: List<String> = emptyList(),
    val location: String? = null,
    val isOpen: Boolean = true,
    val address: String? = null,
    val description: String? = null,
    val menu: List<MenuItem> = emptyList()
)

@Serializable
data class InstamartItem(
    val id: String,
    val name: String,
    val category: String,
    val price: Int,
    val rating: Double,
    val deliveryTimeMinutes: Int,
    val imageUrl: String,
    val tags: List<String> = emptyList()
)

@Serializable
data class MenuItem(
    val id: String,
    val name: String,
    val price: Int,
    val isVeg: Boolean,
    val description: String? = null,
    val category: String? = null,
    val spiceLevel: String? = null,
    val calories: Int? = null
)

@Serializable
data class UserIntent(
    val specificCravings: List<String> = emptyList(),
    val budget: Int? = null,
    val minBudget: Int? = null,
    val dietaryPreference: String? = null, // "veg" | "nonveg" | "vegan"
    val mealType: String? = null, // "food" | "grocery" | "dineout"
    val occasion: String? = null,
    val mood: String? = null,
    val location: String? = null,
    val spiceLevel: String? = null, // "mild" | "medium" | "spicy"
    val excludes: List<String> = emptyList(),
    val partySize: Int? = null,
    val reasoningChain: String? = null, // AI's internal thought process
    val rawQuery: String = ""
)

@Serializable
data class RecommendationResponse(
    val picks: List<RecommendationResult>,
    val summary: String,
    val cognitiveReasoning: String? = null // Chain of Thought from AI
)

@Serializable
data class RecommendationResult(
    val restaurantId: String,
    val reason: String,
    val matchScore: Int = 0 // 0-100 score of how well it matches DNA + Intent
)

data class RestaurantRecommendation(
    val restaurant: Restaurant,
    val reason: String,
    val matchScore: Int = 0
)

data class OrchestratedResponse(
    val summary: String,
    val recommendations: List<RestaurantRecommendation>,
    val reasoningChain: String? = null,
    val isAiFallback: Boolean = false,
    val isRelaxed: Boolean = false,
    val ingredients: List<String> = emptyList(),
    val isGrocery: Boolean = false,
    val isLlmOffline: Boolean = false,
    val isMcp: Boolean = false,
    val aiStatus: String = "CLOUD" // "CLOUD", "MCP", "FALLBACK"
)
