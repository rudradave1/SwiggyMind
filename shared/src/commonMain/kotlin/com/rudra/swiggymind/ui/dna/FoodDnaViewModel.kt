package com.rudra.swiggymind.ui.dna

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rudra.swiggymind.data.local.ChatHistoryDao
import com.rudra.swiggymind.domain.usecase.HeuristicIntentParser
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.rudra.swiggymind.ai.LLMClient

data class FoodDnaState(
    val isLoading: Boolean = true,
    val sessionCount: Int = 0,
    val topCuisines: List<String> = emptyList(),
    val topDishes: List<String> = emptyList(),
    val avgBudgetRange: String = "₹0-0",
    val budgetTag: String = "Unknown",
    val spiceTolerance: String = "Medium",
    val spiceProgress: Float = 0.5f,
    val dietPreference: String = "Neutral",
    val priority: String = "Variety focused",
    val aiAnalysis: String = "",
    val isLocked: Boolean = true
)

class FoodDnaViewModel(
    private val chatHistoryDao: ChatHistoryDao,
    private val llmClient: LLMClient
) : ViewModel() {

    val uiState: StateFlow<FoodDnaState> = chatHistoryDao.getAllUserMessages()
        .map { messages ->
            if (messages.isEmpty()) return@map FoodDnaState(isLoading = false, sessionCount = 0, isLocked = true)

            val intents = messages.map { com.rudra.swiggymind.domain.usecase.HeuristicIntentParser.parse(it.text) }
            val sessionCount = intents.size

            if (sessionCount < 3) {
                return@map FoodDnaState(isLoading = false, sessionCount = sessionCount, isLocked = true)
            }

            // 1. Top Cuisines & Dishes
            val allCravings = intents.flatMap { it.specificCravings }
            val cuisineKeywords = listOf("biryani", "pizza", "burger", "dosa", "idli", "thali", "chinese", "north indian", "south indian", "pasta")
            
            val topCuisines = allCravings
                .filter { craving -> cuisineKeywords.any { it.equals(craving, ignoreCase = true) } }
                .groupBy { it.lowercase() }
                .mapValues { it.value.size }
                .toList()
                .sortedByDescending { it.second }
                .take(2)
                .map { it.first.replaceFirstChar { c -> c.uppercase() } }

            val topDishes = allCravings
                .filter { craving -> !cuisineKeywords.any { it.equals(craving, ignoreCase = true) } }
                .groupBy { it.lowercase() }
                .mapValues { it.value.size }
                .toList()
                .sortedByDescending { it.second }
                .take(4)
                .map { it.first.replaceFirstChar { c -> c.uppercase() } }

            // 2. Avg Budget
            val budgets = intents.mapNotNull { it.budget }.filter { it > 0 }
            val avgBudget = if (budgets.isNotEmpty()) budgets.average().toInt() else 0
            val budgetRange = if (avgBudget > 0) {
                val start = (avgBudget / 100) * 100
                "₹$start-${start + 200}"
            } else "₹150-300"
            
            val budgetTag = when {
                avgBudget > 800 -> "Fine Dining"
                avgBudget > 400 -> "Gourmet"
                else -> "Value Seeker"
            }

            // 3. Spice Tolerance
            val spiceScores = intents.map { 
                when(it.spiceLevel) {
                    "spicy" -> 1.0f
                    "medium" -> 0.5f
                    "mild" -> 0.2f
                    else -> 0.4f 
                }
            }
            val avgSpice = spiceScores.average().toFloat()
            val spiceTolerance = when {
                avgSpice > 0.7f -> "High"
                avgSpice > 0.4f -> "Medium"
                else -> "Mild"
            }

            // 4. Diet Preference
            val vegCount = intents.count { it.dietaryPreference == "veg" }
            val nonVegCount = intents.count { it.dietaryPreference == "nonveg" }
            val dietPreference = when {
                vegCount > nonVegCount * 1.5 -> "Veg leaning"
                nonVegCount > vegCount * 1.5 -> "Non-veg leaning"
                else -> "Balanced"
            }

            // 5. Priority
            val quickCount = intents.count { it.mood == "quick" }
            val priority = if (quickCount > sessionCount / 2) "Speed over variety" else "Quality focused"

            val currentState = FoodDnaState(
                isLoading = false,
                sessionCount = sessionCount,
                topCuisines = topCuisines.ifEmpty { listOf("North Indian", "Chinese") },
                topDishes = topDishes.ifEmpty { listOf("Butter Chicken", "Dal Makhani", "Noodles", "Dimsums") },
                avgBudgetRange = budgetRange,
                budgetTag = budgetTag,
                spiceTolerance = spiceTolerance,
                spiceProgress = avgSpice,
                dietPreference = dietPreference,
                priority = priority,
                isLocked = false
            )

            // Perform local rule-based analysis
            val dynamicAnalysis = buildString {
                append("You seem to have a ")
                append(dietPreference.lowercase())
                append(" diet, often craving ")
                if (topCuisines.isNotEmpty()) {
                    append(topCuisines.first())
                } else {
                    append("comfort food")
                }
                append(". ")
                
                when {
                    avgSpice > 0.7f -> append("You love a good spicy kick, ")
                    avgSpice > 0.4f -> append("You enjoy moderate spice, ")
                    else -> append("You prefer milder flavors, ")
                }
                
                when (budgetTag) {
                    "Fine Dining" -> append("and frequently opt for premium dining experiences.")
                    "Gourmet" -> append("and enjoy quality meals with a slightly higher budget.")
                    else -> append("and you appreciate good value for your money.")
                }
                
                if (quickCount > sessionCount / 2) {
                    append(" Speed is also a priority for you.")
                }
            }
            
            currentState.copy(aiAnalysis = dynamicAnalysis)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FoodDnaState())
}
