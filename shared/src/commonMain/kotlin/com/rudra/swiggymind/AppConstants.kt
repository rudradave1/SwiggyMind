package com.rudra.swiggymind

object AppConstants {
    // Timeouts
    const val OPENROUTER_TIMEOUT_MS = 8_000L
    const val IMAGE_LOAD_TIMEOUT_MS = 10_000L

    // UI
    const val CARD_CORNER_RADIUS = 12
    const val CARD_IMAGE_HEIGHT = 140
    const val CHIP_CORNER_RADIUS = 50

    // Business logic
    const val MIN_SESSIONS_FOR_DNA = 3
    const val MAX_CONTEXT_MESSAGES = 6
    const val DEDUP_WINDOW_SECONDS = 30
    const val MAX_RECOMMENDATIONS = 3
    const val QUICK_DELIVERY_MAX_MINS = 30

    // Supported cities
    val SUPPORTED_CITIES = listOf("Ahmedabad", "Mumbai", "Bangalore")
    const val DEFAULT_CITY = "Ahmedabad"

    // URLs
    const val INSTAMART_URL = "https://www.swiggy.com/instamart"
    const val SWIGGY_SEARCH_URL = "https://www.swiggy.com/search?query="
    const val BUILDERS_CLUB_URL = "https://mcp.swiggy.com/builders/#about"
    const val GITHUB_URL = "https://github.com/rudradave1/swiggymind"

    // OpenRouter
    const val OPENROUTER_MODEL = "openrouter/auto"
    const val OPENROUTER_BASE_URL = "https://openrouter.ai/api/v1"
    const val OPENROUTER_REFERER = "https://github.com/rudradave1/swiggymind"
    const val OPENROUTER_APP_TITLE = "SwiggyMind"
    const val MAX_TOKENS = 500
}
