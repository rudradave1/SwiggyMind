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

    // Swiggy Builders Club — MCP endpoints
    const val MCP_BASE_URL_LOCAL   = "http://10.0.2.2:3000"       // local stub (Android emulator)
    const val MCP_BASE_URL_STAGING = "https://mcp.swiggy.com"     // swap when staging creds land
    const val MCP_SERVER_FOOD      = "/food"
    const val MCP_SERVER_INSTAMART = "/im"
    const val MCP_SERVER_DINEOUT   = "/dineout"
    const val MCP_FALLBACK_ADDRESS_ID = "addr_1"                  // used when get_addresses fails

    // Images
    const val FALLBACK_IMAGE_URL         = "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=400"
    const val FALLBACK_DINEOUT_IMAGE_URL = "https://images.unsplash.com/photo-1544739313-0fad7206497f?w=400"
    const val INSTAMART_DELIVERY_MINS    = 20
}
