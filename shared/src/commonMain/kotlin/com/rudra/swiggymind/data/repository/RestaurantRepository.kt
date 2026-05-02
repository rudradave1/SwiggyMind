package com.rudra.swiggymind.data.repository

import com.rudra.swiggymind.AppConstants
import com.rudra.swiggymind.Res
import com.rudra.swiggymind.ai.AiJsonParser
import com.rudra.swiggymind.domain.model.InstamartItem
import com.rudra.swiggymind.domain.model.Restaurant
import com.rudra.swiggymind.domain.model.UserIntent
import com.rudra.swiggymind.domain.repository.SettingsRepository
import org.jetbrains.compose.resources.ExperimentalResourceApi

interface RestaurantRepository {
    suspend fun getRestaurants(intent: UserIntent? = null): List<Restaurant>
    suspend fun getRestaurantById(id: String): Restaurant?
    suspend fun getInstamartItems(intent: UserIntent? = null): List<InstamartItem>
    suspend fun getDineoutVenues(intent: UserIntent? = null): List<Restaurant>
}

class MockRestaurantRepository(
    private val settingsRepository: SettingsRepository
) : RestaurantRepository {
    private var mockRestaurants = emptyList<Restaurant>()
    private var mockInstamartItems = emptyList<InstamartItem>()
    private var mockDineoutVenues = emptyList<Restaurant>()

    @OptIn(ExperimentalResourceApi::class)
    private suspend fun ensureLoaded() {
        if (mockRestaurants.isEmpty()) {
            try {
                val restaurantBytes = Res.readBytes("files/mock_restaurants.json")
                mockRestaurants = AiJsonParser.json.decodeFromString<List<Restaurant>>(restaurantBytes.decodeToString())
                
                val instamartBytes = Res.readBytes("files/mock_instamart.json")
                mockInstamartItems = AiJsonParser.json.decodeFromString<List<InstamartItem>>(instamartBytes.decodeToString())
                
                val dineoutBytes = Res.readBytes("files/mock_dineout.json")
                mockDineoutVenues = AiJsonParser.json.decodeFromString<List<Restaurant>>(dineoutBytes.decodeToString())
            } catch (e: Exception) {
                // Last Resort: Hardcoded data if file reading fails
                mockRestaurants = listOf(
                    Restaurant("r001", "Honest Restaurant", listOf("Gujarati", "Thali"), 4.3, 35, 300, AppConstants.FALLBACK_IMAGE_URL, location = "Ahmedabad"),
                    Restaurant("r005", "Sankalp", listOf("South Indian"), 4.2, 25, 400, AppConstants.FALLBACK_IMAGE_URL, location = "Ahmedabad"),
                    Restaurant("m001", "Swati Snacks", listOf("Street Food"), 4.6, 30, 400, AppConstants.FALLBACK_IMAGE_URL, location = "Mumbai")
                )
            }
        }
    }

    override suspend fun getRestaurants(intent: UserIntent?): List<Restaurant> {
        ensureLoaded()
        val currentCity = settingsRepository.currentCity.value
        
        // Primary filter by city
        var filtered = mockRestaurants.filter { 
            it.location?.equals(currentCity, ignoreCase = true) == true 
        }
        
        if (filtered.isEmpty()) {
            filtered = mockRestaurants.filter { it.location == AppConstants.DEFAULT_CITY }
        }

        if (intent == null) return filtered
        
        intent.budget?.let { budget ->
            // Filter out restaurants where priceForTwo > budget * 2 (approximate per-person * 2 heuristic)
            filtered = filtered.filter { it.costForTwo <= budget * 2 }
        }
        
        if (intent.mood == "quick" && intent.mealType != "dineout") {
            // Quick delivery intent -> only include restaurants where deliveryTime <= 30 minutes
            filtered = filtered.filter { it.deliveryTimeMinutes <= 30 }
        }
        
        if (intent.specificCravings.isNotEmpty()) {
            val cravingFiltered = filtered.filter { restaurant ->
                intent.specificCravings.any { craving ->
                    restaurant.matchesCraving(craving)
                }
            }

            if (cravingFiltered.isNotEmpty()) {
                filtered = cravingFiltered
            }
        }

        if (intent.dietaryPreference != null) {
            val isVeg = intent.dietaryPreference.contains("veg", ignoreCase = true)
            filtered = filtered.filter { it.isVeg == isVeg }
        }

        if (intent.mood == "quick") {
            filtered = filtered.sortedBy { it.deliveryTimeMinutes }
        }

        filtered = filtered.prioritizeByPreferences(intent)
        
        return filtered
    }

    override suspend fun getInstamartItems(intent: UserIntent?): List<InstamartItem> {
        ensureLoaded()
        // Simple filtering for now
        return mockInstamartItems
    }

    override suspend fun getDineoutVenues(intent: UserIntent?): List<Restaurant> {
        ensureLoaded()
        return mockDineoutVenues
    }

    override suspend fun getRestaurantById(id: String): Restaurant? {
        ensureLoaded()
        return (mockRestaurants + mockDineoutVenues).find { it.id == id }
    }
}

