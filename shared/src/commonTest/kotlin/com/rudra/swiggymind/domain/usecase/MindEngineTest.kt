package com.rudra.swiggymind.domain.usecase

import com.rudra.swiggymind.domain.model.UserIntent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MindEngineTest {

    @Test
    fun testHeuristicIntentParser() {
        val query = "I want some spicy biryani under 500"
        val intent = HeuristicIntentParser.parse(query)
        
        assertTrue(intent.specificCravings.contains("biryani"))
        assertTrue(intent.specificCravings.contains("spicy"))
        assertEquals(500, intent.budget)
    }

    @Test
    fun testHeuristicIntentParserBudgetRegex() {
        val queries = mapOf(
            "under 200" to 200,
            "below 500" to 500,
            "within 1000" to 1000
        )

        queries.forEach { (query, expected) ->
            val intent = HeuristicIntentParser.parse(query)
            assertEquals(expected, intent.budget, "Failed for query: $query")
        }
    }

    @Test
    fun testIntentMerging() {
        val lastIntent = UserIntent(
            specificCravings = listOf("spicy"),
            budget = 500,
            dietaryPreference = "veg"
        )
        val currentIntent = UserIntent(
            specificCravings = listOf("biryani")
        )

        // Mocking the merge logic from ResponseOrchestrator
        val merged = currentIntent.copy(
            dietaryPreference = currentIntent.dietaryPreference ?: lastIntent.dietaryPreference,
            budget = currentIntent.budget ?: lastIntent.budget,
            spiceLevel = currentIntent.spiceLevel ?: lastIntent.spiceLevel
        )

        assertEquals("veg", merged.dietaryPreference)
        assertEquals(500, merged.budget)
        assertEquals(listOf("biryani"), merged.specificCravings)
    }
}
