package com.rudra.swiggymind.domain.usecase

import com.rudra.swiggymind.domain.model.UserIntent

object HeuristicIntentParser {
    fun parse(message: String): UserIntent {
        val normalized = message.lowercase()

        val cravings = buildList {
            listOf(
                "biryani", "pizza", "burger", "dosa", "idli", "thali", "paneer",
                "chinese", "north indian", "south indian", "roll", "sandwich",
                "spicy", "salad", "healthy", "momos", "pasta", "cake", "ice cream"
            ).forEach { keyword ->
                if (normalized.contains(keyword)) add(keyword)
            }
        }.distinct()

        val budget = Regex("""(?:under|below|within)\s*[₹rs.\s]*?(\d{2,5})""")
            .find(normalized)
            ?.groupValues
            ?.getOrNull(1)
            ?.toIntOrNull()

        val minBudget = Regex("""(?:above|over|more than)\s*[₹rs.\s]*?(\d{2,5})""")
            .find(normalized)
            ?.groupValues
            ?.getOrNull(1)
            ?.toIntOrNull()

        val dietaryPreference = when {
            normalized.contains("vegan") -> "vegan"
            normalized.contains("veg") || normalized.contains("vegetarian") -> "veg"
            normalized.contains("non veg") || normalized.contains("non-veg") || normalized.contains("chicken") || normalized.contains("mutton") -> "nonveg"
            else -> null
        }

        val hasGroceryKeywords = normalized.contains("grocery") || normalized.contains("instamart") || 
            normalized.contains("milk") || normalized.contains("egg") || 
            normalized.contains("bread") || normalized.contains("butter") ||
            normalized.contains("vegetable") || normalized.contains("fruit") ||
            normalized.contains("onion") || normalized.contains("tomato") ||
            normalized.contains("potato") || normalized.contains("oil") ||
            normalized.contains("sugar") || normalized.contains("salt") ||
            normalized.contains("rice") || normalized.contains("dal") ||
            normalized.contains("paneer") || normalized.contains("cheese") ||
            normalized.contains("soap") || normalized.contains("shampoo") ||
            normalized.contains("chips") || normalized.contains("biscuit")

        val mealType = when {
            hasGroceryKeywords -> "grocery"
            normalized.contains("dineout") || normalized.contains("book a table") || 
            normalized.contains("table for") || normalized.contains("restaurant booking") -> "dineout"
            normalized.contains("buy") || normalized.contains("need") || normalized.contains("order some") -> {
                if (cravings.isNotEmpty()) "food" else "grocery"
            }
            else -> "food"
        }

        val spiceLevel = when {
            normalized.contains("extra spicy") || normalized.contains("very spicy") || normalized.contains("spicy") -> "spicy"
            normalized.contains("mild") -> "mild"
            normalized.contains("medium spicy") -> "medium"
            else -> null
        }

        val mood = when {
            normalized.contains("comfort") -> "comfort"
            normalized.contains("light") -> "light"
            normalized.contains("healthy") -> "healthy"
            normalized.contains("quick delivery") || normalized.contains("quick") || normalized.contains("fast") -> "quick"
            normalized.contains("cheat") -> "indulgent"
            else -> null
        }

        val excludes = buildList {
            listOf("onion", "garlic", "mushroom", "egg", "nuts").forEach { keyword ->
                if (normalized.contains("no $keyword") || normalized.contains("without $keyword")) add(keyword)
            }
        }

        val partySize = Regex("""(?:for|table for)\s+(\d{1,2})""")
            .find(normalized)
            ?.groupValues
            ?.getOrNull(1)
            ?.toIntOrNull()

        return UserIntent(
            specificCravings = cravings,
            budget = budget,
            minBudget = minBudget,
            dietaryPreference = dietaryPreference,
            mealType = mealType,
            mood = mood,
            spiceLevel = spiceLevel,
            excludes = excludes,
            partySize = partySize,
            rawQuery = message
        )
    }

    fun looksUseful(intent: UserIntent): Boolean {
        return intent.specificCravings.isNotEmpty() ||
            intent.budget != null ||
            intent.minBudget != null ||
            intent.dietaryPreference != null ||
            intent.mealType != null ||
            intent.mood != null ||
            intent.spiceLevel != null ||
            intent.excludes.isNotEmpty() ||
            intent.partySize != null
    }
}