private fun List<Restaurant>.prioritizeByPreferences(intent: UserIntent): List<Restaurant> {
    val wantsSpicy = intent.spiceLevel == "spicy" ||
        intent.specificCravings.any { it.equals("spicy", ignoreCase = true) }

    val wantsLight = intent.mood.equals("light", ignoreCase = true) ||
        intent.mood.equals("healthy", ignoreCase = true) ||
        intent.specificCravings.any {
            it.equals("healthy", ignoreCase = true) || it.equals("light", ignoreCase = true)
        }

    if (!wantsSpicy && !wantsLight) return this

    return sortedWith(
        compareByDescending<Restaurant> { restaurant ->
            when {
                wantsSpicy -> restaurant.spicyPriorityScore()
                wantsLight -> restaurant.lightPriorityScore()
                else -> 0
            }
        }.thenByDescending { it.rating }
            .thenBy { it.deliveryTimeMinutes }
            .thenBy { it.costForTwo }
    )
}

private fun Restaurant.matchesCraving(craving: String): Boolean {
    val normalizedCraving = craving.trim().lowercase()

    if (normalizedCraving == "spicy") {
        return spicyPriorityScore() > 0
    }
    if (normalizedCraving == "healthy" || normalizedCraving == "light") {
        return lightPriorityScore() > 0
    }

    val aliases = cravingAliases(normalizedCraving)

    return aliases.any { term ->
        cuisine.any { it.contains(term, ignoreCase = true) } ||
            name.contains(term, ignoreCase = true) ||
            tags.any { it.contains(term, ignoreCase = true) } ||
            (description?.contains(term, ignoreCase = true) == true) ||
            menu.any { menuItem ->
                menuItem.name.contains(term, ignoreCase = true) ||
                    (menuItem.description?.contains(term, ignoreCase = true) == true) ||
                    (menuItem.category?.contains(term, ignoreCase = true) == true)
            }
    }
}

private fun Restaurant.spicyPriorityScore(): Int {
    val tagHits = tags.count { it.containsAnyOf("spicy", "hot", "fiery", "chilli") }
    if (tagHits > 0) return 400 + tagHits

    val cuisineHits = cuisine.count {
        it.containsAnyOf("Chinese", "Thai", "Mexican", "Andhra", "Chettinad", "North Indian")
    }
    if (cuisineHits > 0) return 300 + cuisineHits

    val menuHits = menu.count {
        it.spiceLevel.equals("spicy", ignoreCase = true) || it.spiceLevel.equals("medium", ignoreCase = true)
    }
    if (menuHits > 0) return 200 + menuHits

    return 0
}

private fun Restaurant.lightPriorityScore(): Int {
    val tagHits = tags.count { it.containsAnyOf("healthy", "light", "salad", "low-cal", "diet") }
    if (tagHits > 0) return 400 + tagHits

    val cuisineHits = cuisine.count {
        it.containsAnyOf("Mediterranean", "Japanese", "South Indian", "Continental")
    }
    if (cuisineHits > 0) return 300 + cuisineHits

    val menuHits = menu.count { (it.calories ?: Int.MAX_VALUE) < 400 }
    if (menuHits > 0) return 200 + menuHits

    // If we have no explicit healthy metadata, cheaper is treated as lighter.
    return (1000 - costForTwo).coerceAtLeast(1)
}

private fun String.containsAnyOf(vararg terms: String): Boolean {
    return terms.any { contains(it, ignoreCase = true) }
}

private fun cravingAliases(craving: String): List<String> {
    return when (craving) {
        "spicy" -> listOf("spicy", "chinese", "mexican", "street food", "schezwan", "masala", "chatpata")
        "quick delivery" -> listOf("quick", "fast", "snacks", "street food")
        "healthy" -> listOf("healthy", "salad", "light")
        else -> listOf(craving)
    }
}
